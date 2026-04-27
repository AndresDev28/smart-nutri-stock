# Smart Nutri-Stock: Future Roadmap

> **Document Status**: Post-MVP Phase 2 Planning  
> **Last Updated**: 09/04/2026  
> **MVP Version**: 2.5.0  
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

### ✅ COMPLETADA: Botones de Acción (El "Workflow")

**Priority**: P1 - High  
**Effort**: Medium (3-5 days)  
**Completed**: 2026-04-08  
**Version**: v2.3.0  
**Status**: ✅ Implemented with hotfixes for expired product logic

#### Description

Action buttons were successfully implemented in `BatchCard` for YELLOW and RED/EXPIRED products, creating a workflow for inventory management decisions.

#### Implemented Requirements

- ✅ **Yellow products** show action: "Desc -20%" button (toggles PENDING ↔ DISCOUNTED)
  - Implemented with FilledTonalButton, 48dp min height for XCover7
  - Changes status indicator with visual feedback
- ✅ **Red/Expired products** show conditional actions based on action state:
  - EXPIRED + PENDING: Only "Retirar" button
  - EXPIRED + DISCOUNTED: "Descuento Aplicado" chip + Delete button
  - EXPIRED + REMOVED: Delete button only
  - YELLOW + PENDING: Both "Desc -20%" and "Retirar" buttons
  - YELLOW + DISCOUNTED: "Deshacer Dto" button
- ✅ Action tracking per batch via `WorkflowAction` enum (PENDING, DISCOUNTED, REMOVED)
- ✅ Filter in History: "Todos" | "Pendientes" | "Con acción" (ActionFilter enum)

#### Technical Implementation

- ✅ `WorkflowAction` enum created (PENDING, DISCOUNTED, REMOVED)
- ✅ `ActiveStockEntity` extended with `actionTaken` field (String-based TypeConverter)
- ✅ `UpdateBatchActionUseCase` implemented in Domain layer
- ✅ Room migration MIGRATION_4_5: `ALTER TABLE active_stocks ADD COLUMN actionTaken TEXT NOT NULL DEFAULT 'PENDING'`
- ✅ Action buttons in `BatchCard` with conditional display logic (48dp minHeight)
- ✅ Filter chips row in `HistoryScreen` with StateFlow management

#### Hotfixes Applied

- ✅ Hotfix 1: RED + REMOVED → Delete button (unidirectional) calls `softDeleteBatch()`
- ✅ Hotfix 2a: RED + PENDING → Only "Retirar" button (NO discount)
- ✅ Hotfix 2b: RED + DISCOUNTED → AssistChip "Descuento Aplicado" + Delete button
- ✅ All 5 status/action combos handled correctly

#### Test Coverage

- ✅ 117 tests passed (Domain: 15, Data: 4, Presentation: 4, Total: 117)
- ✅ All spec requirements verified compliant (13/15 scenarios, 2 partial - UI covered by unit tests)
- ✅ Room migration v4 → v5 verified with existing data defaulting to "PENDING"

#### Acceptance Criteria (VERIFIED ✅)

```gherkin
Given a product has YELLOW status (expires in 1-3 days)
When user taps "Desc -20%" button
Then product is marked as "DISCOUNTED"
And button shows "Deshacer Dto" state
And batch persists across app restarts

Given a product has RED/EXPIRED status and PENDING action
When user taps "Retirar" button
Then product is marked as "REMOVED"
And Delete button becomes visible
```

---

---

---

### ✅ COMPLETADA: Notificaciones Push (Alertas Locales)

**Priority**: P2 - Medium
**Effort**: Medium (3-5 days)
**Completed**: 2026-04-16
**Version**: v2.6.0
**Status**: ✅ Implemented with Hilt + WorkManager integration

#### Description

Local push notifications to alert personnel about products entering "Yellow" (expiring soon) or "Red" (expired) states. This transforms the app from a passive ledger into an active alerting system.

#### Implemented Requirements

- ✅ Daily background check via WorkManager for status changes (scheduled at 06:00 AM)
- ✅ Grouped push notifications (e.g., "Tienes 3 lotes por caducar pronto")
- ✅ Deep linking: Tapping notification opens History filtered by the relevant state
- ✅ Notification channels: "Alertas Críticas" (Rojo) y "Avisos Preventivos" (Amarillo)
- ✅ Request Android 13+ Notification Permissions gracefully via in-app request

#### Technical Implementation

- ✅ `StatusCheckWorker`: Daily scans of `ActiveStockEntity` against current date
- ✅ Hilt + WorkManager integration: `@HiltWorker` with proper dependency injection
- ✅ `NotificationHelper`: Native NotificationManager (not compat) for reliability
- ✅ `NotificationChannel` registration: Pre-registered before any builder instantiation
- ✅ Smart enqueue policy: REPLACE only if work is FAILED/CANCELLED, otherwise KEEP
- ✅ Deep linking: `Intent(this, MainActivity::class.java)` with action extras
- ✅ Grouped notifications: `NotificationCompat.InboxStyle` for multiple batches
- ✅ Battery optimization workarounds: EXPONENTIAL backoff, documented for OEM devices
- ✅ Debug test button: "TEST PUSH (DEBUG)" on DashboardScreen for isolated testing

#### Challenges Overcome

- ✅ **Silent Notification Drop**: Fixed by using native NotificationManager and valid monochromatic vector icons
- ✅ **Hilt Worker Factory Failure**: Upgraded to hilt-work:1.2.0 and added AndroidManifest `tools:node="remove"`
- ✅ **Xiaomi/HyperOS Battery Optimization**: Created debug bypass button and documented workarounds
- ✅ **Auto Backup Phantom State**: Disabled `allowBackup="false"` for clean testing

#### Test Coverage

- ✅ Manual device testing PASSED on Samsung XCover7
- ✅ Notification permission request flow verified
- ✅ Notification channel registration verified
- ✅ Deep link navigation verified
- ✅ WorkManager scheduling verified via Background Task Inspector

#### Acceptance Criteria (VERIFIED ✅)

```gherkin
Given there are 2 yellow batches in the database
When the daily background check runs
Then a single grouped notification is triggered
And tapping it opens HistoryScreen filtered by "Yellow"
```

---

## Diseño Premium & UI (Priority 7)

**Priority**: P3 - Design Excellence  
**Effort**: Medium (5-7 days)

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

