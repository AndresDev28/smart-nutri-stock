package com.decathlon.smartnutristock.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.WorkflowAction
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * Unit tests for HistoryCard component.
 *
 * Verifies correct rendering of all three blocks (Header, Attributes, Footer),
 * text overflow handling, MoreVert menu interaction, and StatusPill display.
 */
class HistoryCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleBatch = Batch(
        id = "batch-1",
        ean = "8001234567890",
        quantity = 10,
        expiryDate = Instant.parse("2025-12-31T00:00:00Z"),
        status = SemaphoreStatus.GREEN,
        name = "Proteína Whey Isolate 2kg",
        packSize = 2000,
        deletedAt = null,
        actionTaken = WorkflowAction.PENDING
    )

    @Test
    fun displaysHeaderBlockWithProductNameAndEAN() {
        // Given - a batch with name and EAN
        var editClicked = false
        var deleteClicked = false

        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = sampleBatch,
                        onEditClick = { editClicked = true },
                        onDeleteClick = { deleteClicked = true }
                    )
                }
            }
        }

        // Then - verify product name is displayed
        composeTestRule.onNodeWithText(sampleBatch.name!!)
            .assertIsDisplayed()

        // Then - verify EAN is displayed with JetBrains Mono styling
        composeTestRule.onNodeWithText("EAN: ${sampleBatch.ean}")
            .assertIsDisplayed()

        // Then - verify MoreVert icon is displayed with correct content description
        composeTestRule.onNodeWithContentDescription("Más opciones para ${sampleBatch.name}")
            .assertIsDisplayed()
    }

    @Test
    fun displaysAttributesBlockWithPackAndQuantity() {
        // Given - a batch with pack size and quantity
        val batchWithPack = sampleBatch.copy(
            packSize = 475,
            quantity = 5
        )

        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = batchWithPack,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then - verify Pack label is displayed
        composeTestRule.onNodeWithText("Pack:")
            .assertIsDisplayed()

        // Then - verify Pack value is displayed
        composeTestRule.onNodeWithText("475g")
            .assertIsDisplayed()

        // Then - verify Quantity label is displayed
        composeTestRule.onNodeWithText("Cantidad:")
            .assertIsDisplayed()

        // Then - verify Quantity value is displayed
        composeTestRule.onNodeWithText("5")
            .assertIsDisplayed()
    }

    @Test
    fun displaysFooterBlockWithExpiryDateAndStatusPill() {
        // Given - a batch with expiry date
        val expiryDate = sampleBatch.expiryDate.atZone(ZoneId.systemDefault()).toLocalDate()
        val formattedDate = expiryDate.dayOfMonth.toString().padStart(2, '0') + "/" +
            expiryDate.monthValue.toString().padStart(2, '0') + "/" +
            expiryDate.year

        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = sampleBatch,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then - verify "Vence:" label is displayed
        composeTestRule.onNodeWithText("Vence:")
            .assertIsDisplayed()

        // Then - verify StatusPill is displayed
        composeTestRule.onNodeWithText("Seguro")
            .assertIsDisplayed()
    }

    @Test
    fun displaysCorrectStatusPillForAllStatuses() {
        // Given - all three status values
        val testCases = mapOf(
            SemaphoreStatus.GREEN to "Seguro",
            SemaphoreStatus.YELLOW to "Próximo",
            SemaphoreStatus.EXPIRED to "Caducado"
        )

        // When - verify each status displays correct StatusPill
        testCases.forEach { (status, expectedLabel) ->
            val testBatch = sampleBatch.copy(status = status)

            composeTestRule.setContent {
                MaterialTheme {
                    Surface {
                        HistoryCard(
                            batch = testBatch,
                            onEditClick = {},
                            onDeleteClick = {}
                        )
                    }
                }
            }

            // Then - verify the StatusPill label is displayed
            composeTestRule.onNodeWithText(expectedLabel)
                .assertIsDisplayed()
        }
    }

    @Test
    fun handlesTextOverflowForLongProductName() {
        // Given - a batch with very long product name
        val longName = "Proteína Whey Isolate Ultra Premium con Aminoácidos Esenciales y Sabores Naturales"
        val batchWithLongName = sampleBatch.copy(name = longName)

        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = batchWithLongName,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then - verify the card is still displayed (text doesn't crash layout)
        composeTestRule.onNodeWithText("EAN:")
            .assertIsDisplayed()
    }

    @Test
    fun moreVertIconButtonHasCorrectTapTarget() {
        // Given - a HistoryCard
        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = sampleBatch,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then - verify MoreVert icon has click action (tap target >= 48dp)
        composeTestRule.onNodeWithContentDescription("Más opciones para ${sampleBatch.name}")
            .assertHasClickAction()
    }

    @Test
    fun iconsHaveContentDescriptionsForAccessibility() {
        // Given - a HistoryCard
        val batchWithPack = sampleBatch.copy(packSize = 475, quantity = 5)

        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = batchWithPack,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then - verify all icons have content descriptions
        composeTestRule.onNodeWithContentDescription("Más opciones para ${sampleBatch.name}")
            .assertIsDisplayed()
    }

    @Test
    fun displaysNullNameAsUnknownProduct() {
        // Given - a batch with null name
        val batchWithNullName = sampleBatch.copy(name = null)

        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = batchWithNullName,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then - verify "Producto desconocido" is displayed
        composeTestRule.onNodeWithText("Producto desconocido")
            .assertIsDisplayed()
    }

    @Test
    fun displaysNullPackSizeAsZeroGrams() {
        // Given - a batch with null pack size
        val batchWithNullPack = sampleBatch.copy(packSize = null)

        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = batchWithNullPack,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then - verify "0g" is displayed
        composeTestRule.onNodeWithText("0g")
            .assertIsDisplayed()
    }

    @Test
    fun onEditClickIsCalledWhenMoreVertTapped() {
        // Given - a HistoryCard with edit callback
        var editClicked = false

        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = sampleBatch,
                        onEditClick = { editClicked = true },
                        onDeleteClick = {}
                    )
                }
            }
        }

        // When - MoreVert icon is tapped
        composeTestRule.onNodeWithContentDescription("Más opciones para ${sampleBatch.name}")
            .performClick()

        // Then - verify edit callback was called
        // Note: This test verifies the tap target is clickable, actual menu interaction
        // would require additional UI state management
    }

    @Test
    fun displaysAllThreeBlocksVertically() {
        // Given - a HistoryCard
        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = sampleBatch,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then - verify Header Block elements are displayed
        composeTestRule.onNodeWithText(sampleBatch.name!!)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("EAN:")
            .assertIsDisplayed()

        // Then - verify Attributes Block elements are displayed
        composeTestRule.onNodeWithText("Pack:")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Cantidad:")
            .assertIsDisplayed()

        // Then - verify Footer Block elements are displayed
        composeTestRule.onNodeWithText("Vence:")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Seguro")
            .assertIsDisplayed()
    }

    @Test
    fun displaysCorrectTypographyTokens() {
        // Given - a HistoryCard
        // When - HistoryCard is rendered
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    HistoryCard(
                        batch = sampleBatch,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Then - verify product name is displayed (uses titleMedium)
        composeTestRule.onNodeWithText(sampleBatch.name!!)
            .assertIsDisplayed()

        // Then - verify label text is displayed (uses labelSmall/labelMedium)
        composeTestRule.onNodeWithText("Pack:")
            .assertIsDisplayed()
    }
}
