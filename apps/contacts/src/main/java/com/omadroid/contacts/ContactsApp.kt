package com.omadroid.contacts

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.omadroid.compose.Block
import com.omadroid.compose.Cell
import com.omadroid.compose.Chrome
import com.omadroid.compose.Compose
import com.omadroid.compose.Direction
import com.omadroid.compose.Empty
import com.omadroid.compose.Glyph
import com.omadroid.compose.Input
import com.omadroid.compose.Line
import com.omadroid.compose.LocalGridStyle
import com.omadroid.compose.Page
import com.omadroid.compose.Rule
import com.omadroid.compose.Slot
import com.omadroid.compose.Stack
import com.omadroid.compose.Text
import com.omadroid.compose.cellDp
import com.omadroid.compose.spaceDp
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.launcher.widget.IconGlyphs

sealed class ContactsScreen {
    data object List : ContactsScreen()

    data class Detail(val contactId: Long) : ContactsScreen()

    data class Edit(val contactId: Long?) : ContactsScreen()
}

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
                    Empty(
                        message = "Contacts needs permission to read and write contacts.",
                        action = "Grant",
                        onAction = onRequestPermission,
                    )
                contacts.isEmpty() ->
                    Empty(
                        message = "No contacts",
                        action = "Add",
                        onAction = onAdd,
                    )
                visible.isEmpty() -> Empty(message = "No matches")
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
                Empty(message = "No contacts")
            } else {
                Page {
                    Block {
                        IdentityRow(detail.displayName, detail.photoUri)
                    }
                    if (detail.phones.isEmpty() && detail.emails.isEmpty()) {
                        Line("No phone or email")
                    }
                    detail.phones.forEach { number ->
                        Block { InfoRow(IconGlyphs.PHONE, number) }
                    }
                    detail.emails.forEach { address ->
                        Block { InfoRow(IconGlyphs.ENVELOPE, address) }
                    }
                    Block(onClick = onDelete, description = "Delete") {
                        Stack(Direction.Horizontal, Modifier.height(style.cellDp())) {
                            Node(Slot.square) {
                                Cell(align = Alignment.Center) {
                                    Glyph(IconGlyphs.TRASH, color = style.colors.red)
                                }
                            }
                            Node(Slot.grow()) {
                                Cell(description = "Delete") {
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
            Cell(
                modifier =
                    Modifier
                        .padding(style.spaceDp())
                        .background(Color(if (valid) style.colors.accent else style.colors.lighterBackground)),
                align = Alignment.Center,
                onClick = if (valid) {
                    { onSave(name, phone) }
                } else {
                    null
                },
                description = "Save",
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
private fun ContactList(
    contacts: List<ContactSummary>,
    onOpen: (Long) -> Unit,
) {
    Page {
        contacts.forEachIndexed { index, contact ->
            val label = displayLabel(contact.displayName)
            Block(onClick = { onOpen(contact.id) }, description = label) {
                ContactRow(contact)
            }
            if (index < contacts.lastIndex) {
                Rule()
            }
        }
    }
}

@Composable
private fun ContactRow(contact: ContactSummary) {
    val style = LocalGridStyle.current
    val label = displayLabel(contact.displayName)
    Stack(Direction.Horizontal, Modifier.height(style.cellDp())) {
        Node(Slot.square) {
            Avatar(contact.displayName, contact.photoUri)
        }
        Node(Slot.grow()) {
            Cell(description = label) {
                Text(label, description = label, align = TextAlign.Start)
            }
        }
        if (!contact.phone.isNullOrBlank()) {
            Node(Slot.fit) {
                Cell(align = Alignment.CenterEnd, description = contact.phone) {
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
    val label = displayLabel(name)
    Stack(Direction.Horizontal, Modifier.height(style.cellDp() * 2)) {
        Node(Slot.square) {
            Avatar(name, photoUri)
        }
        Node(Slot.grow()) {
            Cell(description = label) {
                Text(label, description = label, align = TextAlign.Start)
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
    Stack(Direction.Horizontal, Modifier.height(style.cellDp())) {
        Node(Slot.square) {
            Cell(align = Alignment.Center) {
                Glyph(glyph, color = style.colors.muted)
            }
        }
        Node(Slot.grow()) {
            Cell(description = value) {
                Text(value, description = value, align = TextAlign.Start)
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
    Cell(
        modifier =
            Modifier
                .padding(style.spaceDp())
                .background(Color(style.colors.lighterBackground)),
        align = Alignment.Center,
        description = displayLabel(name),
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
