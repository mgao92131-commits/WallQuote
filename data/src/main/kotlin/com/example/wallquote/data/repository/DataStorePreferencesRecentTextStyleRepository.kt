package com.example.wallquote.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.wallquote.data.local.JsonConfig
import com.example.wallquote.data.local.toJson
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.repository.RecentTextStyleRepository
import com.example.wallquote.domain.style.TextStyleSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val RECENT_TEXT_STYLE_JSON_KEY = stringPreferencesKey("recent_text_style_json")
private const val DATASTORE_FILE_NAME = "recent_text_style.preferences_pb"

/**
 * Decodes a stored `recent_text_style_json` value, returning `null` (rather than a silent
 * default) if [json] is corrupt/undecodable. Extracted as a top-level function so it is testable
 * without needing to touch DataStore/Robolectric at all.
 */
internal fun decodeRecentTextStyleJson(json: String): TextStyleConfig? =
    runCatching {
        TextStyleSanitizer.sanitize(JsonConfig.json.decodeFromString<TextStyleConfig>(json))
    }.getOrNull()

/**
 * Stores the single most-recently-used [TextStyleConfig] as JSON in DataStore Preferences,
 * replacing the previous custom-style library (see DECISIONS.md, superseding D-021). Reuses the
 * existing [JsonConfig]/[toJson]/[TextStyleSanitizer] kotlinx.serialization helpers so the
 * on-disk shape matches `collections.textStyleData`. Corrupt/undecodable JSON yields `null` from
 * [get] rather than silently falling back to a default style, so callers can distinguish "no
 * recent style yet" from "recent style is corrupt" if they ever need to.
 *
 * The [DataStore] is built lazily per-instance (rather than via the top-level
 * `preferencesDataStore` delegate, which caches a single [DataStore] statically keyed by file
 * name for the process lifetime) so this class stays trivially testable: each test can construct
 * its own repository instance backed by an isolated temp directory without fighting a
 * cross-test-cached singleton. Hilt already scopes this class itself to [Singleton], so
 * production code still only ever has one [DataStore] instance.
 */
@Singleton
class DataStorePreferencesRecentTextStyleRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : RecentTextStyleRepository {

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { File(context.filesDir, "datastore/$DATASTORE_FILE_NAME") },
        )
    }

    override suspend fun get(): TextStyleConfig? {
        val json = dataStore.data.first()[RECENT_TEXT_STYLE_JSON_KEY] ?: return null
        return decodeRecentTextStyleJson(json)
    }

    override suspend fun save(style: TextStyleConfig) {
        dataStore.edit { prefs ->
            prefs[RECENT_TEXT_STYLE_JSON_KEY] = style.toJson()
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(RECENT_TEXT_STYLE_JSON_KEY)
        }
    }
}
