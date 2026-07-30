package com.example.wallquote.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.example.wallquote.data.local.AppDatabase
import com.example.wallquote.data.local.CollectionDao
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.data.repository.CollectionRepositoryImpl
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
        texts = listOf("Line A", "Line B"),
        textStyle = TextStyleConfig(colorHex = "#FFFFFF"),
    )

    @Test
    fun upsertAndObserve() = runTest {
        val id = repository.upsertCollection(sampleConfig())
        val list = repository.observeOrderedCollections().first()
        assertEquals(1, list.size)
        assertEquals(id, list[0].id)
        assertEquals(listOf("Line A", "Line B"), list[0].texts)
    }

    @Test
    fun updateReplacesTextLines() = runTest {
        val id = repository.upsertCollection(sampleConfig())
        repository.upsertCollection(
            sampleConfig().copy(id = id, texts = listOf("Only one")),
        )
        val loaded = repository.getCollection(id)
        assertEquals(listOf("Only one"), loaded?.texts)
    }

    @Test
    fun deleteRemovesCollection() = runTest {
        val id = repository.upsertCollection(sampleConfig())
        repository.deleteCollection(id)
        assertTrue(repository.observeOrderedCollections().first().isEmpty())
        assertNull(dao.getWithLines(id))
    }
}
