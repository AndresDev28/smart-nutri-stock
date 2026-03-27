package com.decathlon.smartnutristock.presentation.ui.scanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * UI Tests for ProductRegistrationBottomSheet.
 *
 * Tests form validation and user interactions.
 */
class ProductRegistrationBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysInitialState_withScannedEAN() {
        // Given
        val testEan = "1234567890123"
        var onDismissCalled = false
        var onSaveCalled = false
        var savedProductName = ""
        var savedPackSize = 0

        // When
        composeTestRule.setContent {
            ProductRegistrationBottomSheet(
                ean = testEan,
                onDismiss = { onDismissCalled = true },
                onRegister = { name, packSize ->
                    onSaveCalled = true
                    savedProductName = name
                    savedPackSize = packSize
                }
            )
        }

        // Then - verify EAN is displayed and is read-only (enabled=false shows it's read-only)
        composeTestRule.onNodeWithTag("ean_field")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(testEan)
            .assertIsDisplayed()

        // Then - verify title is displayed
        composeTestRule.onNodeWithText("Registrar Nuevo Producto")
            .assertIsDisplayed()

        // Then - verify name field is empty
        composeTestRule.onNodeWithTag("name_field")
            .assertIsDisplayed()

        // Then - verify pack size field is empty
        composeTestRule.onNodeWithTag("pack_size_field")
            .assertIsDisplayed()

        // Then - verify Save button is initially disabled (form not valid)
        composeTestRule.onNodeWithTag("save_button")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun showsValidationError_whenNameIsEmpty() {
        // Given
        val testEan = "1234567890123"
        var onSaveCalled = false

        // When
        composeTestRule.setContent {
            ProductRegistrationBottomSheet(
                ean = testEan,
                onDismiss = { },
                onRegister = { _, _ -> onSaveCalled = true }
            )
        }

        // When - enter valid pack size but leave name empty
        composeTestRule.onNodeWithTag("pack_size_field")
            .performTextInput("500")

        // Then - verify Save button is still disabled (name is empty)
        composeTestRule.onNodeWithTag("save_button")
            .assertIsNotEnabled()

        // Then - verify onRegister is not called
        assert(!onSaveCalled) { "onRegister should not be called when name is empty" }
    }

    @Test
    fun showsValidationError_whenNameIsTooShort() {
        // Given
        val testEan = "1234567890123"
        var onSaveCalled = false

        // When
        composeTestRule.setContent {
            ProductRegistrationBottomSheet(
                ean = testEan,
                onDismiss = { },
                onRegister = { _, _ -> onSaveCalled = true }
            )
        }

        // When - enter name with only 2 characters (below minimum of 3)
        composeTestRule.onNodeWithTag("name_field")
            .performTextInput("ab")

        // Then - verify error message is displayed
        composeTestRule.onNodeWithText("Mínimo 3 caracteres")
            .assertIsDisplayed()

        // Then - verify Save button is disabled
        composeTestRule.onNodeWithTag("save_button")
            .assertIsNotEnabled()

        // Then - verify onRegister is not called
        assert(!onSaveCalled) { "onRegister should not be called when name is too short" }
    }

    @Test
    fun enablesSaveButton_andCallsOnRegister_whenFormIsValid() {
        // Given
        val testEan = "1234567890123"
        var onDismissCalled = false
        var onSaveCalled = false
        var savedProductName = ""
        var savedPackSize = 0

        // When
        composeTestRule.setContent {
            ProductRegistrationBottomSheet(
                ean = testEan,
                onDismiss = { onDismissCalled = true },
                onRegister = { name, packSize ->
                    onSaveCalled = true
                    savedProductName = name
                    savedPackSize = packSize
                }
            )
        }

        // When - enter valid name
        composeTestRule.onNodeWithTag("name_field")
            .performTextInput("Proteína Whey")

        // When - enter valid pack size
        composeTestRule.onNodeWithTag("pack_size_field")
            .performTextInput("500")

        // Then - verify Save button is enabled
        composeTestRule.onNodeWithTag("save_button")
            .assertIsEnabled()

        // When - click Save button
        composeTestRule.onNodeWithTag("save_button")
            .performClick()

        // Then - verify onRegister callback was called
        assert(onSaveCalled) { "onRegister should be called when form is valid and Save is clicked" }
        assert(savedProductName == "Proteína Whey") { "Product name should be 'Proteína Whey'" }
        assert(savedPackSize == 500) { "Pack size should be 500" }
    }

    @Test
    fun callsOnDismiss_whenCancelButtonClicked() {
        // Given
        val testEan = "1234567890123"
        var onDismissCalled = false
        var onSaveCalled = false

        // When
        composeTestRule.setContent {
            ProductRegistrationBottomSheet(
                ean = testEan,
                onDismiss = { onDismissCalled = true },
                onRegister = { _, _ -> onSaveCalled = true }
            )
        }

        // When - click Cancel button
        composeTestRule.onNodeWithText("Cancelar")
            .performClick()

        // Then - verify onDismiss callback was called
        assert(onDismissCalled) { "onDismiss should be called when Cancel is clicked" }

        // Then - verify onRegister was not called
        assert(!onSaveCalled) { "onRegister should not be called when Cancel is clicked" }
    }

    @Test
    fun showsValidationError_whenPackSizeIsNotPositive() {
        // Given
        val testEan = "1234567890123"
        var onSaveCalled = false

        // When
        composeTestRule.setContent {
            ProductRegistrationBottomSheet(
                ean = testEan,
                onDismiss = { },
                onRegister = { _, _ -> onSaveCalled = true }
            )
        }

        // When - enter valid name
        composeTestRule.onNodeWithTag("name_field")
            .performTextInput("Proteína Whey")

        // When - enter pack size of 0 (not positive)
        composeTestRule.onNodeWithTag("pack_size_field")
            .performTextInput("0")

        // Then - verify error message is displayed
        composeTestRule.onNodeWithText("Debe ser positivo")
            .assertIsDisplayed()

        // Then - verify Save button is disabled
        composeTestRule.onNodeWithTag("save_button")
            .assertIsNotEnabled()

        // Then - verify onRegister is not called
        assert(!onSaveCalled) { "onRegister should not be called when pack size is not positive" }
    }
}
