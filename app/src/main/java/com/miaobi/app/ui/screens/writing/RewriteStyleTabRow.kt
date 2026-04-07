package com.miaobi.app.ui.screens.writing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miaobi.app.domain.model.RewriteStyle

/**
 * 5 种写作风格 Tab 行（用于改写风格选择）
 * 与架构文档对齐：古风·现代·简洁·华丽·口语化
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewriteStyleTabRow(
    selectedStyle: RewriteStyle,
    onStyleSelected: (RewriteStyle) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WritingStyles.forEach { style ->
                    val isSelected = style == selectedStyle
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (enabled) onStyleSelected(style) },
                        label = {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        enabled = enabled,
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

/**
 * 与架构文档对齐的 5 种风格
 */
val WritingStyles = listOf(
    RewriteStyle.CLASSICAL,
    RewriteStyle.MODERN,
    RewriteStyle.CONCISE,
    RewriteStyle.FLOWERY,
    RewriteStyle.COLLOQUIAL
)
