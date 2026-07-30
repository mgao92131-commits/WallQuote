package com.example.wallquote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.usecase.DeleteCollectionUseCase
import com.example.wallquote.domain.usecase.ObserveOrderedCollectionsUseCase
import com.example.wallquote.wallpaper.AndroidClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeOrderedCollectionsUseCase: ObserveOrderedCollectionsUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
) : ViewModel() {

    private val clock = AndroidClock()

    val collections: StateFlow<List<CollectionConfig>> =
        observeOrderedCollectionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Ticks periodically so Home cards can reflect "active by schedule" and the timeline "now" marker. */
    val nowMinuteOfDay: StateFlow<Int> = flow {
        while (true) {
            emit(clock.minuteOfDay())
            delay(NOW_TICK_MILLIS)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), clock.minuteOfDay())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            runCatching { deleteCollectionUseCase(id) }
                .onFailure { _errorMessage.value = "删除失败，请重试" }
        }
    }

    private companion object {
        const val NOW_TICK_MILLIS = 30_000L
    }
}
