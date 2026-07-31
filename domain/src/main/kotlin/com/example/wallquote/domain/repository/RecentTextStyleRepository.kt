package com.example.wallquote.domain.repository

import com.example.wallquote.domain.model.TextStyleConfig

/**
 * Persists the single most-recently-used [TextStyleConfig] so a newly created collection can
 * start from it instead of the hardcoded default. Existing collections always keep their own
 * saved style and are never overwritten from this (see [D-021 replacement decision in
 * DECISIONS.md]).
 */
interface RecentTextStyleRepository {
    suspend fun get(): TextStyleConfig?

    suspend fun save(style: TextStyleConfig)

    suspend fun clear()
}
