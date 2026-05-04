package com.phonkskull.editor

import android.app.Application
import com.arthenica.ffmpegkit.FFmpegKitConfig

class PhonkSkullApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FFmpegKitConfig.enableLogCallback(null)
    }
}
