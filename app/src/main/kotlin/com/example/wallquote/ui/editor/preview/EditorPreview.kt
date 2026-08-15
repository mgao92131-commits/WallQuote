package com.example.wallquote.ui.editor.preview

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.domain.layout.QuoteGestureTransformer
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.ui.editor.EditorUiState

/**
 * Fullscreen quote preview. Pan (one finger) and pan+rotate (two fingers) are always enabled;
 * raw transforms are emitted via [onTransformChange] and clamped by
 * [com.example.wallquote.ui.editor.EditorViewModel.updateTransformRequested].
 */
@Composable
fun EditorPreview(
    state: EditorUiState,
    onTransformChange: (QuoteTransform) -> Unit,
    onViewportSizeChanged: (widthPx: Int, heightPx: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestState = rememberUpdatedState(state)
    Box(
        modifier = modifier.onSizeChanged { onViewportSizeChanged(it.width, it.height) },
    ) {
        QuotePreview(
            state = state.toPreviewInput(),
            modifier = Modifier.fillMaxSize(),
            processedPhotoBitmap = state.processedPreviewBitmap,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, _, rotation ->
                        val widthPx = size.width
                        val heightPx = size.height
                        if (widthPx <= 0 || heightPx <= 0) return@detectTransformGestures
                        val current = latestState.value
                        val rawTransform = QuoteGestureTransformer.apply(
                            current = current.transform,
                            panX = pan.x,
                            panY = pan.y,
                            viewportWidth = widthPx,
                            viewportHeight = heightPx,
                            rotationDeltaDegrees = rotation,
                        )
                        onTransformChange(rawTransform)
                    }
                },
        )
    }
}
