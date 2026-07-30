package com.example.wallquote.domain.model

data class CustomTextStyle(
    val id: Long = 0,
    val name: String,
    val style: TextStyleConfig,
    val sortOrder: Int = 0,
)
