> **ARCHIVED**: See [FUTURE_ROADMAP.md](../../FUTURE_ROADMAP.md) for active development work.
>
> This file contains **v1.0 planning content** (679 lines) archived on 2026-05-08.
> All content preserved for historical reference.
>
> ---
>
> # BACKLOG: Smart Nutri-Stock v1.0

**Team**: DEC (Decathlon)
**Project**: Smart Nutri-Stock v1.0
**Hardware**: Samsung Galaxy XCover7 (Android 14+)
**Created**: 2026-03-19

---

## Milestones Overview

| Milestone | EPIC | Priority | Tickets | Effort |
|-----------|------|----------|----------|--------|
| M-02 | EPIC-02: Room Database | Critical | 3 | 21 pts |
| M-03 | EPIC-03: ML Kit Integration | Critical | 3 | 21 pts |
| M-04 | EPIC-04: Domain Layer TDD | Critical | 4 | 24 pts |
| M-05 | EPIC-05: Jetpack Compose UI | High | 4 | 21 pts |
| M-06 | EPIC-06: WorkManager | High | 2 | 13 pts |
| M-07 | EPIC-07: Testing Strategy | Medium | 3 | 16 pts |
| M-01 | EPIC-01: Init & Configuration | Low | 2 | 8 pts |
| M-08 | EPIC-08: CI/CD & Deployment | Low | 2 | 8 pts |

**Total**: 23 tickets | 132 story points

---

## M-02: EPIC-02 - Room Database (Critical)

### [CRITICAL] Create ProductCatalog Entity and DAO

**Description**: Implement Room entity for product catalog with EAN lookup optimization

**Gherkin Scenarios**:
```gherkin
Scenario: Lookup product by EAN in <500ms
  Given database contains product with EAN="8435489901234"
  When user queries by EAN="8435489901234"
  Then the product must be returned within 500ms
  And the result must contain name="Protein Bar Chocolate" and packSize=20

Scenario: Handle non-existent EAN
  Given database does not contain EAN="9999999999999"
  When user queries by EAN="9999999999999"
  Then the result must be null
  And the query must complete within 500ms
```

**Dependencies**: None
**Effort**: 5 pts

---

### [CRITICAL] Create ActiveStock Entity with Upsert Logic

**Description**: Implement Room entity for stock inventory with logical upsert to prevent duplicates

**Gherkin Scenarios**:
```gherkin
Scenario: Upsert existing batch aggregates quantity
  Given ActiveStock has entry EAN="123" expiryDate="2026-07-01" quantity=10
  When user scans same product with same expiry and quantity=5
  Then the system must update quantity to 15 (10+5)
  And must not create a duplicate record

Scenario: Insert new batch when EAN+expiryDate combination is new
  Given ActiveStock has no entry with EAN="456" expiryDate="2026-08-01"
  When user scans product with EAN="456" expiryDate="2026-08-01" quantity=20
  Then the system must create a new record with quantity=20

Scenario: Prevent duplicate batchId for same EAN+expiryDate
  Given ActiveStock has unique index on (ean, expiryDate)
  When attempting to insert duplicate EAN+expiryDate
  Then the database must throw SQLiteConstraintException
  And the transaction must roll back
```

**Dependencies**: M-02-Ticket-01
**Effort**: 8 pts

---

### [CRITICAL] Bulk CSV Import for Product Catalog

**Description**: Implement CSV import for initial product catalog load (2,600 products in <5 seconds)

**Gherkin Scenarios**:
```gherkin
Scenario: Import 2,600 products from CSV in <5 seconds
  Given a CSV file contains 2,600 product records
  When user imports the CSV file
  Then all 2,600 records must be inserted into ProductCatalog
  And the import must complete within 5 seconds

Scenario: Handle duplicate EAN during import
  Given database already contains EAN="8435489901234"
  When CSV import contains duplicate EAN="8435489901234"
  Then the existing record must be updated (REPLACE strategy)
  And no duplicate records must be created
```

**Dependencies**: M-02-Ticket-01
**Effort**: 8 pts

---

## M-03: EPIC-03 - ML Kit Integration (Critical)

### [CRITICAL] Implement Barcode Scanner with ML Kit

**Description**: Create barcode scanner using Google ML Kit Vision API with real-time detection

