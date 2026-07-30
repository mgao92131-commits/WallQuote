package com.example.wallquote.domain

import com.example.wallquote.domain.model.CollectionConfig

object CollectionValidation {
    fun validateForSave(config: CollectionConfig): String? {
        if (config.name.isBlank()) return "收藏集名称不能为空"
        if (config.lines.none { it.text.isNotBlank() }) return "至少需要一条非空白名言"
        return null
    }
}
