package com.miaobi.app.ui.screens.writing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miaobi.app.domain.model.RewriteStyle

/**
 * 6 种写作风格 Tab 行（用于改写风格选择）
 * 风格：古风·现代·简洁·华丽·口语化·文艺
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
        Box {
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
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(text = style.emoji)
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
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
            // Scroll indicator gradient on right edge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(24.dp)
                    .fillMaxHeight()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * 6 种写作风格
 */
val WritingStyles = listOf(
    RewriteStyle.CLASSICAL,
    RewriteStyle.MODERN,
    RewriteStyle.CONCISE,
    RewriteStyle.FLOWERY,
    RewriteStyle.COLLOQUIAL,
    RewriteStyle.LITERARY
)