**Gherkin Scenarios**:
```gherkin
Scenario: Detect barcode in <2 seconds at 30cm distance
  Given camera is open and focused on barcode at 30cm distance
  When barcode is visible in frame with low light conditions
  Then the EAN must be detected within 2 seconds
  And visual feedback (green border) must appear around barcode

Scenario: Handle low-light conditions with LED flash
  Given lighting is below 100 lux (very dim)
  When barcode detection fails after 1 second
  Then the system must suggest enabling LED flash
  And detection must retry with flash enabled
```

**Dependencies**: None
**Effort**: 8 pts

---

### [CRITICAL] Implement OCR Date Extraction

**Description**: Create OCR date extractor using Google ML Kit Text Recognition for expiry dates

**Gherkin Scenarios**:
```gherkin
Scenario: Extract MM/YY format with >90% confidence
  Given image contains text "07/26" in expiry date field
  When OCR analysis completes
  Then the system must extract "07/26" with confidence >90%
  And the result must be returned as MM/YY string

Scenario: OCR failure fallback to manual entry
  Given OCR cannot detect date with confidence >90% after 3 attempts
  When OCR completes with low confidence
  Then the system must show manual date picker dialog
  And must default to current date + 6 months as suggestion
```

**Dependencies**: None
**Effort**: 8 pts

---

### [CRITICAL] Manual Entry Fallback UI

**Description**: Create manual entry form for barcode and date when hardware fails

**Gherkin Scenarios**:
```gherkin
Scenario: User manually enters EAN and expiry date
  Given barcode scanner fails or is unavailable
  When user clicks "Manual Entry" button
  Then the system must show form with EAN text field and date picker
  And manual entry must be always accessible from scanner screen

Scenario: Validate EAN format (13 digits)
  Given user enters invalid EAN with 12 digits
  When user submits the form
  Then the system must show error "EAN must be 13 digits"
  And the submission must be prevented
```

**Dependencies**: M-03-Ticket-01, M-03-Ticket-02
**Effort**: 5 pts

---

## M-04: EPIC-04 - Domain Layer TDD (Critical)

### [CRITICAL] ParseExpiryDateUseCase (TDD)

**Description**: Implement date parsing use case following TDD methodology (tests first)

**Gherkin Scenarios**:
```gherkin
Scenario: Parse "07/26" → 01/07/2026 (day 1, not day 31)
  Given a date parser is implemented following TDD
  When parsing "07/26"
  Then the result must be "2026-07-01" (1st day of July)
  And unit tests must be written BEFORE implementation

Scenario: Parse invalid format returns null
  Given input is "13/26" (invalid month)
  When parsing "13/26"
  Then the result must be null
  And the use case must handle error gracefully
```

**Dependencies**: None
**Effort**: 5 pts

---

### [CRITICAL] CalculateStatusUseCase (TDD)

**Description**: Implement semaphore status calculation use case with TDD

**Gherkin Scenarios**:
```gherkin
Scenario: Calculate GREEN status for expiry >90 days
  Given expiry date is 95 days from today
  When calculate status
  Then the result must be GREEN (>90d)

Scenario: Calculate YELLOW status for expiry ≤30 days and >15 days
  Given expiry date is 25 days from today
  When calculate status
  Then the result must be YELLOW (≤30d)

Scenario: Calculate RED status for expiry ≤15 days
  Given expiry date is 10 days from today
  When calculate status
  Then the result must be RED (≤15d)
```

**Dependencies**: M-04-Ticket-01
**Effort**: 5 pts

---

### [CRITICAL] UpsertStockUseCase (TDD)

**Description**: Implement upsert logic use case with TDD

**Gherkin Scenarios**:
```gherkin
Scenario: Upsert adds quantity to existing batch
  Given repository has batch with EAN="123" expiryDate="2026-07-01" quantity=10
  When upsert is called with EAN="123" expiryDate="2026-07-01" quantity=5
  Then the repository must update quantity to 15 (10+5)
  And no new record must be created

Scenario: Upsert creates new batch for unique combination
  Given repository has no batch with EAN="456" expiryDate="2026-08-01"
  When upsert is called with EAN="456" expiryDate="2026-08-01" quantity=20
  Then the repository must create new record with quantity=20
```

**Dependencies**: M-04-Ticket-02
**Effort**: 8 pts

---

### [CRITICAL] GetSemaphoreCountersUseCase (TDD)

**Description**: Implement semaphore counters query use case with TDD

