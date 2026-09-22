package com.compressflow.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Official CompressFlow logo vector component translated from StitchMCP 31-app-icon.html.
 * Renders the vibrant primary blue container, white chevron, light-blue chevron, and emerald accent dot.
 */
@Composable
fun CompressFlowLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    showContainer: Boolean = true
) {
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val scale = w / 48f

            if (showContainer) {
                drawRoundRect(
                    color = Color(0xFF2563EB),
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(12f * scale, 12f * scale)
                )
            }

            // White Chevron: M14 16L24 24L14 32
            val path1 = Path().apply {
                moveTo(14f * scale, 16f * scale)
                lineTo(24f * scale, 24f * scale)
                lineTo(14f * scale, 32f * scale)
            }
            drawPath(
                path = path1,
                color = Color.White,
                style = Stroke(
                    width = 3.5f * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Light Blue Chevron: M22 16L32 24L22 32
            val path2 = Path().apply {
                moveTo(22f * scale, 16f * scale)
                lineTo(32f * scale, 24f * scale)
                lineTo(22f * scale, 32f * scale)
            }
            drawPath(
                path = path2,
                color = Color(0xFF93C5FD),
                style = Stroke(
                    width = 3.5f * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Emerald accent dot: cx=34, cy=24, r=3
            drawCircle(
                color = Color(0xFF22C55E),
                radius = 3f * scale,
                center = Offset(34f * scale, 24f * scale)
            )
        }
    }
}
