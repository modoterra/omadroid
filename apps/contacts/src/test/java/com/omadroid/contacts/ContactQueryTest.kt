package com.omadroid.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactQueryTest {
    @Test
    fun sortsByDisplayNameIgnoringCase() {
        val sorted =
            sortContactsByName(
                listOf(
                    summary(3, "zeta"),
                    summary(1, "Ada"),
                    summary(2, "bob"),
                ),
            )

        assertEquals(listOf("Ada", "bob", "zeta"), sorted.map { it.displayName })
    }

    @Test
    fun blankNamesSortLast() {
        val sorted =
            sortContactsByName(
                listOf(
                    summary(2, "   "),
                    summary(1, "Ada"),
                    summary(3, ""),
                ),
            )

        assertEquals(listOf("Ada", "   ", ""), sorted.map { it.displayName })
    }

    @Test
    fun filterMatchesNameSubstring() {
        val contacts =
            listOf(
                summary(1, "Ada Lovelace", phone = "555-0100"),
                summary(2, "Alan Turing", phone = "555-0199"),
            )

        assertEquals(listOf("Ada Lovelace"), filterContacts(contacts, "love").map { it.displayName })
    }

    @Test
    fun filterMatchesPhoneDigits() {
        val contacts =
            listOf(
                summary(1, "Ada", phone = "+1 (555) 0100"),
                summary(2, "Alan", phone = "555-0199"),
            )

        assertEquals(listOf("Alan"), filterContacts(contacts, "019-9").map { it.displayName })
    }

    @Test
    fun blankQueryKeepsOrder() {
        val contacts = listOf(summary(1, "Ada"), summary(2, "Alan"))
        assertEquals(contacts, filterContacts(contacts, "  "))
    }

    @Test
    fun initialsUseFirstAndLastWord() {
        assertEquals("AL", contactInitials("Ada Lovelace"))
        assertEquals("A", contactInitials("Ada"))
        assertEquals("?", contactInitials("   "))
    }

    @Test
    fun displayLabelFallsBackWhenBlank() {
        assertEquals("Ada", displayLabel("Ada"))
        assertEquals("Unnamed", displayLabel("  "))
    }

    @Test
    fun normalizeDraftRequiresAName() {
        assertEquals(ContactDraft("Ada Lovelace", "555-0100"), normalizeDraft(" Ada Lovelace ", " 555-0100 "))
        assertNull(normalizeDraft("   ", "555-0100"))
    }

    private fun summary(
        id: Long,
        name: String,
        phone: String? = null,
    ): ContactSummary = ContactSummary(id, "key-$id", name, phone = phone)
}
