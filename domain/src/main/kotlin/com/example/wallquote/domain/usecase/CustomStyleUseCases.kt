package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.repository.CustomStyleRepository
import com.example.wallquote.domain.style.TextStyleNormalizer
import com.example.wallquote.domain.style.TextStyleValidator

class ObserveCustomStylesUseCase(private val repository: CustomStyleRepository) {
    operator fun invoke() = repository.observeOrdered()
}

class GetCustomStyleUseCase(private val repository: CustomStyleRepository) {
    suspend operator fun invoke(id: Long) = repository.getById(id)
}

class SaveCustomStyleUseCase(private val repository: CustomStyleRepository) {
    suspend operator fun invoke(style: CustomTextStyle): Long {
        val name = style.name.trim()
        if (name.isEmpty()) throw IllegalArgumentException("样式名称不能为空")
        if (repository.existsName(name, excludingId = style.id.takeIf { it != 0L })) {
            throw IllegalArgumentException("样式名称已存在")
        }
        TextStyleValidator.validate(style.style)?.let { throw IllegalArgumentException(it) }
        return repository.save(
            style.copy(
                name = name,
                style = TextStyleNormalizer.normalize(style.style),
            ),
        )
    }
}

class DeleteCustomStyleUseCase(private val repository: CustomStyleRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}

class ReorderCustomStylesUseCase(private val repository: CustomStyleRepository) {
    suspend operator fun invoke(orderedIds: List<Long>) = repository.reorder(orderedIds)
}

class CreateStyleFromCollectionUseCase(private val repository: CustomStyleRepository) {
    suspend operator fun invoke(name: String, style: TextStyleConfig): Long =
        SaveCustomStyleUseCase(repository)(
            CustomTextStyle(name = name, style = style),
        )
}
