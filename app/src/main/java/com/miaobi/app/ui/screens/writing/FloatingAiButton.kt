package com.miaobi.app.ui.screens.writing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

/**
 * 右下角悬浮 AI 按钮
 * - 静止时：AutoAwesome 图标
 * - 生成中：Close 图标 + CircularProgressIndicator
 */
@Composable
fun FloatingAiButton(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isGenerating) 45f else 0f,
        label = "fab_rotation"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isGenerating)
            MaterialTheme.colorScheme.errorContainer
        else
            MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isGenerating)
            MaterialTheme.colorScheme.onErrorContainer
        else
            MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box {
            Icon(
                imageVector = if (isGenerating) Icons.Default.Close else Icons.Default.AutoAwesome,
                contentDescription = if (isGenerating) "停止生成" else "AI 续写",
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
            )
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