**Gherkin Scenarios**:
```gherkin
Scenario: Get counters from repository
  Given database contains 5 GREEN, 3 YELLOW, 2 RED records
  When getSemaphoreCounters is called
  Then the result must be SemaphoreCounters(green=5, yellow=3, red=2)
  And the result must update reactively when database changes

Scenario: Handle empty database
  Given database contains no records
  When getSemaphoreCounters is called
  Then the result must be SemaphoreCounters(green=0, yellow=0, red=0)
```

**Dependencies**: M-04-Ticket-03
**Effort**: 6 pts

---

## M-05: EPIC-05 - Jetpack Compose UI (High)

### [HIGH] Scanner Screen with Camera Preview

**Description**: Create scanner Composable with camera preview, ML Kit integration, and confirmation dialog

**Gherkin Scenarios**:
```gherkin
Scenario: Complete scanner workflow in <10 seconds per product
  Given user opens scanner screen
  When user scans barcode, OCR date, and confirms quantity
  Then the entire workflow must complete in <10 seconds
  And the product must be saved to ActiveStock

Scenario: Show confirmation dialog after successful scan
  Given barcode and OCR date are detected
  When detection completes
  Then the system must show confirmation dialog with product name, expiry date, and quantity field
  And quantity must default to product packSize
```

**Dependencies**: M-03-Ticket-03, M-04-Ticket-03
**Effort**: 8 pts

---

### [HIGH] Dashboard with Semaphore Counters

**Description**: Create dashboard Composable with semáforo counters and stock list

**Gherkin Scenarios**:
```gherkin
Scenario: Dashboard loads with counters in <1 second
  Given user opens dashboard screen
  When dashboard loads
  Then semaphore counters must be displayed in <1 second
  And GREEN, YELLOW, RED counts must be prominently visible

Scenario: Tap GREEN counter filters list
  Given dashboard displays GREEN counter with value=5
  When user taps GREEN counter
  Then the stock list must show only GREEN records (expiry >90d)
```

**Dependencies**: M-04-Ticket-04
**Effort**: 5 pts

---

### [HIGH] History Screen with Search

**Description**: Create history Composable with searchable stock list and filters

**Gherkin Scenarios**:
```gherkin
Scenario: Search filters list in <500ms
  Given stock list contains 100+ records
  When user types search query "protein"
  Then the list must filter results in <500ms
  And only matching products must be displayed

Scenario: Apply status filter
  Given user selects RED status filter
  When filter is applied
  Then the list must show only RED records (expiry ≤15d)
```

**Dependencies**: M-05-Ticket-02
**Effort**: 5 pts

---

### [HIGH] One-Handed Layout Optimizations

**Description**: Optimize all screens for one-handed use on Samsung XCover7 (thumb zone at bottom 20%)

**Gherkin Scenarios**:
```gherkin
Scenario: Primary actions in thumb zone (bottom 20%)
  Given screen displays 6.3" XCover7 display
  Then primary action buttons must be in bottom 20% of screen
  And buttons must be at least 48dp × 48dp for easy thumb access

Scenario: High contrast colors for readability
  Given warehouse lighting is low (100-200 lux)
  When screen is displayed
  Then text must have high contrast ratio (>4.5:1)
  And semáforo colors must be easily distinguishable
```

**Dependencies**: M-05-Ticket-01, M-05-Ticket-02, M-05-Ticket-03
**Effort**: 3 pts

---

## M-06: EPIC-06 - WorkManager (High)

### [HIGH] Daily Semaphore Update Worker

**Description**: Create WorkManager worker for daily status recalculation at 06:00 AM

**Gherkin Scenarios**:
```gherkin
Scenario: Worker executes on schedule at 06:00 AM
  Given it is 06:00 AM and the device is on
  When the worker runs
  Then all ActiveStock records must be queried
  And semaphore status must be recalculated for each record
  And the update must complete within 10 seconds

Scenario: Worker does not run when battery is low
  Given battery level is <15%
  When 06:00 AM arrives
  Then the worker must be skipped (constraint: requiresBatteryNotLow)
  And status must be deferred until battery is >15%
```

**Dependencies**: M-04-Ticket-02
**Effort**: 8 pts

---

### [HIGH] Post-Boot Initialization

**Description**: Configure WorkManager to run worker 6 hours after device boot

