package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.model.BackgroundSpec

data class SaveCollectionResult(
    val collectionId: Long,
    val savedBackground: BackgroundSpec,
)
