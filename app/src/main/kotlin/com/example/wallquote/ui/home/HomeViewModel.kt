package com.example.wallquote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.usecase.DeleteCollectionUseCase
import com.example.wallquote.domain.usecase.ObserveOrderedCollectionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeOrderedCollectionsUseCase: ObserveOrderedCollectionsUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
) : ViewModel() {

    val collections: StateFlow<List<CollectionConfig>> =
        observeOrderedCollectionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            deleteCollectionUseCase(id)
        }
    }
}
