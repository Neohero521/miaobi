package com.miaobi.app.ui.screens.writing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/**
 * 右下角悬浮 AI 按钮
 * - 静止时：AutoAwesome 羽毛笔图标 (typewriter aesthetic)
 * - 生成中：Close 图标 + 旋转动画 + 进度指示
 */
@Composable
fun FloatingAiButton(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isGenerating) 45f else 0f,
        animationSpec = tween(300),
        label = "fab_rotation"
    )

    // Pulsing scale when generating
    val scale by animateFloatAsState(
        targetValue = if (isGenerating) 1.05f else 1f,
        animationSpec = tween(600),
        label = "fab_scale"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .shadow(if (isGenerating) 12.dp else 6.dp, CircleShape),
        shape = CircleShape,
        containerColor = if (isGenerating)
            MaterialTheme.colorScheme.errorContainer
        else
            MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isGenerating)
            MaterialTheme.colorScheme.onErrorContainer
        else
            MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Spinning icon when generating
            Icon(
                imageVector = if (isGenerating) Icons.Default.Close else Icons.Default.AutoAwesome,
                contentDescription = if (isGenerating) "停止生成" else "AI 续写",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )

            // Tiny progress dot
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
