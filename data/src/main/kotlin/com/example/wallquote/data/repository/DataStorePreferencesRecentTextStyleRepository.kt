package com.example.wallquote.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.wallquote.data.local.JsonConfig
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.repository.RecentTextStyleRepository
import com.example.wallquote.domain.style.RecentTextStyles
import com.example.wallquote.domain.style.TextStyleSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val RECENT_TEXT_STYLE_JSON_KEY = stringPreferencesKey("recent_text_style_json")
private val RECENT_TEXT_STYLES_JSON_KEY = stringPreferencesKey("recent_text_styles_json")
private const val DATASTORE_FILE_NAME = "recent_text_style.preferences_pb"

internal fun decodeRecentTextStyleJson(json: String): TextStyleConfig? =
    runCatching {
        TextStyleSanitizer.sanitize(JsonConfig.json.decodeFromString<TextStyleConfig>(json))
    }.getOrNull()

internal fun decodeRecentTextStylesJson(json: String): List<TextStyleConfig> {
    val trimmed = json.trim()
    if (trimmed.startsWith("[")) {
        return runCatching {
            JsonConfig.json.decodeFromString<List<TextStyleConfig>>(trimmed)
                .map { TextStyleSanitizer.sanitize(it) }
        }.getOrElse { emptyList() }
    }
    return listOfNotNull(decodeRecentTextStyleJson(trimmed))
}

@Singleton
class DataStorePreferencesRecentTextStyleRepository(
    context: Context,
    fileName: String = DATASTORE_FILE_NAME,
) : RecentTextStyleRepository {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, DATASTORE_FILE_NAME)

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { File(context.filesDir, "datastore/$fileName") },
        )
    }

    override suspend fun getAll(): List<TextStyleConfig> {
        val prefs = dataStore.data.first()
        val listJson = prefs[RECENT_TEXT_STYLES_JSON_KEY]
        if (listJson != null) return decodeRecentTextStylesJson(listJson)
        val legacy = prefs[RECENT_TEXT_STYLE_JSON_KEY] ?: return emptyList()
        return listOfNotNull(decodeRecentTextStyleJson(legacy))
    }

    override suspend fun save(style: TextStyleConfig) {
        val next = RecentTextStyles.push(style, getAll())
        dataStore.edit { prefs ->
            prefs[RECENT_TEXT_STYLES_JSON_KEY] = JsonConfig.json.encodeToString(next)
            prefs.remove(RECENT_TEXT_STYLE_JSON_KEY)
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(RECENT_TEXT_STYLES_JSON_KEY)
            prefs.remove(RECENT_TEXT_STYLE_JSON_KEY)
        }
    }
}
