package com.example.wallquote.domain.editor

/**
 * Client-side list identity for editor rows.
 * Persisted lines use positive DB ids; unsaved lines use negative temporary keys.
 */
object EditorClientKeys {
    fun isTemporary(clientKey: Long): Boolean = clientKey < 0

    fun assertUnique(keys: List<Long>) {
        require(keys.size == keys.toSet().size) {
            "Duplicate editor clientKeys: $keys"
        }
    }
}