**Gherkin Scenarios**:
```gherkin
Scenario: Worker runs 6 hours after device restart
  Given device boots up at 10:00 AM
  When 6 hours elapse
  Then the semaphore update worker must execute
  And all statuses must be recalculated

Scenario: Worker respects retry policy
  Given worker fails to update (e.g., database locked)
  When retry policy applies
  Then worker must retry with exponential backoff (10s → 20s → 40s)
  And maximum 3 retry attempts must be allowed
```

**Dependencies**: M-06-Ticket-01
**Effort**: 5 pts

---

## M-07: EPIC-07 - Testing Strategy (Medium)

### [MEDIUM] Unit Tests for Domain Layer (TDD)

**Description**: Write comprehensive unit tests for all use cases (≥90% coverage)

**Gherkin Scenarios**:
```gherkin
Scenario: ParseExpiryDateUseCase passes all edge cases
  Given unit tests cover valid MM/YY formats (01/01 - 12/99)
  When tests run
  Then all tests must pass
  And code coverage must be ≥90%

Scenario: CalculateStatusUseCase passes boundary tests
  Given unit tests cover exact thresholds (90d, 30d, 15d)
  When tests run
  Then boundary conditions must be correct
  And GREEN status must be >90d (not >=90d)
```

**Dependencies**: M-04-Ticket-01, M-04-Ticket-02, M-04-Ticket-03, M-04-Ticket-04
**Effort**: 8 pts

---

### [MEDIUM] Room Integration Tests

**Description**: Write integration tests for DAOs using in-memory database

**Gherkin Scenarios**:
```gherkin
Scenario: ActiveStockDao upsert test passes
  Given in-memory database is created
  When upsertStock is called with existing batch
  Then quantity must be aggregated correctly
  And no duplicate records must exist

Scenario: SemaphoreCounters query returns correct values
  Given database contains mixed statuses
  When getSemaphoreCounters is called
  Then result must match actual COUNT(*) queries
```

**Dependencies**: M-02-Ticket-01, M-02-Ticket-02
**Effort**: 5 pts

---

### [MEDIUM] UI Tests with Compose Testing

**Description**: Write UI tests for critical flows (scanner, dashboard)

**Gherkin Scenarios**:
```gherkin
Scenario: Scanner screen test passes
  Given Compose test rule is set up
  When user clicks "Scan" button
  Then confirmation dialog must appear
  And product details must be displayed correctly

Scenario: Dashboard screen test passes
  Given Compose test rule is set up
  When dashboard loads
  Then semaphore counters must be displayed
  And counters must show correct values
```

**Dependencies**: M-05-Ticket-01, M-05-Ticket-02
**Effort**: 3 pts

---

## M-01: EPIC-01 - Init & Configuration (Low)

### [LOW] Initialize Android Project with Kotlin + Compose

**Description**: Set up project structure with Gradle, Kotlin, Jetpack Compose, and Hilt

**Gherkin Scenarios**:
```gherkin
Scenario: Project builds successfully
  Given all dependencies are configured in build.gradle.kts
  When Gradle build runs
  Then build must succeed without errors
  And APK size must be ≤15MB

Scenario: Hilt injection works
  Given Hilt application class is configured
  When app launches
  Then Hilt must initialize without errors
  And dependencies must be injectable
```

**Dependencies**: None
**Effort**: 5 pts

---

### [LOW] Configure APK Build for Direct Installation

**Description**: Set up Gradle tasks for signing APK and version management

**Gherkin Scenarios**:
```gherkin
Scenario: APK builds with correct signing
  Given signing configuration is set up
  When assembleRelease task runs
  Then APK must be signed with correct certificate
  And APK must be installable on XCover7

Scenario: Version code increments correctly
  Given versionName="1.0.0" and versionCode=1
  When version is incremented
  Then versionName must be "1.0.1" and versionCode must be 2
```

**Dependencies**: M-01-Ticket-01
**Effort**: 3 pts

---

## M-08: EPIC-08 - CI/CD & Deployment (Low)

### [LOW] GitHub Actions Build Pipeline

**Description**: Create GitHub Actions workflow for automated builds and tests

**Gherkin Scenarios**:
```gherkin
Scenario: Pipeline runs on push to main
  Given push event triggers workflow
  When workflow runs
  Then all unit tests must execute
  And build must succeed in ≤5 minutes

Scenario: Pipeline runs on pull request
  Given pull request is opened
  When workflow runs
  Then all tests must execute
  And build must pass before merge is allowed
```

