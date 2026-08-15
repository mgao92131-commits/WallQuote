package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RenameCollectionUseCaseTest {

    @Test
    fun invoke_updatesName() = runBlocking {
        val repo = FakeRepo(
            CollectionConfig(
                id = 1L,
                name = "旧名称",
                schedule = DailyTimeRange(0, 0),
                background = BackgroundSpec.Solid("#000000"),
                lines = listOf(QuoteLine(id = 1, text = "quote", displayOrder = 0)),
                textStyle = TextStyleConfig(),
            ),
        )
        RenameCollectionUseCase(repo)(1L, "  新名称  ")
        assertEquals("新名称", repo.saved?.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invoke_blankName_throws() = runBlocking {
        val repo = FakeRepo(
            CollectionConfig(
                id = 1L,
                name = "旧名称",
                schedule = DailyTimeRange(0, 0),
                background = BackgroundSpec.Solid("#000000"),
                lines = listOf(QuoteLine(id = 1, text = "quote", displayOrder = 0)),
                textStyle = TextStyleConfig(),
            ),
        )
        RenameCollectionUseCase(repo)(1L, "   ")
    }

    private class FakeRepo(private var current: CollectionConfig) : CollectionRepository {
        var saved: CollectionConfig? = null
        override fun observeOrderedCollections(): Flow<List<CollectionConfig>> = flowOf(listOf(current))
        override suspend fun getCollection(id: Long): CollectionConfig? = current.takeIf { it.id == id }
        override suspend fun upsertCollection(config: CollectionConfig): Long {
            saved = config
            current = config
            return config.id
        }
        override suspend fun deleteCollection(id: Long) = Unit
    }
}
