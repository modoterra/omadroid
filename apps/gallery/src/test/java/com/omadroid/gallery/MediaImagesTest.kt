package com.omadroid.gallery

import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaImagesTest {
    @Test
    fun usesDateTakenWhenPresent() {
        val image =
            MediaImages.image(
                id = 1L,
                displayName = "shot.jpg",
                dateTakenMillis = 1_700_000_000_000L,
                dateAddedSeconds = 10L,
                bucketId = 2L,
                bucketDisplayName = "Camera",
            )
        assertEquals(1_700_000_000_000L, image.dateTakenMillis)
        assertEquals("shot.jpg", image.displayName)
        assertEquals("Camera", image.bucketDisplayName)
    }

    @Test
    fun fallsBackToDateAddedSeconds() {
        val image =
            MediaImages.image(
                id = 3L,
                displayName = "  ",
                dateTakenMillis = 0L,
                dateAddedSeconds = 1_700_000L,
                bucketId = 4L,
                bucketDisplayName = null,
            )
        assertEquals(1_700_000_000L, image.dateTakenMillis)
        assertEquals(MediaImages.UNTITLED, image.displayName)
        assertEquals(MediaImages.UNCATEGORIZED, image.bucketDisplayName)
    }

    @Test
    fun groupsByBucketNewestFirst() {
        val older =
            MediaImages.image(1, "a.jpg", 100L, 0L, 10L, "Camera")
        val newer =
            MediaImages.image(2, "b.jpg", 300L, 0L, 10L, "Camera")
        val other =
            MediaImages.image(3, "c.jpg", 200L, 0L, 20L, "Downloads")
        val albums = MediaImages.albums(listOf(older, other, newer))
        assertEquals(2, albums.size)
        assertEquals(10L, albums[0].bucketId)
        assertEquals(listOf(2L, 1L), albums[0].images.map { it.id })
        assertEquals("Downloads", albums[1].name)
        assertEquals(newer, albums[0].cover)
    }

    @Test
    fun findsById() {
        val image = MediaImages.image(9, "n.jpg", 1L, 0L, 1L, "Camera")
        assertEquals(image, MediaImages.find(listOf(image), 9L))
        assertEquals(null, MediaImages.find(listOf(image), 8L))
    }

    @Test
    fun formatsMediumDateInUtc() {
        val millis = Instant.parse("2024-03-15T12:00:00Z").toEpochMilli()
        assertEquals(
            "Mar 15, 2024",
            MediaImages.formatDate(millis, Locale.US, ZoneOffset.UTC),
        )
        assertEquals("", MediaImages.formatDate(0L, Locale.US, ZoneOffset.UTC))
    }

    @Test
    fun sampleSizeHalvesUntilRequestFits() {
        assertEquals(1, MediaImages.bitmapSampleSize(100, 100, 100, 100))
        assertEquals(4, MediaImages.bitmapSampleSize(4000, 3000, 500, 500))
        assertEquals(1, MediaImages.bitmapSampleSize(0, 0, 0, 0))
    }

    @Test
    fun navPushesAndPopsViewer() {
        val start = GalleryNav()
        assertFalse(start.canPop)
        assertEquals(GalleryRoute.Library, start.current)
        val opened = start.push(GalleryRoute.Viewer(7L))
        assertTrue(opened.canPop)
        assertEquals(GalleryRoute.Viewer(7L), opened.current)
        assertEquals(GalleryRoute.Library, opened.pop().current)
    }
}
