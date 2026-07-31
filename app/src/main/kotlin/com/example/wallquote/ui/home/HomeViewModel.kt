package com.example.wallquote.ui.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.usecase.DeleteCollectionUseCase
import com.example.wallquote.domain.usecase.ObserveOrderedCollectionsUseCase
import com.example.wallquote.wallpaper.AndroidClock
import com.example.wallquote.wallpaper.background.BackgroundImageLoader
import com.example.wallquote.wallpaper.background.BackgroundImageResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What a Home list card shows for one collection, including its (possibly still-loading) photo thumbnail. */
data class CollectionCardUiModel(
    val config: CollectionConfig,
    val thumbnailBitmap: Bitmap? = null,
    /** True once a load attempt for a Photo background has completed without producing a bitmap. */
    val thumbnailLoadFailed: Boolean = false,
)

private data class ThumbnailState(
    val bitmap: Bitmap?,
    val failed: Boolean,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeOrderedCollectionsUseCase: ObserveOrderedCollectionsUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
    private val backgroundImageLoader: BackgroundImageLoader,
) : ViewModel() {

    private val clock = AndroidClock()

    private val collections: StateFlow<List<CollectionConfig>> =
        observeOrderedCollectionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- P4-011: Photo background thumbnails, loaded via the shared BackgroundImageLoader (same
    // decode+blur pipeline as the editor/wallpaper) at a fixed small size, with per-collection
    // cancellation so stale/removed cards never clobber a newer card's bitmap. ---
    private val _thumbnails = MutableStateFlow<Map<Long, ThumbnailState>>(emptyMap())
    private val thumbnailJobs = mutableMapOf<Long, Job>()
    private val thumbnailKeys = mutableMapOf<Long, String>()

    val cardModels: StateFlow<List<CollectionCardUiModel>> =
        combine(collections, _thumbnails) { list, thumbs ->
            list.map { config ->
                val thumb = thumbs[config.id]
                CollectionCardUiModel(
                    config = config,
                    thumbnailBitmap = thumb?.bitmap,
                    thumbnailLoadFailed = thumb?.failed ?: false,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Ticks periodically so Home cards can reflect "active by schedule" and the timeline "now" marker. */
    val nowMinuteOfDay: StateFlow<Int> = flow {
        while (true) {
            emit(clock.minuteOfDay())
            delay(NOW_TICK_MILLIS)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), clock.minuteOfDay())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            collections.collect { syncThumbnails(it) }
        }
    }

    private fun syncThumbnails(list: List<CollectionConfig>) {
        val activeIds = list.map { it.id }.toSet()
        val staleIds = thumbnailJobs.keys.filterNot { it in activeIds }
        if (staleIds.isNotEmpty()) {
            staleIds.forEach { id ->
                thumbnailJobs.remove(id)?.cancel()
                thumbnailKeys.remove(id)
            }
            _thumbnails.update { current -> current - staleIds.toSet() }
        }

        list.forEach { config ->
            val photo = config.background as? BackgroundSpec.Photo
            if (photo == null) {
                val hadEntry = thumbnailJobs.remove(config.id) != null || thumbnailKeys.remove(config.id) != null
                if (hadEntry) {
                    _thumbnails.update { it - config.id }
                }
                return@forEach
            }
            val key = "${photo.assetId}:${photo.blurRadiusDp}"
            if (thumbnailKeys[config.id] == key) return@forEach // unchanged: keep the cached bitmap
            thumbnailKeys[config.id] = key
            thumbnailJobs.remove(config.id)?.cancel()
            _thumbnails.update { it - config.id } // clear stale bitmap while the new one loads
            thumbnailJobs[config.id] = viewModelScope.launch {
                val result = runCatching {
                    backgroundImageLoader.load(
                        assetId = photo.assetId,
                        targetWidth = THUMBNAIL_WIDTH_PX,
                        targetHeight = THUMBNAIL_HEIGHT_PX,
                        blurRadiusDp = photo.blurRadiusDp,
                        density = 1f,
                    )
                }.getOrNull()
                ensureActive() // dropped if cancelled (superseded/removed) before the load finished
                val bitmap = (result as? BackgroundImageResult.Success)?.bitmap
                _thumbnails.update {
                    it + (config.id to ThumbnailState(bitmap = bitmap, failed = bitmap == null))
                }
            }
        }
    }

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
        const val THUMBNAIL_WIDTH_PX = 360
        const val THUMBNAIL_HEIGHT_PX = 200
    }
}
