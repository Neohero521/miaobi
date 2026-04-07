package com.miaobi.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 自动保存状态指示器
 * - 编辑中（"✒ 正在编辑..."）
 * - 保存中（"📝 保存中..."）
 * - 已保存（"✓ 已保存" 2秒后淡出）
 * - 保存失败（"⚠ 保存失败"）
 */
@Composable
fun SaveStatusIndicator(
    isSaving: Boolean,
    lastSaveTime: Long?,  // timestamp in ms, null = 从未保存
    hasUnsavedChanges: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    var showSavedBadge by remember { mutableStateOf(false) }
    var justSaved by remember { mutableStateOf(false) }

    // 当保存完成时，显示 "已保存" 徽章 2 秒
    LaunchedEffect(lastSaveTime) {
        if (lastSaveTime != null && hasUnsavedChanges.not()) {
            justSaved = true
            showSavedBadge = true
            delay(2000)
            showSavedBadge = false
            justSaved = false
        }
    }

    // 强制重绘以显示 "保存中"
    var savingState by remember { mutableStateOf(false) }
    LaunchedEffect(isSaving) {
        savingState = isSaving
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(300)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when {
                error != null -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                showSavedBadge -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                savingState -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                when {
                    error != null -> {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "保存失败",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                    showSavedBadge -> {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "已保存",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    savingState -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "保存中...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    hasUnsavedChanges -> {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "编辑中",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    else -> {
                        // 空状态，不显示
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }
            }
        }
    }
}
