package com.example.wallquote.data.local

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteTransform

fun CollectionWithLines.toDomain(): CollectionConfig {
    val background = parseBackground(collection.backgroundType, collection.backgroundData)
    val style = parseTextStyle(collection.textStyleData)
    val quoteLines = lines
        .sortedBy { it.displayOrder }
        .map { entity ->
            QuoteLine(
                id = entity.id,
                text = entity.text,
                displayOrder = entity.displayOrder,
            )
        }
    return CollectionConfig(
        id = collection.id,
        name = collection.name,
        schedule = com.example.wallquote.domain.model.DailyTimeRange(
            collection.startMinuteOfDay,
            collection.endMinuteOfDay,
        ),
        background = background,
        lines = quoteLines,
        textStyle = style,
        transform = QuoteTransform(
            centerXFraction = collection.offsetX,
            centerYFraction = collection.offsetY,
            rotationDegrees = collection.rotation,
        ),
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
        offsetX = transform.centerXFraction,
        offsetY = transform.centerYFraction,
        rotation = transform.rotationDegrees,
        sortOrder = sortOrder,
    )
}

fun CollectionConfig.toLineEntities(collectionId: Long): List<CollectionTextLineEntity> =
    lines
        .sortedBy { it.displayOrder }
        .mapIndexed { index, line ->
            CollectionTextLineEntity(
                id = line.id,
                collectionId = collectionId,
                text = line.text,
                displayOrder = index,
            )
        }
