package com.example.wallquote.domain

import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.style.TextStyleNormalizer

object CollectionValidation {
    fun validateForSave(config: CollectionConfig): String? {
        if (config.name.isBlank()) return "收藏集名称不能为空"
        if (config.lines.none { it.text.isNotBlank() }) return "至少需要一条非空白名言"
        BackgroundValidation.validate(config.background)?.let { return it }
        return TextStyleNormalizer.validate(config.textStyle)
    }
}
