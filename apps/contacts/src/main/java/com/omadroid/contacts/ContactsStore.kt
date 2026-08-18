package com.omadroid.contacts

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.RawContacts

interface ContactsStore {
    fun list(): List<ContactSummary>

    fun detail(id: Long): ContactDetail?

    fun insert(draft: ContactDraft): Long?

    fun update(id: Long, draft: ContactDraft): Boolean

    fun delete(id: Long): Boolean
}

class ContactsContractStore(
    private val resolver: ContentResolver,
) : ContactsStore {
    override fun list(): List<ContactSummary> {
        val phones = primaryPhones()
        val contacts = mutableListOf<ContactSummary>()
        query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ),
        ) { cursor ->
            val id = cursor.long(ContactsContract.Contacts._ID) ?: return@query
            contacts.add(
                ContactSummary(
                    id = id,
                    lookupKey = cursor.string(ContactsContract.Contacts.LOOKUP_KEY).orEmpty(),
                    displayName = cursor.string(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY).orEmpty(),
                    photoUri = cursor.string(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI),
                    phone = phones[id],
                ),
            )
        }
        return sortContactsByName(contacts)
    }

    override fun detail(id: Long): ContactDetail? {
        var lookupKey = ""
        var displayName = ""
        var photoUri: String? = null
        var found = false
        query(
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id),
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ),
        ) { cursor ->
            found = true
            lookupKey = cursor.string(ContactsContract.Contacts.LOOKUP_KEY).orEmpty()
            displayName = cursor.string(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY).orEmpty()
            photoUri = cursor.string(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
        }
        if (!found) {
            return null
        }
        return ContactDetail(
            id = id,
            lookupKey = lookupKey,
            displayName = displayName,
            photoUri = photoUri,
            phones = phonesFor(id),
            emails = emailsFor(id),
        )
    }

    override fun insert(draft: ContactDraft): Long? {
        val rawUri =
            resolver.insert(
                RawContacts.CONTENT_URI,
                ContentValues().apply {
                    putNull(RawContacts.ACCOUNT_TYPE)
                    putNull(RawContacts.ACCOUNT_NAME)
                },
            ) ?: return null
        val rawId = ContentUris.parseId(rawUri)
        insertName(rawId, draft.displayName)
        if (draft.phone.isNotEmpty()) {
            insertPhone(rawId, draft.phone)
        }
        return contactIdForRaw(rawId)
    }

    override fun update(id: Long, draft: ContactDraft): Boolean {
        val rawId = firstRawContactId(id) ?: return false
        upsertName(id, rawId, draft.displayName)
        upsertPhone(id, rawId, draft.phone)
        return true
    }

    override fun delete(id: Long): Boolean {
        val deleted =
            resolver.delete(
                RawContacts.CONTENT_URI,
                "${RawContacts.CONTACT_ID}=?",
                arrayOf(id.toString()),
            )
        return deleted > 0
    }

    private fun primaryPhones(): Map<Long, String> {
        val phones = linkedMapOf<Long, String>()
        query(
            Phone.CONTENT_URI,
            arrayOf(Phone.CONTACT_ID, Phone.NUMBER, Phone.IS_SUPER_PRIMARY, Phone.IS_PRIMARY),
            sort = "${Phone.IS_SUPER_PRIMARY} DESC, ${Phone.IS_PRIMARY} DESC",
        ) { cursor ->
            val id = cursor.long(Phone.CONTACT_ID) ?: return@query
            if (id in phones) {
                return@query
            }
            val number = cursor.string(Phone.NUMBER)?.trim().orEmpty()
            if (number.isNotEmpty()) {
                phones[id] = number
            }
        }
        return phones
    }

    private fun phonesFor(contactId: Long): List<String> {
        val phones = mutableListOf<String>()
        query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER),
            "${Phone.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
        ) { cursor ->
            val number = cursor.string(Phone.NUMBER)?.trim().orEmpty()
            if (number.isNotEmpty() && number !in phones) {
                phones.add(number)
            }
        }
        return phones
    }

    private fun emailsFor(contactId: Long): List<String> {
        val emails = mutableListOf<String>()
        query(
            Email.CONTENT_URI,
            arrayOf(Email.ADDRESS),
            "${Email.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
        ) { cursor ->
            val address = cursor.string(Email.ADDRESS)?.trim().orEmpty()
            if (address.isNotEmpty() && address !in emails) {
                emails.add(address)
            }
        }
        return emails
    }

    private fun insertName(rawId: Long, name: String) {
        resolver.insert(
            ContactsContract.Data.CONTENT_URI,
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                put(StructuredName.DISPLAY_NAME, name)
            },
        )
    }

    private fun insertPhone(rawId: Long, phone: String) {
        resolver.insert(
            ContactsContract.Data.CONTENT_URI,
            ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                put(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                put(Phone.NUMBER, phone)
                put(Phone.TYPE, Phone.TYPE_MOBILE)
            },
        )
    }

    private fun upsertName(contactId: Long, rawId: Long, name: String) {
        val updated =
            resolver.update(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply { put(StructuredName.DISPLAY_NAME, name) },
                "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                arrayOf(contactId.toString(), StructuredName.CONTENT_ITEM_TYPE),
            )
        if (updated == 0) {
            insertName(rawId, name)
        }
    }

    private fun upsertPhone(contactId: Long, rawId: Long, phone: String) {
        var dataId: Long? = null
        query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(contactId.toString(), Phone.CONTENT_ITEM_TYPE),
        ) { cursor ->
            if (dataId == null) {
                dataId = cursor.long(ContactsContract.Data._ID)
            }
        }
        when {
            phone.isEmpty() && dataId != null -> {
                resolver.delete(
                    ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId!!),
                    null,
                    null,
                )
            }
            phone.isNotEmpty() && dataId != null -> {
                resolver.update(
                    ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId!!),
                    ContentValues().apply { put(Phone.NUMBER, phone) },
                    null,
                    null,
                )
            }
            phone.isNotEmpty() -> insertPhone(rawId, phone)
        }
    }

    private fun firstRawContactId(contactId: Long): Long? {
        var rawId: Long? = null
        query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts._ID),
            "${RawContacts.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
        ) { cursor ->
            if (rawId == null) {
                rawId = cursor.long(RawContacts._ID)
            }
        }
        return rawId
    }

    private fun contactIdForRaw(rawId: Long): Long? {
        var contactId: Long? = null
        query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts.CONTACT_ID),
            "${RawContacts._ID}=?",
            arrayOf(rawId.toString()),
        ) { cursor ->
            contactId = cursor.long(RawContacts.CONTACT_ID)
        }
        return contactId
    }

    private fun query(
        uri: Uri,
        projection: Array<String>,
        selection: String? = null,
        args: Array<String>? = null,
        sort: String? = null,
        each: (Cursor) -> Unit,
    ) {
        resolver.query(uri, projection, selection, args, sort)?.use { cursor ->
            while (cursor.moveToNext()) {
                each(cursor)
            }
        }
    }
}

private fun Cursor.string(column: String): String? {
    val index = getColumnIndex(column)
    if (index < 0 || isNull(index)) {
        return null
    }
    return getString(index)
}

private fun Cursor.long(column: String): Long? {
    val index = getColumnIndex(column)
    if (index < 0 || isNull(index)) {
        return null
    }
    return getLong(index)
}
