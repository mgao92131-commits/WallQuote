package com.example.wallquote.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.wallquote.data.local.JsonConfig
import com.example.wallquote.data.local.toJson
import com.example.wallquote.domain.model.HorizontalTextAlignment
import com.example.wallquote.domain.model.SystemFontFamily
import com.example.wallquote.domain.model.TextStyleConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RecentTextStyleRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var repo: DataStorePreferencesRecentTextStyleRepository

    @Before
    fun setUp() = runBlocking {
        repo = DataStorePreferencesRecentTextStyleRepository(
            context,
            "recent-test-${UUID.randomUUID()}.preferences_pb",
        )
        repo.clear()
    }

    @Test
    fun get_withNothingSaved_returnsNull() = runBlocking {
        assertNull(repo.get())
        assertEquals(emptyList<TextStyleConfig>(), repo.getAll())
    }

    @Test
    fun saveThenGet_roundTripsTheStyle() = runBlocking {
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
    fun save_pushesMruAndKeepsPrevious() = runBlocking {
        repo.save(TextStyleConfig(colorHex = "#111111"))
        repo.save(TextStyleConfig(colorHex = "#222222"))

        assertEquals("#222222", repo.get()?.colorHex)
        assertEquals(listOf("#222222", "#111111"), repo.getAll().map { it.colorHex })
    }

    @Test
    fun save_dedupesIdenticalStyleToFront() = runBlocking {
        val first = TextStyleConfig(colorHex = "#111111", textSizeSp = 20f)
        val second = TextStyleConfig(colorHex = "#222222", textSizeSp = 24f)
        repo.save(first)
        repo.save(second)
        repo.save(first)

        assertEquals(listOf("#111111", "#222222"), repo.getAll().map { it.colorHex })
    }

    @Test
    fun clear_removesSavedStyle() = runBlocking {
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

    @Test
    fun decodeRecentTextStylesJson_wrapsLegacySingleObject() {
        val style = TextStyleConfig(colorHex = "#ABCDEF", textSizeSp = 24f)
        val decoded = decodeRecentTextStylesJson(style.toJson())
        assertEquals(listOf(style), decoded)
    }

    @Test
    fun decodeRecentTextStylesJson_readsArray() {
        val styles = listOf(
            TextStyleConfig(colorHex = "#111111"),
            TextStyleConfig(colorHex = "#222222"),
        )
        val json = JsonConfig.json.encodeToString(styles)
        assertEquals(styles, decodeRecentTextStylesJson(json))
    }
}
