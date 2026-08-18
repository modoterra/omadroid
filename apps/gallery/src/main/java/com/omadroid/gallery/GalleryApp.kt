package com.omadroid.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.omadroid.compose.Direction
import com.omadroid.compose.IconButton
import com.omadroid.compose.LocalGridStyle
import com.omadroid.compose.NoFling
import com.omadroid.compose.NoOverscroll
import com.omadroid.compose.Slot
import com.omadroid.compose.Spacer
import com.omadroid.compose.Stack
import com.omadroid.compose.Text
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
            !permitted -> PermissionPane(onRequestPermission)
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
private fun PermissionPane(onRequestPermission: () -> Unit) {
    val style = LocalGridStyle.current
    Stack(Direction.Vertical) {
        Spacer()
        Node(Slot.fit) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Gallery needs access to photos", color = style.colors.foreground)
            }
        }
        Node(Slot.units(1)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRequestPermission,
                    )
                    .semantics { contentDescription = "Allow access" },
                contentAlignment = Alignment.Center,
            ) {
                Text("Allow access", color = style.colors.accent, description = "Allow access")
            }
        }
        Spacer()
    }
}

@Composable
private fun EmptyPane() {
    val style = LocalGridStyle.current
    Stack(Direction.Vertical) {
        Node(Slot.units(1)) { ChromeBar("Photos", canPop = false, onBack = {}) }
        Node(Slot.grow()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No images", color = style.colors.muted)
            }
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
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val space = with(density) { style.spacePx.toDp() }
    val minCell = with(density) { (style.unitPx * 3).toDp() }
    Stack(Direction.Vertical, gap = false) {
        Node(Slot.units(1)) { ChromeBar("Albums", canPop, onBack) }
        Node(Slot.grow()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minCell),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(space),
                horizontalArrangement = Arrangement.spacedBy(space),
                verticalArrangement = Arrangement.spacedBy(space),
                flingBehavior = NoFling,
                overscrollEffect = NoOverscroll,
            ) {
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
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val space = with(density) { style.spacePx.toDp() }
    val minCell = with(density) { (style.unitPx * 3).toDp() }
    val zone = remember { ZoneId.systemDefault() }
    val locale = remember { Locale.getDefault() }
    Stack(Direction.Vertical, gap = false) {
        Node(Slot.units(1)) { ChromeBar(title, canPop, onBack) }
        Node(Slot.grow()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minCell),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(space),
                horizontalArrangement = Arrangement.spacedBy(space),
                verticalArrangement = Arrangement.spacedBy(space),
                flingBehavior = NoFling,
                overscrollEffect = NoOverscroll,
            ) {
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
        Node(Slot.units(1)) { ChromeBar(name, canPop = true, onBack = onBack) }
        Node(Slot.grow()) {
            if (image == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(name, color = style.colors.muted)
                }
            } else {
                FullImage(image.id, name)
            }
        }
        if (date.isNotEmpty()) {
            Node(Slot.units(1)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(style.colors.darkBackground)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(date, color = style.colors.muted, description = date)
                }
            }
        }
    }
}

@Composable
private fun ChromeBar(title: String, canPop: Boolean, onBack: () -> Unit) {
    val style = LocalGridStyle.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(style.colors.lighterBackground)),
    ) {
        Stack(Direction.Horizontal) {
            if (canPop) {
                Node(Slot.square) {
                    IconButton(IconGlyphs.BACK, "Back", onClick = onBack)
                }
            }
            Node(Slot.grow()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Text(title, description = title, align = TextAlign.Start)
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
    Box(
        Modifier
            .aspectRatio(1f)
            .background(Color(style.colors.lighterBackground))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
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
    Box(
        Modifier
            .aspectRatio(1f)
            .background(Color(style.colors.lighterBackground))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
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
