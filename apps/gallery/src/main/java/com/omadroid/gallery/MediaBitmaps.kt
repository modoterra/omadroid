package com.omadroid.gallery

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size

object MediaBitmaps {
    fun thumbnail(resolver: ContentResolver, id: Long, sizePx: Int): Bitmap? {
        val size = sizePx.coerceAtLeast(1)
        val uri = mediaUri(id)
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                resolver.loadThumbnail(uri, Size(size, size), null)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Thumbnails.getThumbnail(
                    resolver,
                    id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null,
                )
            }
        } catch (_: Exception) {
            decodeSampled(resolver, uri, size, size)
        }
    }

    fun full(resolver: ContentResolver, id: Long, maxW: Int, maxH: Int): Bitmap? =
        decodeSampled(resolver, mediaUri(id), maxW.coerceAtLeast(1), maxH.coerceAtLeast(1))

    private fun decodeSampled(
        resolver: ContentResolver,
        uri: Uri,
        reqW: Int,
        reqH: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null
        val opts =
            BitmapFactory.Options().apply {
                inSampleSize = MediaImages.bitmapSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
            }
        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }
}
