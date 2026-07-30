package com.example.wallquote.ui.style

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.usecase.DeleteCustomStyleUseCase
import com.example.wallquote.domain.usecase.ObserveCustomStylesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomStylesListViewModel @Inject constructor(
    observeCustomStylesUseCase: ObserveCustomStylesUseCase,
    private val deleteCustomStyleUseCase: DeleteCustomStyleUseCase,
) : ViewModel() {

    val styles: StateFlow<List<CustomTextStyle>> = observeCustomStylesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            runCatching { deleteCustomStyleUseCase(id) }
                .onFailure { _errorMessage.value = "删除失败，请重试" }
        }
    }
}
