package com.example.wallquote.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.example.wallquote.data.local.AppDatabase
import com.example.wallquote.data.local.CollectionDao
import com.example.wallquote.data.local.CollectionEntity
import com.example.wallquote.data.local.CollectionTextLineEntity
import com.example.wallquote.data.local.SaveFailedException
import com.example.wallquote.data.local.toEntity
import com.example.wallquote.data.repository.CollectionRepositoryImpl
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.TextStyleConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CollectionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CollectionDao
    private lateinit var repository: CollectionRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.collectionDao()
        repository = CollectionRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleConfig(name: String = "Test") = CollectionConfig(
        name = name,
        schedule = DailyTimeRange(0, 0),
        background = BackgroundSpec.Solid("#112233"),
        lines = listOf(
            QuoteLine(id = 0, text = "Line A", displayOrder = 0),
            QuoteLine(id = 0, text = "Line B", displayOrder = 1),
        ),
        textStyle = TextStyleConfig(colorHex = "#FFFFFF"),
    )

    @Test
    fun upsertAndObserve() = runTest {
        val id = repository.upsertCollection(sampleConfig())
        val list = repository.observeOrderedCollections().first()
        assertEquals(1, list.size)
        assertEquals(id, list[0].id)
        assertEquals(listOf("Line A", "Line B"), list[0].lines.map { it.text })
    }

    @Test
    fun updatePreservesLineIdsWhenTextChanges() = runTest {
        val id = repository.upsertCollection(sampleConfig())
        val loaded = repository.getCollection(id)!!
        val firstId = loaded.lines[0].id
        val secondId = loaded.lines[1].id
        repository.upsertCollection(
            loaded.copy(
                lines = listOf(
                    loaded.lines[0].copy(text = "Line A2"),
                    loaded.lines[1],
                ),
            ),
        )
        val reloaded = repository.getCollection(id)!!
        assertEquals(firstId, reloaded.lines[0].id)
        assertEquals(secondId, reloaded.lines[1].id)
        assertEquals("Line A2", reloaded.lines[0].text)
    }

    @Test
    fun deleteCollection_cascadesLines() = runTest {
        val id = repository.upsertCollection(sampleConfig())
        assertEquals(2, dao.countLinesForCollection(id))
        repository.deleteCollection(id)
        assertTrue(repository.observeOrderedCollections().first().isEmpty())
        assertEquals(0, dao.countLinesForCollection(id))
        assertNull(dao.getWithLines(id))
    }

    @Test
    fun saveCollectionWithLines_rollsBackOnFkViolation() = runTest {
        // Insert succeeds first; orphan line update must fail and leave original intact.
        val id = repository.upsertCollection(sampleConfig())
        val before = repository.getCollection(id)!!
        try {
            dao.saveCollectionWithLines(
                before.toEntity(),
                listOf(
                    CollectionTextLineEntity(
                        id = 777_777,
                        collectionId = id,
                        text = "orphan",
                        displayOrder = 0,
                    ),
                ),
            )
            throw AssertionError("Expected SaveFailedException")
        } catch (_: SaveFailedException) {
            // expected
        }
        val after = repository.getCollection(id)!!
        assertEquals(before.lines.map { it.text }, after.lines.map { it.text })
    }

    @Test
    fun observeOrdersBySortOrderThenId() = runTest {
        val id1 = repository.upsertCollection(sampleConfig("B").copy(sortOrder = 1))
        val id2 = repository.upsertCollection(sampleConfig("A").copy(sortOrder = 1))
        val names = repository.observeOrderedCollections().first().map { it.name }
        assertEquals(listOf("B", "A"), names)
        assertTrue(id1 < id2)
    }

    @Test
    fun updateMissingCollectionFailsTransaction() = runTest {
        try {
            dao.saveCollectionWithLines(
                CollectionEntity(
                    id = 42,
                    name = "missing",
                    startMinuteOfDay = 0,
                    endMinuteOfDay = 0,
                    backgroundType = "solid",
                    backgroundData = """{"type":"solid","colorHex":"#000000"}""",
                    textStyleData = "{}",
                    centerXFraction = 0.5f,
                    centerYFraction = 0.5f,
                    rotation = 0f,
                    sortOrder = 0,
                ),
                emptyList(),
            )
            throw AssertionError("Expected SaveFailedException")
        } catch (_: com.example.wallquote.data.local.SaveFailedException) {
            // expected
        }
        assertTrue(dao.observeAllWithLines().first().isEmpty())
    }

    @Test
    fun updateOrphanLineFailsAndRollsBack() = runTest {
        val id = repository.upsertCollection(sampleConfig())
        val loaded = repository.getCollection(id)!!
        try {
            repository.upsertCollection(
                loaded.copy(
                    lines = listOf(
                        loaded.lines[0],
                        QuoteLine(id = 999_999, text = "ghost", displayOrder = 1),
                    ),
                ),
            )
            throw AssertionError("Expected SaveFailedException")
        } catch (_: Exception) {
            // expected
        }
        val reloaded = repository.getCollection(id)!!
        assertEquals(listOf("Line A", "Line B"), reloaded.lines.map { it.text })
    }
}
