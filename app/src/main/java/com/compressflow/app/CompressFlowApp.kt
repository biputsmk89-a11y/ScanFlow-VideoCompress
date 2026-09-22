package com.compressflow.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class CompressFlowApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
