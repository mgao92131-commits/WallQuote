package com.example.wallquote.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoxScope.EditorTopOverlay(
    onClose: () -> Unit,
) {
    val hintShadow = Shadow(
        color = Color.Black.copy(alpha = 0.65f),
        blurRadius = 8f,
    )
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(start = 20.dp, top = 12.dp),
    ) {
        Text(
            text = "Preview",
            style = TextStyle(
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                shadow = hintShadow,
            ),
        )
        Text(
            text = "Drag to move / rotate",
            style = TextStyle(
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
                shadow = hintShadow,
            ),
        )
    }
    IconButton(
        onClick = onClose,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f)),
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "关闭",
            tint = Color.White,
        )
    }
}
