package com.fpz.videoplayer.player

actual fun createPlayerController(): IPlayerController = DesktopPlayerController()
