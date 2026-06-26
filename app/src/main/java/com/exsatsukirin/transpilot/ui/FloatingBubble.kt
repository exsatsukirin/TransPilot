package com.exsatsukirin.transpilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingBubbleContent(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "译",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
