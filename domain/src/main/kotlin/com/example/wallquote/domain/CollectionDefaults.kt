package com.example.wallquote.domain

import com.example.wallquote.domain.model.QuoteLine

object CollectionDefaults {
    const val NEW_COLLECTION_NAME = "新收藏集"
    const val DEFAULT_SOLID_HEX = "#2E3440"
    val defaultLines: List<QuoteLine> = listOf(
        QuoteLine(id = 0, text = "Focus on the present.", displayOrder = 0),
    )
}
