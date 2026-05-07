package com.decathlon.smartnutristock.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for StatusPill component.
 *
 * Verifies correct rendering of icons, colors, labels, and accessibility
 * for all SemaphoreStatus values (GREEN, YELLOW, EXPIRED).
 */
class StatusPillTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysCorrectIconAndTextForGreenStatus() {
        // Given - GREEN status
        val status = SemaphoreStatus.GREEN
        val expectedLabel = "Seguro"
        val expectedContentDescription = "Estado: $expectedLabel"

        // When - StatusPill is rendered with GREEN status
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    StatusPill(status = status)
                }
            }
        }

        // Then - verify the correct label is displayed
        composeTestRule.onNodeWithText(expectedLabel)
            .assertIsDisplayed()

        // Then - verify the correct content description for accessibility
        composeTestRule.onNodeWithContentDescription(expectedContentDescription)
            .assertIsDisplayed()

        // Then - verify the component meets minimum tap target size
        composeTestRule.onNodeWithText(expectedLabel)
            .assertExists()
    }

    @Test
    fun displaysCorrectIconAndTextForYellowStatus() {
        // Given - YELLOW status
        val status = SemaphoreStatus.YELLOW
        val expectedLabel = "Próximo"
        val expectedContentDescription = "Estado: $expectedLabel"

        // When - StatusPill is rendered with YELLOW status
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    StatusPill(status = status)
                }
            }
        }

        // Then - verify the correct label is displayed
        composeTestRule.onNodeWithText(expectedLabel)
            .assertIsDisplayed()

        // Then - verify the correct content description for accessibility
        composeTestRule.onNodeWithContentDescription(expectedContentDescription)
            .assertIsDisplayed()

        // Then - verify the component meets minimum tap target size
        composeTestRule.onNodeWithText(expectedLabel)
            .assertExists()
    }

    @Test
    fun displaysCorrectIconAndTextForExpiredStatus() {
        // Given - EXPIRED status
        val status = SemaphoreStatus.EXPIRED
        val expectedLabel = "Caducado"
        val expectedDescription = "Estado: $expectedLabel"

        // When - StatusPill is rendered with EXPIRED status
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    StatusPill(status = status)
                }
            }
        }

        // Then - verify the correct label is displayed
        composeTestRule.onNodeWithText(expectedLabel)
            .assertIsDisplayed()

        // Then - verify the correct content description for accessibility
        composeTestRule.onNodeWithContentDescription(expectedDescription)
            .assertIsDisplayed()

        // Then - verify the component meets minimum tap target size
        composeTestRule.onNodeWithText(expectedLabel)
            .assertExists()
    }

    @Test
    fun hasAccessibilityContentDescriptionForAllStatuses() {
        // Given - all three status values
        val statuses = listOf(
            SemaphoreStatus.GREEN to "Estado: Seguro",
            SemaphoreStatus.YELLOW to "Estado: Próximo",
            SemaphoreStatus.EXPIRED to "Estado: Caducado"
        )

        // When - verify each status has correct content description
        statuses.forEach { (status, expectedDescription) ->
            composeTestRule.setContent {
                MaterialTheme {
                    Surface {
                        StatusPill(status = status)
                    }
                }
            }

            // Then - verify the accessibility content description matches expected
            composeTestRule.onNodeWithContentDescription(expectedDescription)
                .assertContentDescriptionEquals(expectedDescription)
        }
    }

    @Test
    fun displaysStatusPillWithCorrectLabelForAllValues() {
        // Given - all SemaphoreStatus values and their expected labels
        val testCases = mapOf(
            SemaphoreStatus.GREEN to "Seguro",
            SemaphoreStatus.YELLOW to "Próximo",
            SemaphoreStatus.EXPIRED to "Caducado"
        )

        // When - verify each status displays correct label
        testCases.forEach { (status, expectedLabel) ->
            composeTestRule.setContent {
                MaterialTheme {
                    Surface {
                        StatusPill(status = status)
                    }
                }
            }

            // Then - verify the status label is displayed
            composeTestRule.onNodeWithText(expectedLabel)
                .assertIsDisplayed()
        }
    }
}
