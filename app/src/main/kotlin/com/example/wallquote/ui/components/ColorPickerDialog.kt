package com.example.wallquote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.wallquote.core.preview.parseColorHex
import com.example.wallquote.ui.theme.WallQuoteColors

@Composable
fun ColorPickerDialog(
    initialHex: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = remember(initialHex) { hexToHsv(initialHex) }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var saturation by remember { mutableFloatStateOf(initial[1]) }
    var value by remember { mutableFloatStateOf(initial[2]) }
    var hexText by remember { mutableStateOf(normalizeHex(initialHex)) }
    val current = hsvToColor(hue, saturation, value)

    fun applyHsv(h: Float, s: Float, v: Float) {
        hue = h
        saturation = s
        value = v
        hexText = colorToHex(hsvToColor(h, s, v))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SaturationValueBox(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onChange = { s, v -> applyHsv(hue, s, v) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                )
                HueBar(
                    hue = hue,
                    onHueChange = { applyHsv(it, saturation, value) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(current)
                            .border(1.dp, WallQuoteColors.BeigeMuted, CircleShape),
                    )
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { raw ->
                            hexText = raw
                            val parsed = parseTypedHex(raw) ?: return@OutlinedTextField
                            val hsv = hexToHsv(parsed)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                            hexText = parsed
                        },
                        label = { Text("Hex") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(colorToHex(current)) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hueColor = hsvToColor(hue, 1f, 1f)
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .pointerInput(hue) {
                detectTapGestures { offset ->
                    onChange(
                        (offset.x / size.width).coerceIn(0f, 1f),
                        (1f - offset.y / size.height).coerceIn(0f, 1f),
                    )
                }
            }
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange(
                        (change.position.x / size.width).coerceIn(0f, 1f),
                        (1f - change.position.y / size.height).coerceIn(0f, 1f),
                    )
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val thumb = Offset(saturation * size.width, (1f - value) * size.height)
        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = thumb)
        drawCircle(color = Color.Black, radius = 8.dp.toPx(), center = thumb, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = hsvToColor(hue, saturation, value), radius = 5.dp.toPx(), center = thumb)
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hues = listOf(
        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
    )
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onHueChange((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onHueChange((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(hues))
        val x = (hue / 360f) * size.width
        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(x, size.height / 2f))
        drawCircle(
            color = Color.Black,
            radius = 7.dp.toPx(),
            center = Offset(x, size.height / 2f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val hsv = floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun hexToHsv(hex: String): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(parseColorHex(hex).toArgb(), hsv)
    return hsv
}

private fun colorToHex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

private fun normalizeHex(hex: String): String {
    val body = hex.trim().removePrefix("#")
    return if (body.length == 6) "#${body.uppercase()}" else hex
}

private fun parseTypedHex(raw: String): String? {
    val body = raw.trim().removePrefix("#")
    if (body.length != 6) return null
    return body.toIntOrNull(16)?.let { "#${body.uppercase()}" }
}
