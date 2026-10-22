package com.fpz.videoplayer

actual fun createVideoLibrary(): IVideoLibrary = AndroidVideoLibrary()
