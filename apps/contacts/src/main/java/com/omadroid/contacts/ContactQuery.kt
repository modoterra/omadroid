package com.omadroid.contacts

data class ContactSummary(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
    val phone: String? = null,
)

data class ContactDetail(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
)

data class ContactDraft(
    val displayName: String,
    val phone: String,
)

fun displayLabel(name: String, unnamed: String = "Unnamed"): String =
    name.trim().ifEmpty { unnamed }

fun contactInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) {
        return "?"
    }
    val first = parts.first().first().uppercaseChar()
    if (parts.size == 1) {
        return first.toString()
    }
    return first.toString() + parts.last().first().uppercaseChar()
}

fun phoneDigits(raw: String): String = raw.filter { it.isDigit() }

fun sortContactsByName(contacts: List<ContactSummary>): List<ContactSummary> {
    val blank = "\uFFFF"
    return contacts.sortedWith(
        compareBy(String.CASE_INSENSITIVE_ORDER) { contact ->
            contact.displayName.trim().ifEmpty { blank }
        },
    )
}

fun filterContacts(contacts: List<ContactSummary>, query: String): List<ContactSummary> {
    val needle = query.trim()
    if (needle.isEmpty()) {
        return contacts
    }
    val digits = phoneDigits(needle)
    return contacts.filter { contact ->
        contact.displayName.contains(needle, ignoreCase = true) ||
            (digits.isNotEmpty() && phoneDigits(contact.phone.orEmpty()).contains(digits))
    }
}

fun normalizeDraft(name: String, phone: String): ContactDraft? {
    val displayName = name.trim()
    if (displayName.isEmpty()) {
        return null
    }
    return ContactDraft(displayName, phone.trim())
}
