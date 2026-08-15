package com.example.wallquote.ui.editor.background

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.wallquote.core.preview.parseColorHex
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.ui.editor.EditorUiState
import com.example.wallquote.ui.theme.WallQuoteColors

@Composable
fun BackgroundPanel(
    state: EditorUiState,
    onApplyPreset: (BackgroundPreset) -> Unit,
    onPickPhoto: () -> Unit,
    onPickCancelled: () -> Unit,
    onPhotoPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) onPickCancelled() else onPhotoPicked(uri.toString())
    }

    val matched = BackgroundPresets.matching(state.backgroundSpec)
    val usingPhoto = state.backgroundSpec is BackgroundSpec.Photo
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BackgroundPresets.all.forEach { preset ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(presetBrush(preset.spec))
                            .border(
                                width = if (matched?.id == preset.id) 2.5.dp else 1.dp,
                                color = if (matched?.id == preset.id) {
                                    WallQuoteColors.Cream
                                } else {
                                    WallQuoteColors.BeigeMuted
                                },
                                shape = CircleShape,
                            )
                            .clickable { onApplyPreset(preset) },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = preset.label,
                        color = WallQuoteColors.Ink,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(WallQuoteColors.SurfaceRaised)
                .border(
                    width = if (usingPhoto) 1.5.dp else 1.dp,
                    color = if (usingPhoto) WallQuoteColors.Beige else WallQuoteColors.BeigeMuted,
                    shape = RoundedCornerShape(18.dp),
                )
                .clickable {
                    onPickPhoto()
                    photoPicker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    tint = WallQuoteColors.CreamMuted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (usingPhoto) "替换图片" else "选择图片",
                    color = WallQuoteColors.Cream,
                )
            }
        }
    }
}

private fun presetBrush(spec: BackgroundSpec): Brush =
    when (spec) {
        is BackgroundSpec.Solid -> Brush.linearGradient(
            listOf(parseColorHex(spec.colorHex), parseColorHex(spec.colorHex)),
        )
        is BackgroundSpec.Gradient -> Brush.linearGradient(
            listOf(parseColorHex(spec.startColorHex), parseColorHex(spec.endColorHex)),
        )
        is BackgroundSpec.Photo -> Brush.linearGradient(
            listOf(WallQuoteColors.SurfaceRaised, WallQuoteColors.Canvas),
        )
    }
