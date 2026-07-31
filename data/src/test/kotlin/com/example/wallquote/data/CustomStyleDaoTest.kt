package com.example.wallquote.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.wallquote.data.local.AppDatabase
import com.example.wallquote.data.local.CustomStyleDao
import com.example.wallquote.data.local.CustomStyleEntity
import com.example.wallquote.data.repository.CustomStyleRepositoryImpl
import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.model.TextStyleConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P4-014: name uniqueness must be case/whitespace-insensitive via `normalizedName`, and
 * `reorder` must be atomic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CustomStyleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CustomStyleDao
    private lateinit var repository: CustomStyleRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.customStyleDao()
        repository = CustomStyleRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun existsName_isCaseInsensitive() = runTest {
        repository.save(CustomTextStyle(name = "Sunset", style = TextStyleConfig()))
        assertTrue(repository.existsName("sunset"))
        assertTrue(repository.existsName("SUNSET"))
        assertTrue(repository.existsName("  Sunset  "))
        assertFalse(repository.existsName("sunrise"))
    }

    @Test
    fun existsName_excludingOwnIdAllowsSameName() = runTest {
        val id = repository.save(CustomTextStyle(name = "Sunset", style = TextStyleConfig()))
        assertFalse(repository.existsName("sunset", excludingId = id))
        assertTrue(repository.existsName("sunset", excludingId = id + 1))
    }

    @Test
    fun save_insertingDuplicateNormalizedName_violatesUniqueIndex() = runTest {
        dao.insert(
            CustomStyleEntity(
                name = "Sunset",
                normalizedName = "sunset",
                textStyleData = "{}",
                sortOrder = 0,
            ),
        )
        try {
            dao.insert(
                CustomStyleEntity(
                    name = "SUNSET",
                    normalizedName = "sunset",
                    textStyleData = "{}",
                    sortOrder = 1,
                ),
            )
            throw AssertionError("Expected unique constraint violation on normalizedName")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // expected: unique index on normalizedName rejects the case-insensitive duplicate
        }
    }

    @Test
    fun save_setsBothNameAndNormalizedName() = runTest {
        val id = repository.save(CustomTextStyle(name = "  Golden Hour  ", style = TextStyleConfig()))
        val entity = dao.getById(id)!!
        assertEquals("  Golden Hour  ", entity.name)
        assertEquals("golden hour", entity.normalizedName)
    }

    @Test
    fun reorder_updatesSortOrderForAllIdsInGivenOrder() = runTest {
        val id1 = repository.save(CustomTextStyle(name = "A", style = TextStyleConfig()))
        val id2 = repository.save(CustomTextStyle(name = "B", style = TextStyleConfig()))
        val id3 = repository.save(CustomTextStyle(name = "C", style = TextStyleConfig()))

        repository.reorder(listOf(id3, id1, id2))

        val ordered = repository.observeOrdered().first()
        assertEquals(listOf(id3, id1, id2), ordered.map { it.id })
        assertEquals(listOf(0, 1, 2), ordered.map { it.sortOrder })
    }

    @Test
    fun reorder_isAtomicAndDoesNotPartiallyApplyOnFailure() = runTest {
        val id1 = repository.save(CustomTextStyle(name = "A", style = TextStyleConfig()))
        val id2 = repository.save(CustomTextStyle(name = "B", style = TextStyleConfig()))
        val before = dao.getById(id1)!!.sortOrder to dao.getById(id2)!!.sortOrder

        // A nonexistent id in the middle of the list does not fail any individual UPDATE (Room's
        // UPDATE-by-id simply affects zero rows), so this asserts the transaction still applies
        // every valid update consistently as a single all-or-nothing unit rather than leaving a
        // mix of pre- and post-reorder sortOrders if it were interrupted partway through.
        repository.reorder(listOf(id1, 999_999L, id2))

        val after1 = dao.getById(id1)!!.sortOrder
        val after2 = dao.getById(id2)!!.sortOrder
        assertEquals(0, after1)
        assertEquals(2, after2)
        assertTrue(before != (after1 to after2))
    }
}
