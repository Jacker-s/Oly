package com.jack.friend.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WappiLikeIcon(
    filled: Boolean,
    tint: Color,
    size: Dp = 24.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        
        val path = Path().apply {
            moveTo(width * 0.5f, height * 0.25f)
            cubicTo(width * 0.45f, height * 0.1f, width * 0.1f, height * 0.1f, width * 0.1f, height * 0.45f)
            cubicTo(width * 0.1f, height * 0.7f, width * 0.5f, height * 0.9f, width * 0.5f, height * 0.9f)
            cubicTo(width * 0.5f, height * 0.9f, width * 0.9f, height * 0.7f, width * 0.9f, height * 0.45f)
            cubicTo(width * 0.9f, height * 0.1f, width * 0.55f, height * 0.1f, width * 0.5f, height * 0.25f)
            close()
        }

        if (filled) {
            drawPath(path = path, color = tint)
        } else {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun WappiCommentIcon(
    tint: Color,
    size: Dp = 24.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        
        val path = Path().apply {
            // Main bubble body
            moveTo(width * 0.15f, height * 0.15f)
            lineTo(width * 0.85f, height * 0.15f)
            lineTo(width * 0.85f, height * 0.65f)
            lineTo(width * 0.5f, height * 0.65f)
            lineTo(width * 0.25f, height * 0.85f)
            lineTo(width * 0.25f, height * 0.65f)
            lineTo(width * 0.15f, height * 0.65f)
            close()
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Small detail: a dot or line inside
        drawCircle(
            color = tint,
            radius = 1.5.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun WappiShareIcon(
    tint: Color,
    size: Dp = 24.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        
        val path = Path().apply {
            // The "send" or "share" arrow style
            moveTo(width * 0.15f, height * 0.5f)
            lineTo(width * 0.85f, height * 0.15f)
            lineTo(width * 0.55f, height * 0.85f)
            lineTo(width * 0.45f, height * 0.55f)
            close()
            
            // Connection line
            moveTo(width * 0.45f, height * 0.55f)
            lineTo(width * 0.15f, height * 0.85f)
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
