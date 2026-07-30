package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.model.CollectionConfig

class SelectActiveCollectionsUseCase {
    operator fun invoke(
        collections: List<CollectionConfig>,
        minuteOfDay: Int,
    ): List<CollectionConfig> =
        collections.filter { it.schedule.contains(minuteOfDay) }
}
