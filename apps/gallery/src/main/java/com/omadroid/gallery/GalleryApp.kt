package com.omadroid.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.omadroid.compose.Cell
import com.omadroid.compose.Chrome
import com.omadroid.compose.Direction
import com.omadroid.compose.Empty
import com.omadroid.compose.LocalGridStyle
import com.omadroid.compose.Slot
import com.omadroid.compose.Stack
import com.omadroid.compose.Text
import com.omadroid.compose.Tiles
import com.omadroid.launcher.widget.IconGlyphs
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GalleryApp(
    permitted: Boolean,
    images: List<MediaImage>,
    nav: GalleryNav,
    onRequestPermission: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenImage: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val albums = remember(images) { MediaImages.albums(images) }
    when {
        !permitted -> Empty(
            message = "Gallery needs access to photos",
            action = "Allow access",
            onAction = onRequestPermission,
        )
        images.isEmpty() -> EmptyPane()
        else ->
            when (val route = nav.current) {
                is GalleryRoute.Library ->
                    LibraryPane(
                        albums = albums,
                        canPop = nav.canPop,
                        onBack = onBack,
                        onOpenAlbum = onOpenAlbum,
                        onOpenImage = onOpenImage,
                    )
                is GalleryRoute.Album -> {
                    val album = albums.firstOrNull { it.bucketId == route.bucketId }
                    ImageGridPane(
                        title = album?.name ?: MediaImages.UNCATEGORIZED,
                        images = album?.images ?: emptyList(),
                        canPop = nav.canPop,
                        onBack = onBack,
                        onOpenImage = onOpenImage,
                    )
                }
                is GalleryRoute.Viewer -> {
                    val image = MediaImages.find(images, route.imageId)
                    ViewerPane(image = image, onBack = onBack)
                }
            }
    }
}

@Composable
private fun EmptyPane() {
    Stack(Direction.Vertical, gap = false) {
        Chrome("Photos")
        Node(Slot.grow()) {
            Empty("No images")
        }
    }
}

@Composable
private fun LibraryPane(
    albums: List<MediaAlbum>,
    canPop: Boolean,
    onBack: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenImage: (Long) -> Unit,
) {
    if (albums.size <= 1) {
        val album = albums.firstOrNull()
        ImageGridPane(
            title = album?.name ?: "Photos",
            images = album?.images ?: emptyList(),
            canPop = canPop,
            onBack = onBack,
            onOpenImage = onOpenImage,
        )
        return
    }
    AlbumGridPane(
        albums = albums,
        canPop = canPop,
        onBack = onBack,
        onOpenAlbum = onOpenAlbum,
    )
}

@Composable
private fun AlbumGridPane(
    albums: List<MediaAlbum>,
    canPop: Boolean,
    onBack: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
) {
    Stack(Direction.Vertical, gap = false) {
        Chrome(
            title = "Albums",
            leading = if (canPop) IconGlyphs.BACK else null,
            leadingDescription = "Back",
            onLeading = if (canPop) onBack else null,
        )
        Node(Slot.grow()) {
            Tiles {
                items(albums, key = { it.bucketId }) { album ->
                    AlbumCell(album, onClick = { onOpenAlbum(album.bucketId) })
                }
            }
        }
    }
}

@Composable
private fun ImageGridPane(
    title: String,
    images: List<MediaImage>,
    canPop: Boolean,
    onBack: () -> Unit,
    onOpenImage: (Long) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val locale = remember { Locale.getDefault() }
    Stack(Direction.Vertical, gap = false) {
        Chrome(
            title = title,
            leading = if (canPop) IconGlyphs.BACK else null,
            leadingDescription = "Back",
            onLeading = if (canPop) onBack else null,
        )
        Node(Slot.grow()) {
            Tiles {
                items(images, key = { it.id }) { image ->
                    val date = MediaImages.formatDate(image.dateTakenMillis, locale, zone)
                    ImageCell(
                        id = image.id,
                        label = date.ifEmpty { image.displayName },
                        onClick = { onOpenImage(image.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerPane(image: MediaImage?, onBack: () -> Unit) {
    val style = LocalGridStyle.current
    val zone = remember { ZoneId.systemDefault() }
    val locale = remember { Locale.getDefault() }
    val name = image?.displayName ?: MediaImages.UNTITLED
    val date = image?.let { MediaImages.formatDate(it.dateTakenMillis, locale, zone) }.orEmpty()
    Stack(Direction.Vertical, gap = false) {
        Chrome(
            title = name,
            leading = IconGlyphs.BACK,
            leadingDescription = "Back",
            onLeading = onBack,
        )
        Node(Slot.grow()) {
            if (image == null) {
                Empty(name)
            } else {
                FullImage(image.id, name)
            }
        }
        if (date.isNotEmpty()) {
            Node(Slot.units(1)) {
                Cell(
                    modifier = Modifier.background(Color(style.colors.darkBackground)),
                    align = Alignment.Center,
                    description = date,
                ) {
                    Text(date, color = style.colors.muted, description = date)
                }
            }
        }
    }
}

@Composable
private fun AlbumCell(album: MediaAlbum, onClick: () -> Unit) {
    val style = LocalGridStyle.current
    val cover = album.cover
    val label = "${album.name} (${album.images.size})"
    Cell(
        modifier = Modifier.aspectRatio(1f).background(Color(style.colors.lighterBackground)),
        onClick = onClick,
        description = label,
    ) {
        if (cover != null) {
            LoadedThumb(cover.id, album.name, Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(style.colors.darkBackground)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(label, color = style.colors.foreground, description = label, align = TextAlign.Start)
        }
    }
}

@Composable
private fun ImageCell(id: Long, label: String, onClick: () -> Unit) {
    val style = LocalGridStyle.current
    Cell(
        modifier = Modifier.aspectRatio(1f).background(Color(style.colors.lighterBackground)),
        align = Alignment.Center,
        onClick = onClick,
        description = label,
    ) {
        LoadedThumb(id, label, Modifier.fillMaxSize(), fallback = label)
    }
}

@Composable
private fun LoadedThumb(
    id: Long,
    description: String,
    modifier: Modifier = Modifier,
    fallback: String? = null,
) {
    val context = LocalContext.current
    val style = LocalGridStyle.current
    val sizePx = style.unitPx * 3
    var bitmap by remember(id, sizePx) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(id, sizePx) {
        bitmap =
            withContext(Dispatchers.IO) {
                MediaBitmaps.thumbnail(context.contentResolver, id, sizePx)?.asImageBitmap()
            }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = description,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else if (fallback != null) {
        Text(fallback, color = style.colors.muted, description = description)
    }
}

@Composable
private fun FullImage(id: Long, description: String) {
    val context = LocalContext.current
    val style = LocalGridStyle.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color(style.colors.darkerBackground)),
        contentAlignment = Alignment.Center,
    ) {
        val reqW = constraints.maxWidth.coerceAtLeast(1)
        val reqH = constraints.maxHeight.coerceAtLeast(1)
        var bitmap by remember(id, reqW, reqH) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(id, reqW, reqH) {
            bitmap =
                withContext(Dispatchers.IO) {
                    MediaBitmaps.full(context.contentResolver, id, reqW, reqH)?.asImageBitmap()
                }
        }
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(description, color = style.colors.muted)
        }
    }
}
