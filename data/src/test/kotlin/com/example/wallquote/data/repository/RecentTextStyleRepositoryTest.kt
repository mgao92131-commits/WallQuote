package com.example.wallquote.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.wallquote.data.local.toJson
import com.example.wallquote.domain.model.HorizontalTextAlignment
import com.example.wallquote.domain.model.SystemFontFamily
import com.example.wallquote.domain.model.TextStyleConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the DataStore-backed "recent text style" repository that replaced the custom-style
 * library (see DECISIONS.md, superseding D-021).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RecentTextStyleRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun newRepository() = DataStorePreferencesRecentTextStyleRepository(context)

    @Test
    fun get_withNothingSaved_returnsNull() = runBlocking {
        assertNull(newRepository().get())
    }

    @Test
    fun saveThenGet_roundTripsTheStyle() = runBlocking {
        val repo = newRepository()
        val style = TextStyleConfig(
            colorHex = "#112233",
            textSizeSp = 40f,
            fontFamily = SystemFontFamily.Monospace,
            isBold = true,
            horizontalAlignment = HorizontalTextAlignment.Start,
        )

        repo.save(style)

        assertEquals(style, repo.get())
    }

    @Test
    fun save_overwritesPreviousValue() = runBlocking {
        val repo = newRepository()
        repo.save(TextStyleConfig(colorHex = "#111111"))
        repo.save(TextStyleConfig(colorHex = "#222222"))

        assertEquals("#222222", repo.get()?.colorHex)
    }

    @Test
    fun clear_removesSavedStyle() = runBlocking {
        val repo = newRepository()
        repo.save(TextStyleConfig(colorHex = "#112233"))

        repo.clear()

        assertNull(repo.get())
    }

    @Test
    fun decodeRecentTextStyleJson_withCorruptJson_returnsNull() {
        assertNull(decodeRecentTextStyleJson("{not valid json"))
        assertNull(decodeRecentTextStyleJson(""))
    }

    @Test
    fun decodeRecentTextStyleJson_withValidJson_roundTrips() {
        val style = TextStyleConfig(colorHex = "#ABCDEF", textSizeSp = 24f)

        assertEquals(style, decodeRecentTextStyleJson(style.toJson()))
    }
}
