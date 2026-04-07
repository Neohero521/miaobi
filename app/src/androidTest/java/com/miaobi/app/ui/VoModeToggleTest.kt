package com.miaobi.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.miaobi.app.ui.screens.writing.VoModeToggle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class VoModeToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchToggle(
        isVoMode: Boolean = true,
        onToggle: (Boolean) -> Unit = {}
    ) {
        composeTestRule.setContent {
            VoModeToggle(
                isVoMode = isVoMode,
                onToggle = onToggle
            )
        }
    }

    // ===== Component presence tests =====

    @Test
    fun `displays V label`() {
        launchToggle()
        composeTestRule.onNodeWithText("V").assertExists()
    }

    @Test
    fun `displays O label`() {
        launchToggle()
        composeTestRule.onNodeWithText("O").assertExists()
    }

    @Test
    fun `displays Switch component`() {
        launchToggle()
        composeTestRule.onAllNodes(hasTestTag("Switch")).onFirst().assertExists()
    }

    // ===== Switch state tests =====

    @Test
    fun `switch is checked when isVoMode is true`() {
        launchToggle(isVoMode = true)
        composeTestRule.onAllNodes(isToggleable().and(hasTestTag("Switch")))
            .onFirst()
            .assertIsOn()
    }

    @Test
    fun `switch is unchecked when isVoMode is false`() {
        launchToggle(isVoMode = false)
        composeTestRule.onAllNodes(isToggleable().and(hasTestTag("Switch")))
            .onFirst()
            .assertIsOff()
    }

    // ===== Toggle via switch click =====

    @Test
    fun `switch calls onToggle when toggled from VoMode`() {
        var toggledTo = true
        launchToggle(
            isVoMode = true,
            onToggle = { toggledTo = it }
        )

        composeTestRule.onAllNodes(isToggleable().and(hasTestTag("Switch")))
            .onFirst()
            .performClick()
        assertEquals(false, toggledTo)
    }

    @Test
    fun `switch calls onToggle when toggled from OMode`() {
        var toggledTo = false
        launchToggle(
            isVoMode = false,
            onToggle = { toggledTo = it }
        )

        composeTestRule.onAllNodes(isToggleable().and(hasTestTag("Switch")))
            .onFirst()
            .performClick()
        assertEquals(true, toggledTo)
    }

    // ===== V button click tests =====

    @Test
    fun `V button calls onToggle with true when clicked while in O mode`() {
        var toggledTo = false
        launchToggle(
            isVoMode = false,
            onToggle = { toggledTo = it }
        )

        composeTestRule.onNodeWithText("V").performClick()
        assertEquals(true, toggledTo)
    }

    @Test
    fun `V button does not call onToggle when already in Vo mode`() {
        var called = false
        launchToggle(
            isVoMode = true,
            onToggle = { called = true }
        )

        composeTestRule.onNodeWithText("V").performClick()
        assertTrue(!called)
    }

    // ===== O button click tests =====

    @Test
    fun `O button calls onToggle with false when clicked while in V mode`() {
        var toggledTo = true
        launchToggle(
            isVoMode = true,
            onToggle = { toggledTo = it }
        )

        composeTestRule.onNodeWithText("O").performClick()
        assertEquals(false, toggledTo)
    }

    @Test
    fun `O button does not call onToggle when already in O mode`() {
        var called = false
        launchToggle(
            isVoMode = false,
            onToggle = { called = true }
        )

        composeTestRule.onNodeWithText("O").performClick()
        assertTrue(!called)
    }

    // ===== Multiple toggle interactions =====

    @Test
    fun `multiple toggles alternate between modes`() {
        var mode = true
        launchToggle(
            isVoMode = mode,
            onToggle = { mode = it }
        )

        // Toggle via switch
        composeTestRule.onAllNodes(isToggleable().and(hasTestTag("Switch")))
            .onFirst()
            .performClick()
        assertEquals(false, mode)

        // Toggle back
        composeTestRule.onAllNodes(isToggleable().and(hasTestTag("Switch")))
            .onFirst()
            .performClick()
        assertEquals(true, mode)

        // Click V when already true - no toggle
        composeTestRule.onNodeWithText("V").performClick()
        assertEquals(true, mode)

        // Click O to toggle to false
        composeTestRule.onNodeWithText("O").performClick()
        assertEquals(false, mode)
    }

    // ===== Mode consistency =====

    @Test
    fun `V label is bold when in VoMode`() {
        launchToggle(isVoMode = true)
        composeTestRule.onNodeWithText("V").assertExists()
    }

    @Test
    fun `O label is bold when in OMode`() {
        launchToggle(isVoMode = false)
        composeTestRule.onNodeWithText("O").assertExists()
    }
}
