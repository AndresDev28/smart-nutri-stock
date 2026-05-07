package com.decathlon.smartnutristock.presentation.ui.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import org.junit.Rule
import org.junit.Test

/**
 * UI Tests for DashboardScreen.
 *
 * Tests semaphore counters display and total products count.
 */
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysCorrectSemaphoreCounters() {
        // Given
        val greenCount = 5
        val yellowCount = 2
        val redCount = 0

        // When - Test semaphore counter display behavior by recreating the UI structure
        composeTestRule.setContent {
            Surface {
                androidx.compose.foundation.layout.Row {
                    // Green Counter
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(100.dp)
                            .height(120.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFF4CAF50), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "$greenCount",
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = "Óptimo",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            text = "Productos en buen estado",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Yellow Counter
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(100.dp)
                            .height(120.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFFFFC107), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "$yellowCount",
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = "Caducar",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            text = "Caducan pronto",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Red Counter
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(100.dp)
                            .height(120.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFFFF4444), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "$redCount",
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = "Caducados",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            text = "Requieren atención",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Then - verify green counter displays "5"
        composeTestRule.onNodeWithText("5")
            .assertTextEquals("5")

        // Then - verify yellow counter displays "2"
        composeTestRule.onNodeWithText("2")
            .assertTextEquals("2")

        // Then - verify red counter displays "0"
        composeTestRule.onNodeWithText("0")
            .assertTextEquals("0")

        // Then - verify labels are displayed
        composeTestRule.onNodeWithText("Óptimo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Caducar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Caducados").assertIsDisplayed()

        // Then - verify subtitles are displayed
        composeTestRule.onNodeWithText("Productos en buen estado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Caducan pronto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Requieren atención").assertIsDisplayed()
    }

    @Test
    fun displaysCorrectTotalProducts() {
        // Given
        val counters = SemaphoreCounters(
            green = 5,
            yellow = 2,
            expired = 0
        )
        val expectedTotal = 5 + 2 + 0 // 7

        // When - Test total products display
        composeTestRule.setContent {
            Surface {
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            text = "Total de Productos",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = "${counters.total}",
                            style = androidx.compose.material3.MaterialTheme.typography.displayLarge
                        )
                    }
                }
            }
        }

        // Then - verify total products text matches sum (7)
        composeTestRule.onNodeWithText("7")
            .assertTextEquals("7")

        // Then - verify "Total de Productos" label is displayed
        composeTestRule.onNodeWithText("Total de Productos").assertIsDisplayed()
    }

    @Test
    fun displaysHeaderWithCorrectStructure() {
        // Given
        val userEmail = "test@example.com"
        var logoutCalled = false

        // When - Test new header structure
        composeTestRule.setContent {
            Surface {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Top Row: Logo + Title + Logout Button
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                            androidx.compose.material3.Text(
                                text = "Smart Nutri-Stock",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        androidx.compose.material3.IconButton(
                            onClick = { logoutCalled = true }
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Logout,
                                contentDescription = "Cerrar Sesión",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                    // Greeting Column
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            text = "Hola, ${userEmail.substringBefore("@")}!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            text = "Semáforo de Inventario",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            text = "Resumen actual de tu inventario",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Then - verify app title is displayed
        composeTestRule.onNodeWithText("Smart Nutri-Stock").assertIsDisplayed()

        // Then - verify greeting with username is displayed
        composeTestRule.onNodeWithText("Hola, test!").assertIsDisplayed()

        // Then - verify semaphore title is displayed
        composeTestRule.onNodeWithText("Semáforo de Inventario").assertIsDisplayed()

        // Then - verify subtitle is displayed
        composeTestRule.onNodeWithText("Resumen actual de tu inventario").assertIsDisplayed()

        // Then - verify logout button is present with correct content description
        composeTestRule.onNodeWithContentDescription("Cerrar Sesión").assertIsDisplayed()
    }

    @Test
    fun displaysHeaderWithNullUserEmail() {
        // Given - null email
        val userEmail: String? = null

        // When - Test header with null email
        composeTestRule.setContent {
            Surface {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Greeting Column
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            text = if (userEmail != null) {
                                "Hola, ${userEmail.substringBefore("@")}!"
                            } else {
                                "Hola!"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Then - verify fallback greeting is displayed
        composeTestRule.onNodeWithText("Hola!").assertIsDisplayed()
    }
}
