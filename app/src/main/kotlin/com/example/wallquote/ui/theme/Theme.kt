package com.example.wallquote.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val ColorErrorContainer = Color(0xFF4A2A2A)

private val WallQuoteDarkScheme = darkColorScheme(
    primary = WallQuoteColors.Beige,
    onPrimary = WallQuoteColors.Canvas,
    primaryContainer = WallQuoteColors.SurfaceRaised,
    onPrimaryContainer = WallQuoteColors.Cream,
    secondary = WallQuoteColors.Ink,
    onSecondary = WallQuoteColors.Cream,
    background = WallQuoteColors.Canvas,
    onBackground = WallQuoteColors.Cream,
    surface = WallQuoteColors.Surface,
    onSurface = WallQuoteColors.Cream,
    surfaceVariant = WallQuoteColors.SurfaceRaised,
    onSurfaceVariant = WallQuoteColors.Ink,
    outline = WallQuoteColors.BeigeMuted,
    error = WallQuoteColors.HandleEnd,
    onError = WallQuoteColors.Cream,
    errorContainer = ColorErrorContainer,
    onErrorContainer = WallQuoteColors.Cream,
)

@Composable
fun WallQuoteTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = WallQuoteDarkScheme,
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = WallQuoteColors.Canvas,
                content = content,
            )
        },
    )
}
