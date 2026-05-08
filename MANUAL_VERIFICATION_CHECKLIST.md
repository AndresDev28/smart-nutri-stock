# Manual Verification Checklist - History Screen Redesign
# Phase 4: Integration & Verification

## Overview
This checklist is for manual testing on Samsung XCover7 physical device to verify that the HistoryScreen redesign meets all requirements from the spec.

## Prerequisites
- Device: Samsung XCover7 (or similar small screen device)
- APK built from this change
- Test data: Multiple batches with GREEN, YELLOW, and EXPIRED statuses
- Test data: Batches with action buttons visible (YELLOW/EXPIRED with PENDING state)

## Test Cases

### 1. HistoryScreen Loading and Display
- [ ] HistoryScreen loads without crashes
- [ ] Batches are displayed in LazyColumn list
- [ ] List scrolls smoothly with multiple items
- [ ] No layout issues or visual glitches

### 2. HistoryCard Structure (3-Block Layout)
For each displayed card, verify:
- [ ] **Header Block** is displayed at the top
  - [ ] Product name shows in Plus Jakarta Sans SemiBold (TextDark)
  - [ ] EAN displays below name in JetBrains Mono RoyalBluePrimary
  - [ ] MoreVert icon (three dots) is on the right side
  - [ ] Tap target for MoreVert is comfortable (>= 48dp)

- [ ] **Attributes Block** displays below Header
  - [ ] Pack shows CalendarMonth icon + "Pack:" label + value in grams
  - [ ] Quantity shows Inventory icon + "Cant:" label + value
  - [ ] Spacing between Pack and Quantity is 16dp
  - [ ] Icons have proper content descriptions for accessibility

- [ ] **Footer Block** displays at the bottom
  - [ ] "Vence:" label shows in TextGray
  - [ ] Expiry date displays in status color (Teal/Amber/DeepRed)
  - [ ] StatusPill displays on the right side
  - [ ] StatusPill has CircleShape and correct background alpha (10-15%)

### 3. StatusPill Verification
Verify for each batch status:
- [ ] **GREEN (Seguro)**
  - [ ] StatusPill shows teal/teal-green background
  - [ ] CheckCircle icon displays
  - [ ] Text shows "Seguro"

- [ ] **YELLOW (Próximo)**
  - [ ] StatusPill shows amber/yellow background
  - [ ] Warning icon displays
  - [ ] Text shows "Próximo"

- [ ] **EXPIRED (Caducado)**
  - [ ] StatusPill shows deep red background
  - [ ] Error icon displays
  - [ ] Text shows "Caducado"

### 4. MoreVert Menu Interaction
- [ ] Tap MoreVert icon on any batch card
- [ ] Dropdown menu appears with two options:
  - [ ] Edit option (Editar)
  - [ ] Delete option (Eliminar)
- [ ] Menu tap target is comfortable (>= 48dp)
- [ ] Menu dismisses when tapping outside
- [ ] Menu dismisses when selecting an option
- [ ] Edit action opens Edit bottom sheet (existing behavior)
- [ ] Delete action triggers soft delete with Snackbar undo (existing behavior)

### 5. Action Buttons for YELLOW/EXPIRED Batches
Verify action buttons appear below the card for YELLOW and EXPIRED batches only:

**YELLOW Batches:**
- [ ] With PENDING action: Show two buttons
  - [ ] "Desc -20%" button (applies discount)
  - [ ] "Retirar" button (removes from public)
- [ ] With DISCOUNTED action: Show "Deshacer Dto" button

**EXPIRED Batches:**
- [ ] With PENDING action: Show "Retirar del Público" button
- [ ] With DISCOUNTED action: Show hybrid state
  - [ ] "Descuento Aplicado" chip (non-interactive)
  - [ ] "Eliminar" button (red)
- [ ] With REMOVED action: Show "Eliminar" button (red)

**GREEN Batches:**
- [ ] No action buttons displayed (as per spec)

