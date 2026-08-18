package com.omadroid.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext


@Composable
fun GalleryHost() {
    val context = LocalContext.current
    var permitted by remember { mutableStateOf(GalleryPermissions.granted(context)) }
    var images by remember { mutableStateOf<List<MediaImage>>(emptyList()) }
    var nav by remember { mutableStateOf(GalleryNav()) }
    fun reload() {
        permitted = GalleryPermissions.granted(context)
        images =
            if (permitted) {
                try {
                    MediaStoreImages.query(context.contentResolver)
                } catch (_: SecurityException) {
                    emptyList()
                }
            } else {
                emptyList()
            }
    }
    val ask =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permitted = granted
            reload()
        }
    LaunchedEffect(Unit) { reload() }
    GalleryApp(
        permitted = permitted,
        images = images,
        nav = nav,
        onRequestPermission = { ask.launch(GalleryPermissions.required()) },
        onOpenAlbum = { bucketId -> nav = nav.push(GalleryRoute.Album(bucketId)) },
        onOpenImage = { imageId -> nav = nav.push(GalleryRoute.Viewer(imageId)) },
        onBack = {
            if (nav.canPop) {
                nav = nav.pop()
            }
        },
    )
}
