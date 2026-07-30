package com.example.wallquote.data.repository

import com.example.wallquote.data.local.CustomStyleDao
import com.example.wallquote.data.local.CustomStyleEntity
import com.example.wallquote.data.local.parseTextStyle
import com.example.wallquote.data.local.toJson
import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.repository.CustomStyleRepository
import com.example.wallquote.domain.style.TextStyleNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomStyleRepositoryImpl @Inject constructor(
    private val dao: CustomStyleDao,
) : CustomStyleRepository {

    override fun observeOrdered(): Flow<List<CustomTextStyle>> =
        dao.observeOrdered().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): CustomTextStyle? =
        dao.getById(id)?.toDomain()

    override suspend fun save(style: CustomTextStyle): Long {
        val normalized = style.copy(style = TextStyleNormalizer.normalize(style.style))
        return if (normalized.id == 0L) {
            val sort = dao.maxSortOrder() + 1
            dao.insert(
                CustomStyleEntity(
                    name = normalized.name,
                    textStyleData = normalized.style.toJson(),
                    sortOrder = if (normalized.sortOrder == 0) sort else normalized.sortOrder,
                ),
            )
        } else {
            val rows = dao.update(
                CustomStyleEntity(
                    id = normalized.id,
                    name = normalized.name,
                    textStyleData = normalized.style.toJson(),
                    sortOrder = normalized.sortOrder,
                ),
            )
            if (rows != 1) throw IllegalStateException("update_failed")
            normalized.id
        }
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            dao.updateSortOrder(id, index)
        }
    }

    override suspend fun existsName(name: String, excludingId: Long?): Boolean =
        dao.countByName(name.trim(), excludingId) > 0

    private fun CustomStyleEntity.toDomain() = CustomTextStyle(
        id = id,
        name = name,
        style = TextStyleNormalizer.normalize(parseTextStyle(textStyleData)),
        sortOrder = sortOrder,
    )
}