### 6. Tap Target Verification (XCover7)
Measure or verify comfortable tap targets:
- [ ] MoreVert icon: >= 48dp x 48dp
- [ ] Action buttons: >= 48dp height
- [ ] Filter chips: >= 48dp height
- [ ] One-handed reachability tested (top to bottom of list)

### 7. Typography Verification
- [ ] Product name: Plus Jakarta Sans SemiBold
- [ ] EAN: JetBrains Mono (monospaced)
- [ ] Labels ("Pack:", "Cant:", "Vence:"): LabelSmall/LabelMedium
- [ ] StatusPill text: LabelSmall
- [ ] No text clipping on small screen (XCover7)
- [ ] Long product names truncate gracefully (ellipsize if needed)

### 8. Color Verification
- [ ] Product name: TextDark (onSurface)
- [ ] EAN: RoyalBluePrimary (theme token)
- [ ] Pack/Quantity icons: TextSecondary (onSurfaceVariant)
- [ ] "Vence:" label: TextGray (onSurfaceVariant)
- [ ] Expiry date: Matches status color (Teal/Amber/DeepRed)
- [ ] StatusPill backgrounds: 10-15% alpha of status color

### 9. Spacing Verification
- [ ] 8dp vertical spacing between blocks
- [ ] 16dp horizontal spacing between Pack and Quantity
- [ ] 8dp spacing between HistoryCard and action buttons
- [ ] Card padding: 16dp
- [ ] List item spacing: 8dp between cards

### 10. Performance Verification
- [ ] List scrolls smoothly with 10+ items
- [ ] List scrolls smoothly with 50+ items
- [ ] No noticeable lag or frame drops during scroll
- [ ] Loading state (spinner) displays briefly on first load
- [ ] Empty state displays when no batches exist

### 11. Accessibility Verification
- [ ] All icons have meaningful content descriptions
  - [ ] MoreVert: "Más opciones para {product name}"
  - [ ] CalendarMonth: Pack size icon description
  - [ ] Inventory: Quantity icon description
  - [ ] CheckCircle/Warning/Error: Status icon description
- [ ] StatusPill content description: "Estado: {status}"
- [ ] Screen reader announces card structure correctly
- [ ] Focus order follows visual layout

### 12. Navigation Verification
- [ ] Navigate to HistoryScreen from dashboard
- [ ] Navigate back to dashboard
- [ ] Deep link navigation to HistoryScreen with status filter works
- [ ] Filter chips work correctly (Todos/Pendientes/Con acción)

### 13. Edge Cases
- [ ] Batches with null product name display as "Producto desconocido"
- [ ] Batches with null pack size display as "0g"
- [ ] Very long product names (50+ chars) don't break layout
- [ ] Batches with large quantities (e.g., 999) display correctly
- [ ] Expiry dates with single digits display correctly (e.g., "01/05/2026")

### 14. Theme Token Reuse
Verify no hardcoded colors:
- [ ] Colors come from MaterialTheme.colorScheme
- [ ] Typography tokens used from MaterialTheme.typography
- [ ] Shapes use MaterialTheme.shapes
- [ ] No raw RGB hex codes or Color() values

### 15. Existing Components Verification
Verify no breaking changes to existing components:
- [ ] NutriCard still exists and is not renamed
- [ ] StatusCircle still exists and is not renamed
- [ ] InfoChip still exists and is not renamed
- [ ] Dashboard still uses NutriCard (unchanged)

## Known Limitations
- Action buttons for YELLOW/RED batches are in `BatchCardWithActions` (private function)
- Direct unit testing of action button interactions not possible without refactoring
- Action button functionality verified through manual testing and existing integration tests

## Test Execution Notes
1. Execute this checklist on Samsung XCover7 or similar device
2. Mark each item as [x] when verified
3. Document any issues or deviations from expected behavior
4. Take screenshots of key screens for documentation

## Sign-Off
- Tester Name: _________________
- Date: __________________
- Device: ______________________
- APK Version: ________________
- Overall Status: [ ] PASS [ ] FAIL [ ] PARTIAL

## Issues Found
(Document any issues discovered during testing)
1.
2.
3.
