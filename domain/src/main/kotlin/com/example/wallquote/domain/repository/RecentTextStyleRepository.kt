package com.example.wallquote.domain.repository

import com.example.wallquote.domain.model.TextStyleConfig

/**
 * MRU memory of recently used [TextStyleConfig] values (max 6). Not a user-managed
 * style library: no names, no CRUD page. [get] returns the most recent entry so a
 * newly created collection can inherit it.
 */
interface RecentTextStyleRepository {
    suspend fun getAll(): List<TextStyleConfig>

    suspend fun get(): TextStyleConfig? = getAll().firstOrNull()

    suspend fun save(style: TextStyleConfig)

    suspend fun clear()
}
