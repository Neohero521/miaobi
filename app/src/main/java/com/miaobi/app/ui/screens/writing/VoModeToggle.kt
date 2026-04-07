package com.miaobi.app.ui.screens.writing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * V/O 模式切换开关
 * - V (Verbose)：AI 输出更详细、描写丰富
 * - O (Original)：AI 输出简洁，贴近用户已有文风
 */
@Composable
fun VoModeToggle(
    isVoMode: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isVoMode)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // V mode button
            IconButton(
                onClick = { if (!isVoMode) onToggle(true) },
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "V",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isVoMode) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    color = if (isVoMode)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Toggle
            Switch(
                checked = isVoMode,
                onCheckedChange = onToggle,
                modifier = Modifier.height(24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            // O mode button
            IconButton(
                onClick = { if (isVoMode) onToggle(false) },
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "O",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (!isVoMode) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                    color = if (!isVoMode)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
