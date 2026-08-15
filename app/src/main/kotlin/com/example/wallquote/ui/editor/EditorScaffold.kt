package com.example.wallquote.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

private const val PanelAnimMs = 260

@Composable
fun EditorScaffold(
    selectedPanel: EditorPanel?,
    onSelectPanel: (EditorPanel) -> Unit,
    onDismissPanel: () -> Unit,
    onClose: () -> Unit,
    preview: @Composable BoxScope.() -> Unit,
    panelContent: @Composable (EditorPanel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val targetFraction = selectedPanel?.bottomPanelHeightFraction ?: 0.58f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(PanelAnimMs),
        label = "editorPanelHeight",
    )
    var visiblePanel by remember { mutableStateOf<EditorPanel?>(null) }
    LaunchedEffect(selectedPanel) {
        if (selectedPanel != null) visiblePanel = selectedPanel
    }

    Box(modifier = modifier.fillMaxSize()) {
        preview()

        if (selectedPanel != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismissPanel,
                    ),
            )
        }

        EditorTopOverlay(onClose = onClose)

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            AnimatedVisibility(
                visible = selectedPanel != null,
                enter = slideInVertically(tween(PanelAnimMs)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(PanelAnimMs)) { it } + fadeOut(tween(180)),
            ) {
                val panel = selectedPanel ?: visiblePanel
                if (panel != null) {
                    EditorBottomPanel(
                        height = (screenHeightDp * animatedFraction).dp,
                        onDismiss = onDismissPanel,
                    ) {
                        panelContent(panel)
                    }
                }
            }
            EditorBottomDock(
                selectedPanel = selectedPanel,
                onSelect = onSelectPanel,
            )
        }
    }
}
