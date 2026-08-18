package com.omadroid.contacts

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omadroid.compose.Compose
import com.omadroid.compose.Direction
import com.omadroid.compose.Glyph
import com.omadroid.compose.IconButton
import com.omadroid.compose.Input
import com.omadroid.compose.LocalGridStyle
import com.omadroid.compose.NoFling
import com.omadroid.compose.NoOverscroll
import com.omadroid.compose.Slot
import com.omadroid.compose.Stack
import com.omadroid.compose.StackScope
import com.omadroid.compose.Text
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.launcher.widget.IconGlyphs

@Composable
fun ContactsApp(
    style: GridStyle,
    screen: ContactsScreen,
    query: String,
    contacts: List<ContactSummary>,
    detail: ContactDetail?,
    permitted: Boolean,
    onQuery: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    onSave: (Long?, String, String) -> Boolean,
    onDelete: (Long) -> Unit,
) {
    Compose(style) {
        when (screen) {
            ContactsScreen.List ->
                ListScreen(
                    query = query,
                    contacts = contacts,
                    permitted = permitted,
                    onQuery = onQuery,
                    onRequestPermission = onRequestPermission,
                    onOpen = onOpen,
                    onAdd = onAdd,
                )
            is ContactsScreen.Detail ->
                DetailScreen(
                    detail = detail,
                    onBack = onBack,
                    onEdit = { onEdit(screen.contactId) },
                    onDelete = { onDelete(screen.contactId) },
                )
            is ContactsScreen.Edit ->
                EditScreen(
                    existing = screen.contactId?.let { id -> if (detail?.id == id) detail else null },
                    onBack = onBack,
                    onSave = { name, phone -> onSave(screen.contactId, name, phone) },
                )
        }
    }
}

@Composable
private fun ListScreen(
    query: String,
    contacts: List<ContactSummary>,
    permitted: Boolean,
    onQuery: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    val visible = filterContacts(contacts, query)
    Stack(Direction.Vertical, gap = false) {
        Chrome(
            title = "Contacts",
            leading = null,
            trailing = if (permitted) IconGlyphs.PLUS else null,
            trailingDescription = "Add",
            onTrailing = if (permitted) onAdd else null,
        )
        if (permitted) {
            Stack(Direction.Horizontal, slot = Slot.units(1), pad = true) {
                Input(
                    value = query,
                    onValueChange = onQuery,
                    hint = "Search",
                    icon = IconGlyphs.SEARCH,
                    imeAction = ImeAction.Search,
                )
            }
        }
        Node(Slot.grow()) {
            when {
                !permitted ->
                    EmptyState(
                        message = "Contacts needs permission to read and write contacts.",
                        action = "Grant",
                        onAction = onRequestPermission,
                    )
                contacts.isEmpty() ->
                    EmptyState(
                        message = "No contacts",
                        action = "Add",
                        onAction = onAdd,
                    )
                visible.isEmpty() -> EmptyState(message = "No matches")
                else -> ContactList(visible, onOpen)
            }
        }
    }
}

