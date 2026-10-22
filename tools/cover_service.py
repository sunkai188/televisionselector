#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
视频封面提取侧车服务（三档）：low / mid / high
返回：裁剪后的人脸头像 PNG（不是整帧）
用法：python cover_service.py     # 监听 http://127.0.0.1:8765
接口：GET /cover?path=<视频路径>&strategy=low|mid|high
"""

import io
import os
import threading

import numpy as np
import cv2
from flask import Flask, request, send_file, jsonify

app = Flask(__name__)
_cover_lock = threading.Lock()

# 模型懒加载缓存
_haar = None
_mediapipe_fd = None
_insightface_app = None


def get_haar():
    global _haar
    if _haar is None:
        _haar = cv2.CascadeClassifier(
            cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
    return _haar


def get_mediapipe_fd():
    global _mediapipe_fd
    if _mediapipe_fd is None:
        import mediapipe as mp
        _mediapipe_fd = mp.solutions.face_detection.FaceDetection(
            model_selection=1, min_detection_confidence=0.5)
    return _mediapipe_fd


def get_insightface_app():
    global _insightface_app
    if _insightface_app is None:
        from insightface.app import FaceAnalysis
        _insightface_app = FaceAnalysis(name='buffalo_l', providers=['CPUExecutionProvider'])
        _insightface_app.prepare(ctx_id=0, det_size=(640, 640))
    return _insightface_app


def sharpness(gray):
    """Laplacian 方差清晰度。"""
    return float(cv2.Laplacian(gray, cv2.CV_64F).var())


def crop_face(frame, x1, y1, x2, y2, margin=0.5):
    """以人脸为中心扩边裁剪成头像。"""
    h, w = frame.shape[:2]
    cx, cy = (x1 + x2) / 2.0, (y1 + y2) / 2.0
    side = max(x2 - x1, y2 - y1) * (1.0 + 2.0 * margin)
    half = side / 2.0
    nx1 = max(0, int(cx - half))
    ny1 = max(0, int(cy - half))
    nx2 = min(w, int(cx + half))
    ny2 = min(h, int(cy + half))
    if nx2 - nx1 < 16 or ny2 - ny1 < 16:
        return None
    return frame[ny1:ny2, nx1:nx2]


def encode_png(img):
    ok, buf = cv2.imencode('.png', img)
    return buf.tobytes() if ok else None


def _l2norm(v):
    n = np.linalg.norm(v)
    return v / n if n > 1e-8 else v


# ---------- low: OpenCV Haar 正面级联 + Laplacian 清晰度 ----------
def cover_low(path):
    cascade = get_haar()
    cap = cv2.VideoCapture(path)
    if not cap.isOpened():
        return None
    fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
    step = max(1, int(fps))
    best = None
    idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        if idx % step == 0:
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = cascade.detectMultiScale(gray, 1.1, 5, minSize=(48, 48))
            for (x, y, w, h) in faces:
                s = sharpness(gray[y:y + h, x:x + w])
                if best is None or s > best[0]:
                    crop = crop_face(frame, x, y, x + w, y + h)
                    if crop is not None:
                        best = (s, crop)
        idx += 1
    cap.release()
    return best[1] if best else None


# ---------- mid: MediaPipe 人脸检测 + 6 关键点正脸 + 清晰度 ----------
def cover_mid(path):
    fd = get_mediapipe_fd()
    cap = cv2.VideoCapture(path)
    if not cap.isOpened():
        return None
    fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
    step = max(1, int(fps))
    best = None
    idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        if idx % step == 0:
            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = fd.process(rgb)
            if results.detections:
                h, w = frame.shape[:2]
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                for det in results.detections:
                    b = det.location_data.relative_bounding_box
                    x1 = max(0, int(b.xmin * w))
                    y1 = max(0, int(b.ymin * h))
                    x2 = min(w, int((b.xmin + b.width) * w))
                    y2 = min(h, int((b.ymin + b.height) * h))
                    s = sharpness(gray[y1:y2, x1:x2])
                    frontal = _frontal_from_mediapipe(det.location_data.relative_keypoints, w, h)
                    q = s * (0.5 + 0.5 * frontal)
                    if best is None or q > best[0]:
                        crop = crop_face(frame, x1, y1, x2, y2)
                        if crop is not None:
                            best = (q, crop)
        idx += 1
    cap.release()
    return best[1] if best else None


def _frontal_from_mediapipe(kps, w, h):
    # 6 关键点: 右眼0 左眼1 鼻尖2 嘴3 右耳4 左耳5
    try:
        re_x = kps[0].x * w
        le_x = kps[1].x * w
        nose_x = kps[2].x * w
        d_left = abs(le_x - nose_x)
        d_right = abs(re_x - nose_x)
        asym = abs(d_left - d_right) / max(d_left + d_right, 1e-6)
        return max(0.0, 1.0 - asym * 5.0)
    except Exception:
        return 0.5


# ---------- high: InsightFace 检测 + ArcFace 身份聚类 + 质量 ----------
def cover_high(path):
    fa = get_insightface_app()
    cap = cv2.VideoCapture(path)
    if not cap.isOpened():
        return None
    fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
    step = max(1, int(fps))
    identities = []  # {centroid, count, best:(quality, crop)}
    idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        if idx % step != 0:
            idx += 1
            continue
        faces = fa.get(frame)
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        for face in faces:
            x1, y1, x2, y2 = [int(v) for v in face.bbox]
            x1, y1 = max(0, x1), max(0, y1)
            s = sharpness(gray[y1:y2, x1:x2])
            frontal = _frontal_from_insight(face)
            q = s * (0.5 + 0.5 * frontal)
            emb = face.normed_embedding
            best_i, best_sim = -1, -1.0
            for i, ident in enumerate(identities):
                sim = float(np.dot(emb, ident['centroid']))
                if sim > best_sim:
                    best_sim, best_i = sim, i
            if best_i >= 0 and best_sim > 0.4:
                ident = identities[best_i]
                ident['count'] += 1
                ident['centroid'] = _l2norm(ident['centroid'] * ident['count'] + emb)
                if q > ident['best'][0]:
                    crop = crop_face(frame, x1, y1, x2, y2)
                    if crop is not None:
                        ident['best'] = (q, crop)
            else:
                crop = crop_face(frame, x1, y1, x2, y2)
                identities.append({'centroid': emb, 'count': 1, 'best': (q, crop)})
        idx += 1
    cap.release()
    if not identities:
        return None
    top = max(identities, key=lambda i: i['count'])
    return top['best'][1]


def _frontal_from_insight(face):
    try:
        lm = face.landmark_3d_68
        if lm is None:
            return 0.5
        left_eye = lm[36:42, :2].mean(axis=0)
        right_eye = lm[42:48, :2].mean(axis=0)
        nose = lm[30, :2]
        d_left = float(np.linalg.norm(left_eye - nose))
        d_right = float(np.linalg.norm(right_eye - nose))
        asym = abs(d_left - d_right) / max(d_left + d_right, 1e-6)
        return max(0.0, 1.0 - asym * 5.0)
    except Exception:
        return 0.5


@app.route('/cover', methods=['GET'])
def cover():
    path = request.args.get('path')
    strategy = request.args.get('strategy', 'low')
    if not path or not os.path.exists(path):
        return jsonify({'error': 'file not found'}), 404
    with _cover_lock:
        try:
            if strategy == 'low':
                img = cover_low(path)
            elif strategy == 'mid':
                img = cover_mid(path)
            elif strategy == 'high':
                img = cover_high(path)
            else:
                return jsonify({'error': 'unknown strategy'}), 400
        except Exception as e:
            return jsonify({'error': str(e)}), 500
    if img is None:
        return jsonify({'error': 'no face found'}), 404
    png = encode_png(img)
    if png is None:
        return jsonify({'error': 'encode failed'}), 500
    return send_file(io.BytesIO(png), mimetype='image/png')


if __name__ == '__main__':
    print('cover_service 已启动：http://127.0.0.1:8765/cover')
    app.run(host='127.0.0.1', port=8765)
