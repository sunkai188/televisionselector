package com.fpz.videoplayer

import android.os.Environment

actual fun defaultVideoDirectory(): String =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath
