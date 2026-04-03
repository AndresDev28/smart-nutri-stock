# Smart Nutri-Stock: Future Roadmap

> **Document Status**: Post-MVP Phase 2 Planning  
> **Last Updated**: 28/03/2026  
> **MVP Version**: 2.1.0  
> **Target Device**: Samsung XCover7 (B2B Internal Tool)

---

## Executive Summary

This document captures all deferred features and design directions identified during the MVP sprint. The MVP (v2.1.0) successfully delivers:
- ✅ Barcode scanning with product registration
- ✅ Batch management with expiry dates
- ✅ Semaphore status system (Green/Yellow/Red/Expired)
- ✅ Dashboard with real-time counters
- ✅ History screen with status filtering
- ✅ TDD coverage (81+ unit tests)
- ✅ CI/CD pipeline with GitHub Actions

The following features are prioritized for **Phase 2 (Post-MVP)** development.

---

## Prioritized Backlog

### 🔴 CRÍTICA (1): Edición y Borrado Completo (CRUD)

**Priority**: P0 - Critical  
**Effort**: Medium (3-5 days)  
**Dependencies**: None

#### Description
Allow users to tap a product card in the History screen to edit or delete batch data. This completes the CRUD cycle for inventory management.

#### Requirements
- [ ] Tap gesture on `BatchCard` in `HistoryScreen` opens bottom sheet or dialog
- [ ] Edit mode allows modification of:
  - Gramaje (weight/quantity)
  - Fecha de caducidad (expiry date)
  - EAN (barcode)
- [ ] Delete action with confirmation dialog
- [ ] Confirmation dialog copy: "¿Estás seguro de que quieres eliminar este lote?"
- [ ] Undo snackbar for 5 seconds after deletion

#### Technical Approach
- Reuse `ProductRegistrationBottomSheet` pattern for edit mode
- Add `DeleteBatchUseCase` in Domain layer
- Implement soft-delete in `StockRepository` (mark as deleted, hard delete after 30 days)
- Add `BatchEditDialog` composable

#### Acceptance Criteria
```gherkin
Given a product exists in history
When user taps the product card
Then an edit/delete dialog appears
And user can modify gramaje, fecha, or EAN
And user can delete with confirmation
```

---

### 🟠 ALTA (2): Botones de Acción (El "Workflow")

**Priority**: P1 - High  
**Effort**: Medium (3-5 days)  
**Dependencies**: None

#### Description
Add action buttons to products based on their semaphore status. This creates a workflow for inventory management decisions.

#### Requirements
- [ ] **Yellow products** show action: "Acción tomada: Pegatina -20% Dto"
  - Tapping marks product as "discount applied"
  - Changes status indicator (optional: new status "DISCOUNTED")
- [ ] **Red/Expired products** show action: "Acción tomada: Retirado del Público"
  - Tapping marks product as "removed from shelf"
  - Changes status indicator (optional: new status "REMOVED")
- [ ] Action history tracked per batch
- [ ] Filter in History to show only "pending action" items

#### Technical Approach
- Add `BatchAction` sealed class in Domain (DISCOUNT, REMOVE, NONE)
- Extend `ActiveStockEntity` with `actionTaken` field
- Add `RecordBatchActionUseCase`
- UI: Action buttons appear conditionally in `BatchCard`

#### Acceptance Criteria
```gherkin
Given a product has YELLOW status (expires in 1-3 days)
When user taps "Aplicar -20% Dto" button
Then product is marked as "discounted"
And action is logged with timestamp

Given a product has RED/EXPIRED status
When user taps "Retirar del Público" button
Then product is marked as "removed"
And action is logged with timestamp
```

---

### 🟠 ALTA (3): Exportación de Reportes (CSV/PDF)

**Priority**: P1 - High  
**Effort**: Medium (5-7 days)  
**Dependencies**: None

#### Description
Generate and export inventory status reports for management review.

#### Requirements
- [ ] Export button in `HistoryScreen` (top app bar)
- [ ] Export formats: CSV and PDF
- [ ] Report includes:
  - Total products by status (Green/Yellow/Red/Expired)
  - List of all batches with EAN, name, expiry date, status
  - Actions taken (if #2 implemented)
  - Generation timestamp
- [ ] Share intent to email or save to device
- [ ] Filter options before export (by status, date range)

#### Technical Approach
- Use `Apache POI` or `OpenPDF` for PDF generation
- CSV generation with Kotlin CSV library
- Android `FileProvider` for secure file sharing
- `Intent.ACTION_SEND` for sharing

#### Acceptance Criteria
```gherkin
Given user is on History screen
When user taps "Exportar" button
Then export dialog appears with format options (CSV/PDF)
And user can filter by status/date
And generated report can be shared via email or saved
```

---

### 🟡 MEDIA (4): OCR de Fechas con Cámara

**Priority**: P2 - Medium  
**Effort**: High (7-10 days)  
**Dependencies**: Camera permissions, ML Kit integration

#### Description
Use Google ML Kit to scan expiry dates directly from product packaging, eliminating manual date entry.

#### Requirements
- [ ] Integrate Google ML Kit Text Recognition API
- [ ] Camera preview in batch input flow
- [ ] Date pattern recognition (DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD)
- [ ] Fallback to manual entry if OCR fails
- [ ] Confidence threshold display (e.g., "95% confident")
- [ ] Support for multiple date formats common in EU

#### Technical Approach
- Add `com.google.mlkit:text-recognition` dependency
- Create `DateOcrScanner` use case
- CameraX for camera preview
- Regex patterns for date extraction
- Store scanned image for audit (optional)

#### Acceptance Criteria
```gherkin
Given user is adding a new batch
When user taps "Escanear Fecha" button
Then camera preview opens
And OCR attempts to detect date on packaging
And detected date is auto-filled with confidence score
And user can confirm or edit the date
```

---

### 🟡 MEDIA (5): Autenticación (Login) & Sincronización Nube

**Priority**: P2 - Medium  
**Effort**: Very High (10-15 days)  
**Dependencies**: Backend infrastructure decision (Supabase vs Firebase)

#### Description
Implement user authentication and cloud synchronization for multi-device support and audit trail.

#### Requirements
- [ ] Login screen with username/password
- [ ] Session management (JWT tokens)
- [ ] Cloud database sync (Supabase or Firebase Firestore)
- [ ] Offline-first architecture (local Room as source of truth)
- [ ] Background sync when online
- [ ] Audit trail: track which user created/modified each batch
- [ ] Multi-device support

#### Technical Approach
- **Option A: Supabase** (recommended for cost)
  - Postgres database with Row Level Security
  - Supabase Auth for authentication
  - Real-time sync with Postgres Changes
- **Option B: Firebase**
  - Firestore for database
  - Firebase Auth for authentication
  - Cloud Firestore offline persistence

#### Architecture Changes
- Add `User` domain model
- Add `userId` to `ActiveStockEntity`
- Implement `AuthRepository` interface
- Create `SyncManager` for offline/online sync
- Add `LoginScreen` composable

#### Acceptance Criteria
```gherkin
Given app is opened for first time
When user enters credentials
Then authentication succeeds
And local data syncs with cloud

Given device is offline
When user makes changes
Then changes are stored locally
And sync when connection restored

Given multiple devices with same account
When batch is added on device A
Then batch appears on device B after sync
```

---

## Diseño Premium & UI (Priority 6)

**Priority**: P3 - Design Excellence  
**Effort**: Medium (5-7 days)  
**Reference**: `image_40.png` (confirmed mock-up)

### El Logo 'Barcode Leaf'

A stylized circular emblem that combines:
- **Leaf Motif**: Represents freshness, nutrition, and organic/healthy products
- **Abstracted Barcode Shape**: Curved barcode lines integrated into the leaf design
- **Symbolism**: Technology + Nature = Smart inventory for fresh products

**Color Scheme**:
- **Teal Green** (#00897B): Nature, freshness, safety
- **Royal Blue** (#1565C0): Trust, technology, logistics

### Paleta de Colores Premium

Move away from pure Material Design White to a **premium, softer aesthetic**:

| Element | Current (MVP) | Future (Premium) | Hex Code |
|---------|---------------|------------------|----------|
| Background | White (#FFFFFF) | Off-white/Cream | #FAFAF8 |
| Surface | White (#FFFFFF) | Warm White | #F5F5F0 |
| Primary Action | Blue | Royal Blue | #1565C0 |
| Safe Status | Green | Teal | #00897B |
| Warning | Yellow | Amber | #FFB300 |
| Danger | Red | Deep Red | #C62828 |
| Text Primary | Black | Dark Gray | #212121 |
| Text Secondary | Gray | Soft Gray | #757575 |

### Fonts & Typography

- **Headlines**: Modern sans-serif (Poppins or Inter Bold)
- **Body**: Clean readable font (Inter Regular)
- **Numbers/Data**: Monospace for alignment (JetBrains Mono)

### UI Refinements

- **Subtle Gradients**: Replace flat colors with gentle gradients on cards
- **Rounded Corners**: 16dp-24dp radius on cards and buttons
- **Elevation**: Soft shadows (2-4dp) instead of harsh Material elevation
- **Micro-interactions**: Smooth transitions (300ms) on status changes
- **Empty States**: Illustrated placeholders when no products exist

### Implementation Files to Update

```
res/
├── values/
│   └── colors.xml          # Update with premium palette
├── drawable/
│   └── ic_logo_leaf.xml    # New logo vector
└── font/
    ├── poppins_bold.xml
    └── inter_regular.xml
```

---

## Technical Debt & Improvements

### Addressed in MVP
- ✅ SemaphoreStatus unified (RED removed)
- ✅ Clean Architecture established
- ✅ TDD coverage (81+ tests)
- ✅ CI/CD pipeline

### Future Considerations
- [ ] **Performance**: Profile app on low-end devices
- [ ] **Accessibility**: Add TalkBack support, high contrast mode
- [ ] **Localization**: Prepare strings for ES/FR/EN
- [ ] **Analytics**: Add Firebase Analytics for usage tracking
- [ ] **Crash Reporting**: Integrate Crashlytics

---

## Release Planning

### Phase 2.1 - CRUD Completion (Sprint 1-2)
- Feature #1: Edición y Borrado
- Estimated: 2 weeks

### Phase 2.2 - Workflow Actions (Sprint 3-4)
- Feature #2: Botones de Acción
- Feature #3: Exportación Reportes
- Estimated: 3 weeks

### Phase 2.3 - Advanced Features (Sprint 5-7)
- Feature #4: OCR de Fechas
- Feature #5: Autenticación & Sync
- Estimated: 4-5 weeks

### Phase 2.4 - Premium UI (Parallel)
- Feature #6: Diseño Premium
- Can run in parallel with other features
- Estimated: 1-2 weeks

---

## Appendix: SDD Artifacts Reference

All MVP development artifacts are stored in **Engram** (persistent memory):
- `sdd-init/smart-nutri-stock` - Project context
- `sdd/fix-expired-counter-bug/*` - Bug fix artifacts
- `sdd/feature/batch-management/*` - Batch management feature

Future features should follow the **SDD Workflow**:
1. `/sdd-explore` → 2. `/sdd-propose` → 3. `/sdd-spec` → 4. `/sdd-design` → 5. `/sdd-tasks` → 6. `/sdd-apply` → 7. `/sdd-verify` → 8. `/sdd-archive`

---

*Document generated by Andrés & Gemini collaboration session - 28/03/2026*
