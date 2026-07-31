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
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** P4-014: trimmed + Locale.ROOT-lowercased, matching the unique index on `normalizedName`. */
private fun normalizeStyleName(name: String): String = name.trim().lowercase(Locale.ROOT)

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
                    normalizedName = normalizeStyleName(normalized.name),
                    textStyleData = normalized.style.toJson(),
                    sortOrder = if (normalized.sortOrder == 0) sort else normalized.sortOrder,
                ),
            )
        } else {
            val rows = dao.update(
                CustomStyleEntity(
                    id = normalized.id,
                    name = normalized.name,
                    normalizedName = normalizeStyleName(normalized.name),
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
        dao.reorder(orderedIds)
    }

    override suspend fun existsName(name: String, excludingId: Long?): Boolean =
        dao.countByNormalizedName(normalizeStyleName(name), excludingId) > 0

    private fun CustomStyleEntity.toDomain() = CustomTextStyle(
        id = id,
        name = name,
        style = TextStyleNormalizer.normalize(parseTextStyle(textStyleData)),
        sortOrder = sortOrder,
    )
}
