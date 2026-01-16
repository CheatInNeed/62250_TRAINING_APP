package com.example.gymlocker.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import java.util.concurrent.ConcurrentHashMap

object AvatarImageCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun get(uri: String): ImageBitmap? = cache[uri]

    fun put(uri: String, bitmap: ImageBitmap) {
        cache[uri] = bitmap
    }

    fun remove(uri: String) {
        cache.remove(uri)
    }

    fun clear() {
        cache.clear()
    }
}
