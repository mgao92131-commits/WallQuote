package com.example.wallquote.ui.editor.time

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.wallquote.domain.time.HALF_HOUR_SLOTS
import com.example.wallquote.domain.time.halfHourIndexFromMinute
import com.example.wallquote.ui.editor.CircularTimePicker
import com.example.wallquote.ui.theme.WallQuoteColors
import com.example.wallquote.ui.util.formatMinuteOfDay

@Composable
fun TimePanel(
    startMinute: Int,
    endMinute: Int,
    onStartSlotChange: (Int) -> Unit,
    onEndSlotChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val startSlot = halfHourIndexFromMinute(startMinute)
    val endSlot = halfHourIndexFromMinute(endMinute)
    val isAllDay = startSlot == endSlot
    var showPrecise by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(WallQuoteColors.SurfaceRaised)
                .clickable { showPrecise = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = if (isAllDay) {
                    "All Day"
                } else {
                    "${formatMinuteOfDay(startMinute)} – ${formatMinuteOfDay(endMinute)}"
                },
                color = WallQuoteColors.Cream,
            )
        }
        Spacer(Modifier.height(8.dp))
        CircularTimePicker(
            startMinute = startMinute,
            endMinute = endMinute,
            onStartSlotChange = onStartSlotChange,
            onEndSlotChange = onEndSlotChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showPrecise) {
        AlertDialog(
            onDismissRequest = { showPrecise = false },
            title = { Text("精确时间") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        TimeStepper(
                            label = "开始",
                            minute = startMinute,
                            onDecrement = { onStartSlotChange(wrapSlot(startSlot - 1)) },
                            onIncrement = { onStartSlotChange(wrapSlot(startSlot + 1)) },
                        )
                        TimeStepper(
                            label = "结束",
                            minute = endMinute,
                            onDecrement = { onEndSlotChange(wrapSlot(endSlot - 1)) },
                            onIncrement = { onEndSlotChange(wrapSlot(endSlot + 1)) },
                        )
                    }
                    TextButton(onClick = { onStartSlotChange(0); onEndSlotChange(0) }) {
                        Text(if (isAllDay) "已设为全天" else "设为全天")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrecise = false }) { Text("完成") }
            },
        )
    }
}

@Composable
private fun TimeStepper(
    label: String,
    minute: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = WallQuoteColors.Ink)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) { Text("−", color = WallQuoteColors.Cream) }
            Text(formatMinuteOfDay(minute), color = WallQuoteColors.Cream)
            IconButton(onClick = onIncrement) { Text("+", color = WallQuoteColors.Cream) }
        }
    }
}

private fun wrapSlot(slot: Int): Int = ((slot % HALF_HOUR_SLOTS) + HALF_HOUR_SLOTS) % HALF_HOUR_SLOTS
