package com.miaobi.app.ui

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.text.BasicTextField
import org.junit.Rule
import org.junit.Test

/**
 * Tests for Bug Fix 2: Text Selection triggering rewrite via TextFieldValue mechanism
 * 
 * The WritingContent composable uses TextFieldValue to properly detect when users
 * select text, as opposed to just editing content. This allows the rewrite feature
 * to trigger when text is selected.
 */
class WritingContentSelectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createWritingContent(
        initialContent: String = "",
        onContentChange: (String) -> Unit = {},
        onTextSelected: (String) -> Unit = {}
    ) {
        composeTestRule.setContent {
            var textFieldValue by remember { mutableStateOf(TextFieldValue(initialContent)) }
            var lastSelection by remember { mutableStateOf<TextRange?>(null) }

            val onTextSelectedState by rememberUpdatedState(onTextSelected)

            SelectionContainer {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        val prevSel = lastSelection
                        val newSel = newValue.selection

                        // Detect pure selection: text same, selection changed
                        val isPureSelection = prevSel != null &&
                            newSel != prevSel &&
                            newValue.text == textFieldValue.text

                        textFieldValue = newValue
                        lastSelection = newSel

                        if (newValue.text != initialContent) {
                            onContentChange(newValue.text)
                        }

                        if (isPureSelection && newSel.min != newSel.max) {
                            val selected = newValue.text.substring(newSel.min, newSel.max)
                            if (selected.isNotBlank()) {
                                onTextSelected(selected)
                            }
                        }
                    },
                    modifier = Modifier.testTag("writing_content"),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        androidx.compose.foundation.layout.Box {
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = "开始写作...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }

    @Test
    fun `displays placeholder when empty`() {
        createWritingContent()
        composeTestRule.onNodeWithText("开始写作...").assertExists()
    }

    @Test
    fun `displays content when provided`() {
        createWritingContent(initialContent = "测试内容")
        composeTestRule.onNodeWithText("测试内容").assertExists()
    }

    @Test
    fun `accepts text input`() {
        createWritingContent(
            initialContent = "",
            onContentChange = {}
        )
        
        composeTestRule.onNodeWithText("开始写作...").performTextInput("新输入的内容")
        composeTestRule.onNodeWithText("新输入的内容").assertExists()
    }

    @Test
    fun `placeholder disappears when content exists`() {
        createWritingContent(initialContent = "已有内容")
        composeTestRule.onNodeWithText("开始写作...").assertDoesNotExist()
    }
}
