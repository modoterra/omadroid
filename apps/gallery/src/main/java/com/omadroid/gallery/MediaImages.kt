package com.omadroid.gallery

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

data class MediaImage(
    val id: Long,
    val displayName: String,
    val dateTakenMillis: Long,
    val bucketId: Long,
    val bucketDisplayName: String,
)

data class MediaAlbum(
    val bucketId: Long,
    val name: String,
    val images: List<MediaImage>,
) {
    val cover: MediaImage? get() = images.firstOrNull()
}

sealed class GalleryRoute {
    data object Library : GalleryRoute()

    data class Album(val bucketId: Long) : GalleryRoute()

    data class Viewer(val imageId: Long) : GalleryRoute()
}

data class GalleryNav(
    val routes: List<GalleryRoute> = listOf(GalleryRoute.Library),
) {
    val current: GalleryRoute get() = routes.last()
    val canPop: Boolean get() = routes.size > 1

    fun push(route: GalleryRoute): GalleryNav = copy(routes = routes + route)

    fun pop(): GalleryNav = if (canPop) copy(routes = routes.dropLast(1)) else this
}

object MediaImages {
    const val UNTITLED = "Untitled"
    const val UNCATEGORIZED = "Pictures"

    fun image(
        id: Long,
        displayName: String?,
        dateTakenMillis: Long,
        dateAddedSeconds: Long,
        bucketId: Long,
        bucketDisplayName: String?,
    ): MediaImage {
        val taken =
            if (dateTakenMillis > 0L) {
                dateTakenMillis
            } else {
                dateAddedSeconds.coerceAtLeast(0L) * 1000L
            }
        return MediaImage(
            id = id,
            displayName = displayName?.trim().orEmpty().ifEmpty { UNTITLED },
            dateTakenMillis = taken,
            bucketId = bucketId,
            bucketDisplayName = bucketDisplayName?.trim().orEmpty().ifEmpty { UNCATEGORIZED },
        )
    }

    fun albums(images: List<MediaImage>): List<MediaAlbum> {
        return images
            .groupBy { it.bucketId }
            .map { (bucketId, items) ->
                val sorted = items.sortedWith(compareByDescending<MediaImage> { it.dateTakenMillis }.thenBy { it.id })
                MediaAlbum(
                    bucketId = bucketId,
                    name = sorted.first().bucketDisplayName,
                    images = sorted,
                )
            }.sortedWith(
                compareByDescending<MediaAlbum> { it.images.firstOrNull()?.dateTakenMillis ?: 0L }
                    .thenBy { it.name.lowercase(Locale.US) },
            )
    }

    fun find(images: List<MediaImage>, id: Long): MediaImage? = images.firstOrNull { it.id == id }

    fun formatDate(
        millis: Long,
        locale: Locale = Locale.US,
        zone: ZoneId = ZoneOffset.UTC,
    ): String {
        if (millis <= 0L) {
            return ""
        }
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }

    fun bitmapSampleSize(width: Int, height: Int, reqW: Int, reqH: Int): Int {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val rw = reqW.coerceAtLeast(1)
        val rh = reqH.coerceAtLeast(1)
        var sample = 1
        while (w / (sample * 2) >= rw && h / (sample * 2) >= rh) {
            sample *= 2
        }
        return sample
    }
}
