package com.omadroid.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.omadroid.compose.LocalGridStyle

@Composable
fun ContactsHost() {
    val context = LocalContext.current
    val style = LocalGridStyle.current
    val store = remember { ContactsContractStore(context.contentResolver) }
    var screen by remember { mutableStateOf<ContactsScreen>(ContactsScreen.List) }
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<ContactSummary>>(emptyList()) }
    var detail by remember { mutableStateOf<ContactDetail?>(null) }
    var permitted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.WRITE_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED,
        )
    }
    fun reload() {
        if (!permitted) {
            contacts = emptyList()
            detail = null
            return
        }
        try {
            contacts = store.list()
            val current = screen
            if (current is ContactsScreen.Detail) {
                detail = store.detail(current.contactId)
            }
        } catch (_: SecurityException) {
            permitted = false
            contacts = emptyList()
            detail = null
        }
    }
    val ask =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permitted =
                context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.WRITE_CONTACTS) ==
                        PackageManager.PERMISSION_GRANTED
            reload()
        }
    DisposableEffect(store) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reload()
                }
            }
        context.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            observer,
        )
        reload()
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    ContactsApp(
        style = style,
        screen = screen,
        query = query,
        contacts = contacts,
        detail = detail,
        permitted = permitted,
        onQuery = { query = it },
        onRequestPermission = {
            ask.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                ),
            )
        },
        onOpen = { id ->
            screen = ContactsScreen.Detail(id)
            detail = if (permitted) store.detail(id) else null
        },
        onAdd = { screen = ContactsScreen.Edit(null) },
        onEdit = { id ->
            screen = ContactsScreen.Edit(id)
            if (permitted) {
                detail = store.detail(id)
            }
        },
        onBack = {
            screen =
                when (val current = screen) {
                    is ContactsScreen.Edit ->
                        if (current.contactId != null) {
                            ContactsScreen.Detail(current.contactId)
                        } else {
                            ContactsScreen.List
                        }
                    is ContactsScreen.Detail -> ContactsScreen.List
                    ContactsScreen.List -> current
                }
            if (screen is ContactsScreen.List) {
                detail = null
            } else if (screen is ContactsScreen.Detail && permitted) {
                detail = store.detail((screen as ContactsScreen.Detail).contactId)
            }
        },
        onSave = { id, name, phone ->
            val draft = normalizeDraft(name, phone) ?: return@ContactsApp false
            if (!permitted) {
                return@ContactsApp false
            }
            val savedId =
                try {
                    if (id == null) {
                        store.insert(draft)
                    } else if (store.update(id, draft)) {
                        id
                    } else {
                        null
                    }
                } catch (_: SecurityException) {
                    permitted = false
                    null
                } ?: return@ContactsApp false
            reload()
            screen = ContactsScreen.Detail(savedId)
            detail =
                try {
                    store.detail(savedId)
                } catch (_: SecurityException) {
                    null
                }
            true
        },
        onDelete = { id ->
            if (permitted) {
                try {
                    store.delete(id)
                } catch (_: SecurityException) {
                    permitted = false
                }
            }
            screen = ContactsScreen.List
            detail = null
            reload()
        },
    )
}
