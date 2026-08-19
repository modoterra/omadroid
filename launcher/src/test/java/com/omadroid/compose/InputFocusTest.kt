package com.omadroid.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputFocusTest {
    @Test
    fun waitsUntilTheFieldIsExpanded() {
        assertFalse(shouldRequestInputFocus(autoFocus = true, expanded = false))
        assertTrue(shouldRequestInputFocus(autoFocus = true, expanded = true))
    }

    @Test
    fun doesNotFocusWhenClosed() {
        assertFalse(shouldRequestInputFocus(autoFocus = false, expanded = false))
        assertFalse(shouldRequestInputFocus(autoFocus = false, expanded = true))
    }
}
