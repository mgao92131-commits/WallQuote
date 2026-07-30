package com.example.wallquote.domain.model

data class QuoteLine(
    val id: Long = 0,
    val text: String,
    val displayOrder: Int,
)