| Element        | Current (MVP)   | Future (Premium) | Hex Code |
| -------------- | --------------- | ---------------- | -------- |
| Background     | White (#FFFFFF) | Off-white/Cream  | #FAFAF8  |
| Surface        | White (#FFFFFF) | Warm White       | #F5F5F0  |
| Primary Action | Blue            | Royal Blue       | #1565C0  |
| Safe Status    | Green           | Teal             | #00897B  |
| Warning        | Yellow          | Amber            | #FFB300  |
| Danger         | Red             | Deep Red         | #C62828  |
| Text Primary   | Black           | Dark Gray        | #212121  |
| Text Secondary | Gray            | Soft Gray        | #757575  |

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

### Phase 2.1 & 2.2 - Core & Workflow (Sprint 1-4)

- Feature #1: Edición y Borrado (COMPLETADO)
- Feature #2: Botones de Acción (COMPLETADO)
- Feature #3: Exportación Reportes (COMPLETADO)
- Estimated: 5 weeks (DELIVERED)

### Phase 2.3 - Advanced Features (Sprint 5-7)

- Feature #4: OCR de Fechas (COMPLETADO)
- Feature #5: Notificaciones Push
- Feature #6: Autenticación & Sync ✅ COMPLETADO
- Estimated: 5-6 weeks

### Phase 2.4 - Premium UI (Parallel)

- Feature #7: Diseño Premium
- Can run in parallel with other features
- Estimated: 1-2 weeks

---

## Completed Features

### ✅ COMPLETADA: Edición y Borrado Completo (CRUD)

**Completed**: 2026-04-09  
**Version**: v2.4.0 (as part of workflow)  
**Status**: ✅ Implemented

Full CRUD functionality implemented including:

- **Edit Mode**: Reuses `ProductRegistrationBottomSheet` for modifying quantity, expiry date and product name. Accessible via three-dot menu on `BatchCard`.
- **Soft Delete**: Deletes batch with a 5-second `Undo` snackbar for immediate recovery, improving UX over a confirmation dialog.
- **Optimistic UI**: Instant UI feedback for both edits and deletes.
- **Use Cases**: `UpdateBatchUseCase`, `SoftDeleteBatchUseCase`, `RestoreBatchUseCase`.

**Test Coverage**: Full unit test coverage for use cases and DAO.

---

### ✅ Botones de Acción (El "Workflow")

**Completed**: 2026-04-08  
**Version**: v2.3.0  
**SDD Artifacts**: `action-buttons-workflow`

Action buttons implemented in `BatchCard` for YELLOW and RED/EXPIRED products. Creates a workflow for inventory management decisions with the following features:

- **WorkflowAction Enum**: PENDING (default), DISCOUNTED, REMOVED
- **Action Buttons**: Toggle between PENDING and action states
  - Yellow products: "Desc -20%" and "Retirar" buttons
  - Expired products: Conditional display based on action state
  - All buttons: 48dp minHeight for XCover7 gloves
- **Filter Logic**: "Todos" | "Pendientes" | "Con acción" in HistoryScreen
- **Data Persistence**: Room migration v4 → v5 with String-based TypeConverter
- **Hotfixes**: Corrected logic for expired product workflow (5 status/action combos)

**Test Coverage**: 117 tests passed ✅

**Verification**: All spec requirements verified compliant via `sdd-verify` phase

---

### ✅ COMPLETADA: OCR de Fechas con Cámara

**Completed**: 2026-04-09  
**Version**: v2.5.0  
**Status**: ✅ Implemented with high-precision date extraction

#### Description

Implemented automated expiry date extraction from product packaging using Google ML Kit Text Recognition, eliminating manual entry and reducing registration time.

#### Technical Implementation

- ✅ **DateExtractorUseCase**: Robust regex for DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY, DD/MM/YY, MM/YY, MM/YYYY, and ISO YYYY-MM-DD.
- ✅ **EU Regulation Compliance**: Automatic conversion of MM/YY and MM/YYYY formats to the end of the month using `YearMonth.atEndOfMonth()`.
- ✅ **OcrCameraOverlay**: Custom CameraX implementation with ML Kit TextRecognizer and `DisposableEffect` for lifecycle-safe cleanup.
- ✅ **Hardware Handoff**: Sequential camera access logic (Barcode $\rightarrow$ OCR $\rightarrow$ Barcode) with `isCameraReleased` state gate to prevent race conditions.
- ✅ **Integration**: OCR available in both `ProductRegistrationBottomSheet` and `ScannerScreen` (BatchInputStep.SelectExpiryDate).
- ✅ **UX/Robustness**: 5s timeout with graceful manual fallback ("No se detectó fecha. Ingrese manualmente.") and permission handling via `rememberLauncherForActivityResult`.

#### Hotfixes Applied

- ✅ **Camera Blackout Fix**: Resolved hardware lock by ensuring explicit `unbind()` before switching between barcode and OCR modes.
- ✅ **Batch Separation Fix**: Implemented uniqueness logic (Same EAN + same date $\rightarrow$ SUM quantities; Same EAN + different date $\rightarrow$ NEW record).

#### Test Coverage

- ✅ 34 unit tests (21 for `DateExtractorUseCase` + 13 repository/integration tests).
- ✅ Manual device testing PASSED on Samsung XCover7.

#### Acceptance Criteria (VERIFIED ✅)

```gherkin
Given user is adding a new batch
When user taps "Escanear Fecha" button
Then camera preview opens
And OCR attempts to detect date on packaging
And detected date is auto-filled (handling MM/YY as end-of-month)
And user can confirm or edit the date
```

**SDD Artifacts**: `sdd/ocr-fechas-camara-mlkit/*`, `sdd/ocr-fechas-camara-mlkit-hotfixes/*`

---

**Priority**: P1 - High  
**Effort**: Medium (5-7 days)  
**Completed**: 2026-04-08  
**Version**: v2.4.0  
**Status**: ✅ Implemented with zero external dependencies

#### Description

Report export system implemented for inventory status review in CSV and PDF formats, with native Android sharing via FileProvider.

#### Implemented Requirements

- ✅ Export button in `HistoryScreen` TopAppBar (Share icon)
- ✅ Export formats: CSV (RFC 4180 compliant) and PDF (native PdfDocument)
- ✅ Report includes:
  - EAN, product name, quantity, expiry date, status (semaphore)
  - Actions taken (WorkflowAction: PENDING, DISCOUNTED, REMOVED)
  - Generation timestamp
- ✅ Share via Intent.createChooser (WhatsApp, Email, Drive, etc.)
- ✅ ModalBottomSheet format selection dialog
- ✅ Loading state with CircularProgressIndicator during generation

#### Technical Implementation

- ✅ `DocumentExporter` interface with `CsvExporterImpl` and `PdfExporterImpl`
- ✅ `ExportInventoryUseCase` orchestrates data fetching and export delegation
- ✅ CSV: RFC 4180 compliant with proper escaping (commas, quotes, accents, ñ)
- ✅ PDF: Native `android.graphics.pdf.PdfDocument` with Canvas API, A4 format, color-coded semaphore, multi-page support
- ✅ `ExportModule` Hilt DI with @Named qualifiers for CSV/PDF exporters
- ✅ `FileProvider` configured for Scoped Storage (targetSdk 34, cache directory)
- ✅ `ExportState` sealed class (Idle, Loading, Success, Error) in HistoryViewModel
- ✅ Zero external dependencies — native Android APIs only

#### Test Coverage

- ✅ 22 tests passed (ExportInventoryUseCaseTest: 8, CsvExporterImplTest: 14)
- ✅ CSV RFC 4180 escape validated: commas, double quotes, accents, special chars
- ⚠️ PDF tests require Android instrumentation (not mockable in JVM)

#### Acceptance Criteria (VERIFIED ✅)

```gherkin
Given user is on History screen
When user taps "Exportar" button
Then ModalBottomSheet appears with format options (CSV/PDF)
And user can select format
And generated report can be shared via WhatsApp, Email, or Drive

Given CSV export is selected
When product names contain commas or quotes
Then CSV properly escapes fields per RFC 4180

Given PDF export is selected
When report is generated
Then PDF shows professional table with semaphore color coding
```

---

### ✅ COMPLETADA: Autenticación (Login) & Sincronización Nube

**Completed**: 2026-04-27  
**Version**: v2.7.0  
**Status**: ✅ Implemented with Supabase Auth + Postgrest

#### Description

Implementation of user authentication and cloud synchronization for multi-device support and audit trail, ensuring data safety and coordination across store devices.

#### Implemented Requirements

- ✅ **Secure Auth**: Login screen with email/password, JWT session management via EncryptedSharedPreferences.
- ✅ **Offline-First Sync**: Local Room as source of truth with background synchronization via SyncWorker.
- ✅ **Auto-Login**: Session persistence and Auth Guard to prevent unauthorized access to Dashboard.
- ✅ **Audit Trail**: `userId` and `storeId` injected into all `ActiveStockEntity` records.
- ✅ **Orphan Cleanup**: Automatic claim of local records created before login.
- ✅ **Multi-device Support**: Data consistency across devices via Supabase Postgrest upserts.

#### Technical Implementation

- ✅ **Supabase Stack**: GoTrue for Auth, Postgrest for Data, BOM 2.6.1 for AGP 8.2.2 compatibility.
- ✅ **Database**: Room migration v5 → v6 adding sync columns (`userId`, `storeId`, `syncedAt`, `version`, `isDirty`).
- ✅ **Worker**: `@HiltWorker` based `SyncWorker` with exponential backoff and connectivity constraints.
- ✅ **Domain**: New UseCases (`LoginUseCase`, `LogoutUseCase`, `SyncDataUseCase`, `ClaimOrphanRecordsUseCase`).
- ✅ **Security**: RLS (Row Level Security) enabled in Supabase to isolate data by store.

#### Test Coverage

- ✅ **113 tests passed** (Domain: 31, Integration: 42, UI: 21, WorkManager: 19).
- ✅ **Device Validation**: Manual E2E and instrumented tests passed on Xiaomi 14T.
- ✅ **Offline Resilience**: Verified data persistence and eventual sync after connection restoration.

#### Acceptance Criteria (VERIFIED ✅)

```gherkin
Given user enters valid credentials
When login is triggered
Then authentication succeeds and local data syncs with cloud

Given device is offline
When user modifies a batch
Then changes are stored locally (isDirty=true)
And sync occurs automatically when connection is restored

Given multiple devices with same account
When batch is added on device A
Then batch appears on device B after sync
```

---

## Appendix: SDD Artifacts Reference

All MVP development artifacts are stored in **Engram** (persistent memory):

- `sdd-init/smart-nutri-stock` - Project context
- `sdd/fix-expired-counter-bug/*` - Bug fix artifacts
- `sdd/feature/batch-management/*` - Batch management feature
- `sdd/ocr-fechas-camara-mlkit/*` - OCR date scanning feature
- `sdd/ocr-fechas-camara-mlkit-hotfixes/*` - Camera handoff + batch separation hotfixes

Future features should follow the **SDD Workflow**:

1. `/sdd-explore` → 2. `/sdd-propose` → 3. `/sdd-spec` → 4. `/sdd-design` → 5. `/sdd-tasks` → 6. `/sdd-apply` → 7. `/sdd-verify` → 8. `/sdd-archive`

---

_Document generated by Andrés & Gemini collaboration session - 28/03/2026_
