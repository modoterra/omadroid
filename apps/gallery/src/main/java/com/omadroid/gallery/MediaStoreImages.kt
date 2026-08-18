package com.omadroid.gallery

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

fun mediaUri(id: Long): Uri =
    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

object GalleryPermissions {
    fun required(): String =
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun granted(context: Context): Boolean =
        context.checkSelfPermission(required()) == PackageManager.PERMISSION_GRANTED
}

object MediaStoreImages {
    private val projection =
        arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )

    fun query(resolver: ContentResolver): List<MediaImage> {
        val sort = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        return resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sort,
        )?.use { read(it) } ?: emptyList()
    }

    fun read(cursor: Cursor): List<MediaImage> {
        val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val takenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
        val addedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
        val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        if (idCol < 0) {
            return emptyList()
        }
        val images = ArrayList<MediaImage>(cursor.count.coerceAtLeast(0))
        while (cursor.moveToNext()) {
            images +=
                MediaImages.image(
                    id = cursor.getLong(idCol),
                    displayName = cursor.optionalString(nameCol),
                    dateTakenMillis = cursor.optionalLong(takenCol),
                    dateAddedSeconds = cursor.optionalLong(addedCol),
                    bucketId = cursor.optionalLong(bucketIdCol),
                    bucketDisplayName = cursor.optionalString(bucketNameCol),
                )
        }
        return images
    }

    private fun Cursor.optionalLong(column: Int): Long =
        if (column < 0 || isNull(column)) 0L else getLong(column)

    private fun Cursor.optionalString(column: Int): String? =
        if (column < 0 || isNull(column)) null else getString(column)
}
