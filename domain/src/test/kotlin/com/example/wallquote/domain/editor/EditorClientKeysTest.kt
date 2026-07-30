package com.example.wallquote.domain.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorClientKeysTest {

    @Test
    fun temporaryKeysNeverCollideWithPersistedIds() {
        val persisted = listOf(1L, 2L)
        var next = -1L
        fun newKey(): Long {
            val k = next
            next -= 1
            return k
        }
        val keys = persisted + listOf(newKey(), newKey())
        EditorClientKeys.assertUnique(keys)
        assertTrue(keys.filter { EditorClientKeys.isTemporary(it) }.all { it < 0 })
        assertEquals(listOf(1L, 2L, -1L, -2L), keys)
    }
}
