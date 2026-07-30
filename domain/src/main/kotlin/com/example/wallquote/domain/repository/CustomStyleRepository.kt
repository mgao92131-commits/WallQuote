package com.example.wallquote.domain.repository

import com.example.wallquote.domain.model.CustomTextStyle
import kotlinx.coroutines.flow.Flow

interface CustomStyleRepository {
    fun observeOrdered(): Flow<List<CustomTextStyle>>

    suspend fun getById(id: Long): CustomTextStyle?

    suspend fun save(style: CustomTextStyle): Long

    suspend fun delete(id: Long)

    suspend fun reorder(orderedIds: List<Long>)

    suspend fun existsName(name: String, excludingId: Long? = null): Boolean
}
