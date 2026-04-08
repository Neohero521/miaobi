package com.miaobi.app.ui.screens.writing

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 动态字数统计组件
 */
@Composable
fun AnimatedWordCount(
    wordCount: Int,
    modifier: Modifier = Modifier
) {
    var previousWordCount by remember { mutableStateOf(wordCount) }
    val animatedWordCount by animateIntAsState(
        targetValue = wordCount,
        animationSpec = tween(300),
        label = "word_count"
    )
    
    LaunchedEffect(wordCount) {
        previousWordCount = wordCount
    }
    
    // Determine writing status based on word count
    val status = when {
        wordCount < 100 -> "开始创作"
        wordCount < 500 -> "初具雏形"
        wordCount < 1000 -> "渐入佳境"
        wordCount < 3000 -> "文思泉涌"
        wordCount < 5000 -> "佳作可期"
        else -> "大作将成"
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 字数
        Text(
            text = "${animatedWordCount}字",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        
        // 分隔点
        Box(
            modifier = Modifier
                .size(4.dp)
                .padding(vertical = 2.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ) {}
        }
        
        // 状态
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * 简洁的字符数显示
 */
@Composable
fun SimpleWordCount(
    wordCount: Int,
    modifier: Modifier = Modifier
) {
    val animatedWordCount by animateIntAsState(
        targetValue = wordCount,
        animationSpec = tween(300),
        label = "word_count"
    )
    
    Text(
        text = "${animatedWordCount}字",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = modifier
    )
}
