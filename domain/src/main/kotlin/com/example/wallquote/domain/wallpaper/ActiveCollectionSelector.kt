package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.QuoteLine

/**
 * Filters and sorts collections eligible for wallpaper playback at [minuteOfDay].
 */
object ActiveCollectionSelector {

    fun select(
        collections: List<CollectionConfig>,
        minuteOfDay: Int,
    ): List<CollectionConfig> =
        collections
            .asSequence()
            .filter { it.schedule.contains(minuteOfDay) }
            .map { collection ->
                collection.copy(
                    lines = playableLines(collection.lines),
                )
            }
            .filter { it.lines.isNotEmpty() }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .toList()

    fun playableLines(lines: List<QuoteLine>): List<QuoteLine> =
        lines
            .filter { it.text.isNotBlank() }
            .sortedWith(compareBy({ it.displayOrder }, { it.id }))
}
