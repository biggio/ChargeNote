package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalExtendedColors

@Composable
fun BatterySocBar(
    socPercent: Int,
    modifier: Modifier = Modifier,
    label: String = "充飽電量 (SoC)"
) {
    val extendedColors = LocalExtendedColors.current
    val progress = (socPercent / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "socProgress")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(extendedColors.subCardBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔋 $label",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = extendedColors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$socPercent%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = extendedColors.accentPrimary
            )
        }

        // Background bar track
        Box(
            modifier = Modifier
                .padding(top = 28.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(extendedColors.cardBorder)
        ) {
            // Filled progress
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(extendedColors.accentPrimary)
            )
        }
    }
}