**Dependencies**: M-01-Ticket-02, M-07-Ticket-01, M-07-Ticket-02, M-07-Ticket-03
**Effort**: 5 pts

---

### [LOW] Direct APK Deployment Flow

**Description**: Document and automate APK installation process for XCover7 devices

**Gherkin Scenarios**:
```gherkin
Scenario: APK installs on XCover7 without errors
  Given APK is downloaded to XCover7 device
  When user taps APK file
  Then installation must succeed
  And app must launch successfully

Scenario: Version update overwrites previous version
  Given app version 1.0.0 is installed
  When APK version 1.0.1 is installed
  Then data must be preserved (Room database migration)
  And app must launch with new version
```

**Dependencies**: M-08-Ticket-01
**Effort**: 3 pts

---

## Dependency Graph

```
M-02-Ticket-01 (ProductCatalog Entity)
  ├── M-02-Ticket-02 (ActiveStock Entity)
  ├── M-02-Ticket-03 (CSV Import)
  └── M-04-Ticket-03 (UpsertStockUseCase)
      ├── M-04-Ticket-04 (GetSemaphoreCountersUseCase)
      │   └── M-05-Ticket-02 (Dashboard)
      │       └── M-05-Ticket-03 (History)
      └── M-05-Ticket-01 (Scanner)

M-03-Ticket-01 (Barcode Scanner)
M-03-Ticket-02 (OCR Date Extraction)
  └── M-03-Ticket-03 (Manual Entry UI)
      └── M-05-Ticket-01 (Scanner)

M-04-Ticket-01 (ParseExpiryDateUseCase)
  └── M-04-Ticket-02 (CalculateStatusUseCase)
      ├── M-04-Ticket-03 (UpsertStockUseCase)
      └── M-06-Ticket-01 (Daily Worker)
          └── M-06-Ticket-02 (Post-Boot Init)

M-05-Ticket-01, M-05-Ticket-02, M-05-Ticket-03 (UI Screens)
  └── M-05-Ticket-04 (One-Handed Layout)

M-02-Ticket-01, M-02-Ticket-02 (Room Entities)
  └── M-07-Ticket-02 (Room Integration Tests)

M-05-Ticket-01, M-05-Ticket-02 (Scanner, Dashboard)
  └── M-07-Ticket-03 (UI Tests)

M-01-Ticket-01 (Project Init)
  └── M-01-Ticket-02 (APK Build)

M-01-Ticket-02, M-07-Ticket-01, M-07-Ticket-02, M-07-Ticket-03
  └── M-08-Ticket-01 (GitHub Actions)
      └── M-08-Ticket-02 (APK Deployment)
```

---

## Execution Roadmap

### Sprint 1 (Week 1-2): Critical Path Foundation
- M-02-Ticket-01, M-02-Ticket-02, M-02-Ticket-03 (Room)
- M-04-Ticket-01, M-04-Ticket-02, M-04-Ticket-03, M-04-Ticket-04 (Domain TDD)

### Sprint 2 (Week 3-4): Core Functionality
- M-03-Ticket-01, M-03-Ticket-02, M-03-Ticket-03 (ML Kit)
- M-05-Ticket-01, M-05-Ticket-02 (Scanner, Dashboard)
- M-06-Ticket-01, M-06-Ticket-02 (WorkManager)

### Sprint 3 (Week 5): UI Polish & Testing
- M-05-Ticket-03, M-05-Ticket-04 (History, One-Handed Layout)
- M-07-Ticket-01, M-07-Ticket-02, M-07-Ticket-03 (Tests)

### Sprint 4 (Week 6): CI/CD & Deployment
- M-01-Ticket-01, M-01-Ticket-02 (Init & Build)
- M-08-Ticket-01, M-08-Ticket-02 (CI/CD & Deployment)

---

## Priority Legend

- **[CRITICAL]**: Blocks core business value. Must be done first.
- **[HIGH]**: Important for functionality. Should be done early.
- **[MEDIUM]**: Improves quality and reliability. Can be parallelized.
- **[LOW]**: Infrastructure and tooling. Can be deferred.

---

## Story Point Estimation

- **1-2 pts**: Very simple (few hours)
- **3-5 pts**: Simple (1-2 days)
- **6-8 pts**: Moderate (2-3 days)
- **13+ pts**: Complex (3-5 days)

---

**Status**: ✅ Ready for Linear import
**Last Updated**: 2026-03-19
