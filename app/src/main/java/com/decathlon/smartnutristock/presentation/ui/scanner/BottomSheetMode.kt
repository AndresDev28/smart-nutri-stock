package com.decathlon.smartnutristock.presentation.ui.scanner

/**
 * Mode for the ProductRegistrationBottomSheet.
 *
 * - CREATE: Registering a new product (product not found in catalog)
 * - EDIT: Editing an existing batch (from History screen)
 */
enum class BottomSheetMode {
    CREATE,
    EDIT
}
