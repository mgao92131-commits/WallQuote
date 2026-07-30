package com.example.wallquote.data.local

import com.example.wallquote.domain.model.CollectionConfig

fun CollectionWithLines.toDomain(): CollectionConfig {
    val background = parseBackground(collection.backgroundType, collection.backgroundData)
    val style = parseTextStyle(collection.textStyleData)
    val texts = lines.sortedBy { it.displayOrder }.map { it.text }
    return CollectionConfig(
        id = collection.id,
        name = collection.name,
        schedule = com.example.wallquote.domain.model.DailyTimeRange(
            collection.startMinuteOfDay,
            collection.endMinuteOfDay,
        ),
        background = background,
        texts = texts,
        textStyle = style,
        offsetX = collection.offsetX,
        offsetY = collection.offsetY,
        rotation = collection.rotation,
        sortOrder = collection.sortOrder,
    )
}

fun CollectionConfig.toEntity(): CollectionEntity {
    val (type, data) = background.toStorage()
    return CollectionEntity(
        id = id,
        name = name,
        startMinuteOfDay = schedule.startMinuteOfDay,
        endMinuteOfDay = schedule.endMinuteOfDay,
        backgroundType = type,
        backgroundData = data,
        textStyleData = textStyle.toJson(),
        offsetX = offsetX,
        offsetY = offsetY,
        rotation = rotation,
        sortOrder = sortOrder,
    )
}

fun CollectionConfig.toLineEntities(collectionId: Long): List<CollectionTextLineEntity> =
    texts.mapIndexed { index, text ->
        CollectionTextLineEntity(
            collectionId = collectionId,
            text = text,
            displayOrder = index,
        )
    }
