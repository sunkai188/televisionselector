***

```markdown
# Skill: KMP-VideoPlayer-Skill

## 1. 技能概述 (Overview)
本技能旨在指导开发者（或 AI 编程助手）使用 **Kotlin Multiplatform (KMP)** 和 **Compose Multiplatform (CMP)**，构建一套覆盖 Windows、Linux 和 Android 三端的高性能视频播放应用。核心诉求是“视频播放流畅优先”、“最大化共享 UI 与业务逻辑”，并适应 VS Code + DeepSeek V4 Pro 的轻量级开发工作流。

## 2. 技术栈与工具约束 (Tech Stack & Constraints)
*   **开发环境**：VS Code (需安装 `Kotlin by JetBrains` 官方扩展 Alpha 版) + DeepSeek V4 Pro。
*   **核心语言**：Kotlin (优先使用协程处理异步，避免阻塞主线程)。
*   **UI 框架**：Compose Multiplatform (共享三端 UI)。
*   **构建工具**：Gradle (使用 Kotlin DSL)。
*   **目标平台**：Windows、Linux、Android。
*   **核心原则**：**UI 与逻辑共享，播放引擎平台隔离。** 绝不为了强行一套代码而牺牲视频解码和渲染性能。

## 3. 核心架构与目录结构 (Architecture & Structure)
项目采用 `expect/actual` 机制或依赖注入（DI）进行平台特定实现的桥接。

```text
src/
├── commonMain/kotlin/       # 核心共享区
│   ├── ui/                  # Compose UI 界面 (播放器控制栏、列表等)
│   ├── player/              # 播放器接口定义 (IPlayerController)
│   └── viewmodel/           # 状态管理 (播放进度、缓冲状态)
├── androidMain/kotlin/      # Android 专属实现
│   └── player/              # ExoPlayer (Media3) 封装实现
└── desktopMain/kotlin/      # 桌面端 (Windows/Linux) 专属实现
    └── player/              # VLCJ 或 MPV 封装实现
```

## 4. 技能执行规范 (Execution Rules)

### 规则 1：播放器抽象层设计
*   **Action**：在 `commonMain` 中定义统一的播放器接口（如 `PlaybackState`、`PlayerCommand`）。
*   **Constraint**：UI 层（Compose）只能通过该接口与播放器交互，严禁在 UI 代码中直接引入 `ExoPlayer` 或 `VLCJ` 的类。

### 规则 2：Android 端引擎绑定 (ExoPlayer)
*   **Action**：在 `androidMain` 中，使用 Google 官方推荐的 **Media3 ExoPlayer** 作为播放后端。
*   **Constraint**：必须开启 `MediaCodec` 硬件加速。处理 Android 特有的生命周期（如 `onPause` 时暂停播放，`onResume` 时恢复）。

### 规则 3：桌面端引擎绑定 (VLCJ / MPV)
*   **Action**：在 `desktopMain` 中，优先推荐使用 **VLCJ**（通过 Gradle 插件打包原生库）或 **MPV**（通过 JNA 调用）作为后端。
*   **Constraint**：对于桌面端渲染，使用原生窗口句柄（Handle）或 `SwingPanel` 嵌入，**严禁使用 JavaFX 的 MediaPlayer**（性能不足以支撑高码率视频）。必须确保硬件解码开启。

### 规则 4：AI 辅助编码规范 (DeepSeek V4 Pro 协同)
*   在 VS Code 中向 AI 提问时，明确告知当前所在目录（如 `androidMain` 还是 `commonMain`）。
*   若要求 AI 生成视频渲染代码，必须指定“使用平台原生渲染句柄”。
*   遇到编译错误时，优先检查 `expect` 和 `actual` 的包名及签名是否完全一致。

## 5. 关键性能优化 (Performance Checklist)
- [ ] **异步初始化**：播放器引擎的实例化必须放在后台线程（如 `Dispatchers.IO`），避免应用冷启动卡顿。
- [ ] **状态流防抖**：播放进度（每 100ms 更新一次）需使用 `StateFlow` 传递，并确保 Compose 端使用了 `collectAsStateWithLifecycle` 或等效的优化机制，避免过度重组导致 UI 掉帧。
- [ ] **硬件解码校验**：在目标设备上必须验证是否真正走通了硬解（H.264/H.265），若遇到高码率视频掉帧，优先排查硬解配置。
- [ ] **UI 渲染优化**：Compose 中避免使用复杂度极高的阴影和模糊效果叠加在视频层之上。

## 6. 常见的反模式 (Anti-Patterns) - 严禁使用
1.  **禁止使用 JavaFX** 作为跨平台视频渲染方案（性能与打包体验双差）。
2.  **禁止使用基于 Web 的 UI 方案**（如 Electron 或 WebView 套壳），违背流畅度诉求。
3.  **禁止在 `commonMain` 中引入任何平台强相关的 UI 控件或多媒体库**。
```