@Composable
private fun DetailScreen(
    detail: ContactDetail?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val style = LocalGridStyle.current
    Stack(Direction.Vertical, gap = false) {
        Chrome(
            title = displayLabel(detail?.displayName.orEmpty()),
            leading = IconGlyphs.BACK,
            leadingDescription = "Back",
            onLeading = onBack,
            trailing = IconGlyphs.PENCIL,
            trailingDescription = "Edit",
            onTrailing = if (detail != null) onEdit else null,
        )
        Node(Slot.grow()) {
            if (detail == null) {
                EmptyState(message = "No contacts")
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState(), overscrollEffect = NoOverscroll, flingBehavior = NoFling),
                ) {
                    IdentityRow(detail.displayName, detail.photoUri)
                    if (detail.phones.isEmpty() && detail.emails.isEmpty()) {
                        StatusRow("No phone or email")
                    }
                    detail.phones.forEach { number ->
                        InfoRow(IconGlyphs.PHONE, number)
                    }
                    detail.emails.forEach { address ->
                        InfoRow(IconGlyphs.ENVELOPE, address)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(with(LocalDensity.current) { style.cellPx.toDp() })
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDelete,
                            )
                            .semantics { contentDescription = "Delete" },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Stack(Direction.Horizontal) {
                            Node(Slot.square) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Glyph(IconGlyphs.TRASH, color = style.colors.red)
                                }
                            }
                            Node(Slot.grow()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                    Text("Delete", color = style.colors.red, description = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditScreen(
    existing: ContactDetail?,
    onBack: () -> Unit,
    onSave: (String, String) -> Boolean,
) {
    val style = LocalGridStyle.current
    var name by remember(existing?.id) { mutableStateOf(existing?.displayName.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phones?.firstOrNull().orEmpty()) }
    val valid = normalizeDraft(name, phone) != null
    Stack(Direction.Vertical, gap = false) {
        Chrome(
            title = if (existing == null) "Add" else "Edit",
            leading = IconGlyphs.BACK,
            leadingDescription = "Back",
            onLeading = onBack,
        )
        Stack(Direction.Horizontal, slot = Slot.units(1), pad = true) {
            Input(
                value = name,
                onValueChange = { name = it },
                hint = "Name",
                icon = IconGlyphs.USER,
                autoFocus = existing == null,
                imeAction = ImeAction.Next,
            )
        }
        Stack(Direction.Horizontal, slot = Slot.units(1), pad = true) {
            Input(
                value = phone,
                onValueChange = { phone = it },
                hint = "Phone",
                icon = IconGlyphs.PHONE,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Phone,
                onSubmit = { if (valid) onSave(name, phone) },
            )
        }
        Node(Slot.units(1)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(with(LocalDensity.current) { style.spacePx.toDp() })
                    .background(Color(if (valid) style.colors.accent else style.colors.lighterBackground))
                    .clickable(
                        enabled = valid,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSave(name, phone) },
                    )
                    .semantics { contentDescription = "Save" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Save",
                    color = if (valid) style.colors.background else style.colors.muted,
                    description = "Save",
                )
            }
        }
        Node(Slot.grow()) {}
    }
}

@Composable
private fun StackScope.Chrome(
    title: String,
    leading: String?,
    leadingDescription: String = "",
    onLeading: (() -> Unit)? = null,
    trailing: String? = null,
    trailingDescription: String = "",
    onTrailing: (() -> Unit)? = null,
) {
    val style = LocalGridStyle.current
    Node(Slot.units(1)) {
        Box(Modifier.fillMaxSize().background(Color(style.colors.lighterBackground))) {
            Stack(Direction.Horizontal) {
                if (leading != null && onLeading != null) {
                    Node(Slot.square) {
                        IconButton(leading, leadingDescription, onClick = onLeading)
                    }
                }
                Node(Slot.grow()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        Text(title, description = title, align = TextAlign.Start)
                    }
                }
                if (trailing != null && onTrailing != null) {
                    Node(Slot.square) {
                        IconButton(trailing, trailingDescription, color = style.colors.accent, onClick = onTrailing)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactList(
    contacts: List<ContactSummary>,
    onOpen: (Long) -> Unit,
) {
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val row = with(density) { style.cellPx.toDp() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), overscrollEffect = NoOverscroll, flingBehavior = NoFling),
    ) {
        contacts.forEachIndexed { index, contact ->
            Box(
                Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onOpen(contact.id) },
                    )
                    .semantics { contentDescription = displayLabel(contact.displayName) },
            ) {
                ContactRow(contact, row)
            }
            if (index < contacts.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = with(density) { style.spacePx.toDp() })
                        .height(1.dp)
                        .background(Color(style.colors.muted).copy(alpha = 0.2f)),
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactSummary,
    row: androidx.compose.ui.unit.Dp,
) {
    val style = LocalGridStyle.current
    Stack(Direction.Horizontal, Modifier.height(row)) {
        Node(Slot.square) {
            Avatar(contact.displayName, contact.photoUri)
        }
        Node(Slot.grow()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                Text(displayLabel(contact.displayName), description = displayLabel(contact.displayName), align = TextAlign.Start)
            }
        }
        if (!contact.phone.isNullOrBlank()) {
            Node(Slot.fit) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    Text(contact.phone, color = style.colors.muted, description = contact.phone, align = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun IdentityRow(
    name: String,
    photoUri: String?,
) {
    val style = LocalGridStyle.current
    val row = with(LocalDensity.current) { (style.cellPx * 2).toDp() }
    Stack(Direction.Horizontal, Modifier.height(row)) {
        Node(Slot.square) {
            Avatar(name, photoUri)
        }
        Node(Slot.grow()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                Text(displayLabel(name), description = displayLabel(name), align = TextAlign.Start)
            }
        }
    }
}

@Composable
private fun InfoRow(
    glyph: String,
    value: String,
) {
    val style = LocalGridStyle.current
    val row = with(LocalDensity.current) { style.cellPx.toDp() }
    Stack(Direction.Horizontal, Modifier.height(row)) {
        Node(Slot.square) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Glyph(glyph, color = style.colors.muted)
            }
        }
        Node(Slot.grow()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                Text(value, description = value, align = TextAlign.Start)
            }
        }
    }
}

@Composable
private fun StatusRow(message: String) {
    val style = LocalGridStyle.current
    val row = with(LocalDensity.current) { style.cellPx.toDp() }
    Box(
        Modifier
            .fillMaxWidth()
            .height(row),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(message, color = style.colors.muted, description = message, align = TextAlign.Start)
    }
}

@Composable
private fun EmptyState(
    message: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val style = LocalGridStyle.current
    Stack(Direction.Vertical) {
        Node(Slot.grow()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(message, color = style.colors.muted, description = message)
            }
        }
        if (action != null && onAction != null) {
            Node(Slot.units(1)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(with(LocalDensity.current) { style.spacePx.toDp() })
                        .background(Color(style.colors.lighterBackground))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAction,
                        )
                        .semantics { contentDescription = action },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(action, color = style.colors.accent, description = action)
                }
            }
        }
    }
}

@Composable
private fun Avatar(
    name: String,
    photoUri: String?,
) {
    val style = LocalGridStyle.current
    val photo = rememberContactPhoto(photoUri)
    Box(
        Modifier
            .fillMaxSize()
            .padding(with(LocalDensity.current) { style.spacePx.toDp() })
            .background(Color(style.colors.lighterBackground)),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = displayLabel(name),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(contactInitials(name), color = style.colors.accent, description = displayLabel(name))
        }
    }
}

@Composable
private fun rememberContactPhoto(uri: String?): ImageBitmap? {
    val resolver = LocalContext.current.contentResolver
    return remember(uri) {
        if (uri.isNullOrBlank()) {
            null
        } else {
            try {
                resolver.openInputStream(Uri.parse(uri))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
