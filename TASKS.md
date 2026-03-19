# TASKS: Smart Nutri-Stock v1.0 - Registro Dinámico de Productos

**Team**: DEC (Decathlon)
**Project**: Smart Nutri-Stock v1.0
**Hardware**: Samsung Galaxy XCover7 (Android 14+)
**Change**: implement-dynamic-registration-and-fix-tasks
**Created**: 2026-03-19

---

## EPIC-01: Project Setup & Architecture Foundation

### Task M-01-Ticket-01: Initialize Android Project Structure

**Description**: Set up project structure with Kotlin, Jetpack Compose, Hilt, and Room dependencies. Configure build.gradle.kts files.

**Acceptance Criteria**:
- [ ] Project builds successfully with Kotlin 1.9.20+
- [ ] Jetpack Compose BOM configured
- [ ] Room dependencies added (runtime, compiler, ktx)
- [ ] Hilt dependencies added (hilt-android, hilt-compiler)
- [ ] ML Kit dependencies added (barcode-scanning, text-recognition)
- [ ] WorkManager runtime added
- [ ] AndroidManifest.xml with CAMERA permission
- [ ] SmartNutriStockApp.kt created as Hilt Application class

**Subtasks**:
- [ ] Create project-level build.gradle.kts
- [ ] Create settings.gradle.kts
- [ ] Create app/build.gradle.kts with all dependencies
- [ ] Configure Hilt and KSP plugins
- [ ] Create AndroidManifest.xml with required permissions
- [ ] Create SmartNutriStockApp.kt application class

**Priority**: Medium
**Layer**: Infrastructure
**Estimated Hours**: 6

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
```

---

### Task M-01-Ticket-02: Configure APK Build and Signing

**Description**: Set up APK build configuration with signing, version management, and ProGuard/R8 rules for ML Kit.

**Acceptance Criteria**:
- [ ] Release keystore generated and configured
- [ ] Signing configuration in build.gradle.kts
- [ ] VersionName and VersionCode set up
- [ ] ProGuard/R8 rules for ML Kit configured
- [ ] APK output directory configured
- [ ] Build variants (debug, release) configured
- [ ] minSdk 34, targetSdk 34, compileSdk 34

**Subtasks**:
- [ ] Generate release keystore
- [ ] Configure signing in build.gradle.kts
- [ ] Set up version management
- [ ] Create ProGuard rules for ML Kit
- [ ] Configure build variants

**Priority**: Medium
**Layer**: Infrastructure
**Estimated Hours**: 4

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

---

## EPIC-02: Data Layer - Dynamic Product Registration

### Task M-02-Ticket-01: Create ProductCatalogEntity with @Index

**Description**: Implement Room entity for product catalog with @Index on EAN for optimized lookups.

**Acceptance Criteria**:
- [ ] ProductCatalogEntity with @Entity annotation
- [ ] @PrimaryKey for ean field
- [ ] name (String) field
- [ ] packSize (Int, default=1) field
- [ ] @Index(value = ["ean"]) for O(log n) lookups
- [ ] AppDatabase updated with ProductCatalogEntity

**Subtasks**:
- [ ] Create ProductCatalogEntity.kt
- [ ] Add @Index on ean field
- [ ] Update AppDatabase.kt to include ProductCatalogEntity
- [ ] Set database version to 1

**Priority**: High
**Layer**: Data
**Estimated Hours**: 3

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

---

### Task M-02-Ticket-02: Create ProductCatalogDao

**Description**: Implement DAO with findByEan() and insertOrReplace() methods for dynamic registration.

**Acceptance Criteria**:
- [ ] findByEan(ean: String): ProductCatalogEntity? method
- [ ] insertOrReplace(product: ProductCatalogEntity) with REPLACE strategy
- [ ] getAll(): Flow<List<ProductCatalogEntity>> method
- [ ] Integration tests for findByEan()
- [ ] Integration tests for insertOrReplace()

**Subtasks**:
- [ ] Create ProductCatalogDao.kt interface
- [ ] Implement findByEan() with @Query
- [ ] Implement insertOrReplace() with @Insert(onConflict = REPLACE)
- [ ] Implement getAll() with @Query
- [ ] Write integration tests for findByEan()
- [ ] Write integration tests for insertOrReplace()

**Priority**: High
**Layer**: Data
**Estimated Hours**: 5

**Gherkin Scenarios**:
```gherkin
Scenario: findByEan returns product when exists
  Given database contains product with EAN="8435489901234"
  When findByEan is called with "8435489901234"
  Then the product must be returned
  And name must be "Protein Bar Chocolate"

Scenario: insertOrReplace handles duplicate EAN
  Given database contains EAN="8435489901234"
  When insertOrReplace is called with same EAN but different name
  Then the existing record must be updated
  And no duplicate records must be created
```

---

### Task M-02-Ticket-03: Create ProductRepository Interface

**Description**: Define ProductRepository interface for dynamic registration with registerProduct() method.

**Acceptance Criteria**:
- [ ] ProductRepository interface created
- [ ] findByEan(ean: String): Product? method
- [ ] registerProduct(product: Product): RegisterResult method
- [ ] RegisterResult sealed class with Success and Failure types
- [ ] Failure types: InvalidEan, InvalidName, InvalidPackSize, DuplicateProduct, DatabaseError

**Subtasks**:
- [ ] Create ProductRepository.kt interface
- [ ] Create RegisterResult sealed class
- [ ] Define findByEan() method signature
- [ ] Define registerProduct() method signature
- [ ] Create Failure types for all error scenarios

**Priority**: High
**Layer**: Data
**Estimated Hours**: 3

**Gherkin Scenarios**:
```gherkin
Scenario: ProductRepository interface is defined
  Given ProductRepository interface is created
  When interface is inspected
  Then it must have findByEan() method
  And it must have registerProduct() method
  And it must return RegisterResult sealed class
```

---

### Task M-02-Ticket-04: Create ProductRepositoryImpl

**Description**: Implement ProductRepository with Room DAO integration and RegisterResult handling.

**Acceptance Criteria**:
- [ ] ProductRepositoryImpl injects ProductCatalogDao
- [ ] findByEan() maps Entity to Domain Model
- [ ] registerProduct() handles validation errors
- [ ] registerProduct() calls dao.insertOrReplace()
- [ ] Returns RegisterResult.Success on success
- [ ] Returns RegisterResult.Failure on error

**Subtasks**:
- [ ] Create ProductRepositoryImpl.kt
- [ ] Inject ProductCatalogDao in constructor
- [ ] Implement findByEan() method
- [ ] Implement registerProduct() method
- [ ] Handle DatabaseError exceptions
- [ ] Map Entity to Domain Model
- [ ] Write unit tests for repository

**Priority**: High
**Layer**: Data
**Estimated Hours**: 6

**Gherkin Scenarios**:
```gherkin
Scenario: registerProduct saves successfully
  Given repository is initialized with DAO
  When registerProduct is called with valid product
  Then product must be saved to database
  And RegisterResult.Success must be returned

Scenario: registerProduct handles database error
  Given database is locked or unavailable
  When registerProduct is called
  Then RegisterResult.Failure.DatabaseError must be returned
  And product must not be saved
```

---

### Task M-02-Ticket-05: Create Hilt Modules (Database and UseCase)

**Description**: Configure Hilt modules for production and test environments with database and use case injection.

**Acceptance Criteria**:
- [ ] DatabaseModule.kt for production Room database
- [ ] FakeDatabaseModule.kt for test in-memory database
- [ ] UseCaseModule.kt for providing use cases
- [ ] @TestInstallIn for test module replacement
- [ ] provideProductRepository() in DatabaseModule
- [ ] provideRegisterProductUseCase() in UseCaseModule

**Subtasks**:
- [ ] Create DatabaseModule.kt with @InstallIn(SingletonComponent)
- [ ] Create FakeDatabaseModule.kt with @TestInstallIn
- [ ] Create UseCaseModule.kt with @InstallIn(SingletonComponent)
- [ ] Implement provideDatabase() for production
- [ ] Implement provideFakeDatabase() for tests
- [ ] Implement provideProductDao()
- [ ] Implement provideProductRepository()
- [ ] Implement provideRegisterProductUseCase()

**Priority**: High
**Layer**: Data
**Estimated Hours**: 4

**Gherkin Scenarios**:
```gherkin
Scenario: Hilt provides production database
  Given app is running in production mode
  When DatabaseModule provides database
  Then Room database must be created on disk
  And DAOs must be injectable

Scenario: Hilt provides test database
  Given test is running with Hilt
  When FakeDatabaseModule is installed
  Then in-memory database must be created
  And main thread queries must be allowed
```

---

## EPIC-03: Domain Layer - RegisterProductUseCase (TDD)

### Task M-03-Ticket-01: Implement RegisterProductUseCase (TDD)

**Description**: Create a use case in the Domain layer that handles dynamic product registration with full validation logic. TDD approach: Write tests BEFORE implementation.

**Acceptance Criteria**:
- [ ] Validates EAN format (13 digits)
- [ ] Validates product name length (3-100 characters)
- [ ] Validates packSize (positive integer > 0)
- [ ] Checks for duplicate EAN before registration
- [ ] Returns RegisterResult with appropriate Success/Failure types
- [ ] All error messages are in Spanish
- [ ] **UNIT TESTING (MANDATORY)**: ≥90% coverage for this use case

**Subtasks**:
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for valid EAN scenario
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for invalid EAN (not 13 digits)
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for short name (< 3 chars)
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for invalid packSize (<= 0)
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for duplicate EAN
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for boundary conditions (3 chars, 100 chars)
- [ ] Create RegisterProductUseCase.kt in domain/usecase
- [ ] Implement validateEanFormat() method
- [ ] Implement validateNameLength() method
- [ ] Implement validatePackSize() method
- [ ] Implement checkDuplicateEan() method
- [ ] Implement invoke() operator with Result pattern
- [ ] Ensure all tests pass with ≥90% coverage

**Priority**: High
**Layer**: Domain
**Estimated Hours**: 8 (4 testing + 4 implementation)

**Gherkin Scenarios**:
```gherkin
Scenario: Registro exitoso con datos válidos
  Given el operador escanea un EAN válido de 13 dígitos
  And el operador ingresa "Arroz Blanco La Campaña" como nombre
  And el operador ingresa "1000" como pack size
  When el sistema valida los datos
  Then el producto se guarda exitosamente
  And se retorna RegisterResult.Success con el producto

Scenario: Error por EAN inválido
  Given el operador escanea un código con 12 dígitos
  When el sistema valida el formato del EAN
  Then se retorna RegisterResult.Failure.InvalidEan
  And el mensaje de error es "El código debe tener 13 dígitos"

Scenario: Error por nombre muy corto
  Given el operador ingresa nombre "AB"
  When el sistema valida el nombre
  Then se retorna RegisterResult.Failure.InvalidName
  And el mensaje de error es "El nombre debe tener al menos 3 caracteres"

Scenario: Error por packSize negativo
  Given el operador ingresa packSize "-5"
  When el sistema valida el packSize
  Then se retorna RegisterResult.Failure.InvalidPackSize
  And el mensaje de error es "El tamaño del pack debe ser positivo"

Scenario: Error por EAN duplicado
  Given ya existe un producto con EAN "8435489901234"
  When el operador intenta registrar el mismo EAN
  Then se retorna RegisterResult.Failure.DuplicateProduct
  And el mensaje incluye el nombre del producto existente
```

---

### Task M-03-Ticket-02: Create Product Domain Model

**Description**: Define Product domain model and ValidationError sealed class for use case results.

**Acceptance Criteria**:
- [ ] Product data class with ean, name, packSize
- [ ] RegisterProductRequest data class with userId
- [ ] ValidationError sealed class with all error types
- [ ] Error messages in Spanish
- [ ] RegisterResult sealed class with Success and Failure

**Subtasks**:
- [ ] Create Product.kt domain model
- [ ] Create RegisterProductRequest.kt data class
- [ ] Create ValidationError.kt sealed class
- [ ] Create RegisterResult.kt sealed class

**Priority**: High
**Layer**: Domain
**Estimated Hours**: 2

**Gherkin Scenarios**:
```gherkin
Scenario: Product domain model is defined
  Given Product data class is created
  When model is inspected
  Then it must have ean field (String)
  And it must have name field (String)
  And it must have packSize field (Int)
```

---

## EPIC-04: Domain Layer - Additional Use Cases (TDD)

### Task M-04-Ticket-01: Implement ParseExpiryDateUseCase (TDD)

**Description**: Implement date parsing use case following TDD methodology (tests first). Parse MM/YY format to day 1 of that month.

**Acceptance Criteria**:
- [ ] Parses "07/26" → "2026-07-01" (day 1, not day 31)
- [ ] Returns null for invalid formats
- [ ] Handles month boundaries (01/01 - 12/99)
- [ ] **UNIT TESTING (MANDATORY)**: ≥90% coverage

**Subtasks**:
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for valid MM/YY formats
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for invalid month (13/26)
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for boundary cases
- [ ] Create ParseExpiryDateUseCase.kt
- [ ] Implement date parsing logic
- [ ] Ensure all tests pass

**Priority**: High
**Layer**: Domain
**Estimated Hours**: 5 (2 testing + 3 implementation)

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

---

### Task M-04-Ticket-02: Implement CalculateStatusUseCase (TDD)

**Description**: Implement semaphore status calculation use case with TDD. Calculate GREEN (>90d), YELLOW (≤30d), RED (≤15d).

**Acceptance Criteria**:
- [ ] Returns GREEN for expiry >90 days
- [ ] Returns YELLOW for expiry ≤30 days and >15 days
- [ ] Returns RED for expiry ≤15 days
- [ ] **UNIT TESTING (MANDATORY)**: ≥90% coverage

**Subtasks**:
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for GREEN status (>90d)
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for YELLOW status (≤30d)
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for RED status (≤15d)
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for boundary conditions
- [ ] Create CalculateStatusUseCase.kt
- [ ] Implement status calculation logic
- [ ] Ensure all tests pass

**Priority**: High
**Layer**: Domain
**Estimated Hours**: 5 (2 testing + 3 implementation)

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

---

### Task M-04-Ticket-03: Implement UpsertStockUseCase (TDD)

**Description**: Implement upsert logic use case with TDD. Aggregate quantity for existing batch, create new batch for unique EAN+expiryDate combination.

**Acceptance Criteria**:
- [ ] Upsert adds quantity to existing batch
- [ ] Upsert creates new batch for unique combination
- [ ] Prevents duplicate records
- [ ] **UNIT TESTING (MANDATORY)**: ≥90% coverage

**Subtasks**:
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for upsert aggregating quantity
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for upsert creating new batch
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for duplicate prevention
- [ ] Create UpsertStockUseCase.kt
- [ ] Implement upsert logic
- [ ] Ensure all tests pass

**Priority**: High
**Layer**: Domain
**Estimated Hours**: 8 (4 testing + 4 implementation)

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

---

### Task M-04-Ticket-04: Implement GetSemaphoreCountersUseCase (TDD)

**Description**: Implement semaphore counters query use case with TDD. Return GREEN, YELLOW, RED counts.

**Acceptance Criteria**:
- [ ] Returns correct counts for each status
- [ ] Updates reactively when database changes
- [ ] Handles empty database
- [ ] **UNIT TESTING (MANDATORY)**: ≥90% coverage

**Subtasks**:
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for correct counts
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for empty database
- [ ] **UNIT TESTING (MANDATORY)**: Write unit tests for reactive updates
- [ ] Create GetSemaphoreCountersUseCase.kt
- [ ] Implement counters query logic
- [ ] Ensure all tests pass

**Priority**: High
**Layer**: Domain
**Estimated Hours**: 6 (3 testing + 3 implementation)

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

---

## EPIC-05: Presentation Layer - Scanner & Registration UI

### Task M-05-Ticket-01: Implement ScannerViewModel State Management

**Description**: Update ScannerViewModel with ProductNotFound state and dynamic registration flow integration.

**Acceptance Criteria**:
- [ ] ScannerState sealed class with ProductNotFound state
- [ ] onBarcodeScanned(ean) method checks if product exists
- [ ] onRegisterProduct(ean, name, packSize) method
- [ ] onCancelRegistration() method
- [ ] State transitions: Scanning → ProductFound → ProductNotFound → RegisteringProduct → ProductFound

**Subtasks**:
- [ ] Create ScannerState sealed class
- [ ] Add ProductNotFound state with ean field
- [ ] Add RegisteringProduct state
- [ ] Implement onBarcodeScanned() method
- [ ] Implement onRegisterProduct() method
- [ ] Implement onCancelRegistration() method
- [ ] Inject RegisterProductUseCase
- [ ] Write unit tests for state transitions

**Priority**: High
**Layer**: Presentation
**Estimated Hours**: 6

**Gherkin Scenarios**:
```gherkin
Scenario: ProductNotFound state triggers when EAN not found
  Given user scans EAN that doesn't exist
  When onBarcodeScanned is called
  Then state must be ProductNotFound(ean)
  And Bottom Sheet must be shown

Scenario: Registration success transitions to ProductFound
  Given user registers new product with valid data
  When onRegisterProduct completes
  Then state must be ProductFound(product)
  And flow must advance to date/qty capture
```

---

### Task M-05-Ticket-02: Implement ProductRegistrationBottomSheet

**Description**: Create thumb zone Composable (56dp tap targets) with auto-focus, validation, and one-handed layout.

**Acceptance Criteria**:
- [ ] Bottom Sheet with ModalBottomSheetLayout
- [ ] EAN field (read-only, shows scanned value)
- [ ] Name field (auto-focus, 3-100 chars validation)
- [ ] PackSize field (numeric keyboard, >0 validation)
- [ ] Cancel button (48dp, secondary color)
- [ ] Save button (48dp, primary color, enabled/disabled state)
- [ ] Validation error messages in Spanish
- [ ] imePadding() for keyboard handling
- [ ] Buttons in bottom 20% (thumb zone)
- [ ] Renders in <200ms

**Subtasks**:
- [ ] Create ProductRegistrationBottomSheet.kt
- [ ] Implement ModalBottomSheetLayout with imePadding()
- [ ] Add EAN read-only field
- [ ] Add Name field with auto-focus
- [ ] Add PackSize field with numeric keyboard
- [ ] Implement validation logic (name length, packSize)
- [ ] Add error messages in Spanish
- [ ] Add Cancel and Save buttons (48dp minimum)
- [ ] Implement one-handed layout (bottom 20%)
- [ ] Write UI tests for auto-focus
- [ ] Write UI tests for validation
- [ ] Write UI tests for tap targets (48dp+)

**Priority**: High
**Layer**: Presentation
**Estimated Hours**: 8 (4 implementation + 4 testing)

**Gherkin Scenarios**:
```gherkin
Scenario: Validación en tiempo real del nombre
  Given el usuario escribe "AB" en el campo Nombre
  When el campo pierde el foco
  Then el sistema debe mostrar mensaje de ayuda "Mínimo 3 caracteres"
  And el botón Guardar debe estar deshabilitado
  When el usuario escribe "Barra de Proteína"
  Then el mensaje de ayuda debe desaparecer
  And el botón Guardar debe estar habilitado

Scenario: Botones en zona del pulgar
  Given pantalla XCover7 de 690dp de altura
  When Bottom Sheet se renderiza
  Then botones deben estar en bottom 20% (~138dp)
  And botones deben ser al menos 48dp × 48dp
```

---

### Task M-05-Ticket-03: Integrate ProductRegistrationBottomSheet with ScannerScreen

**Description**: Update ScannerScreen to handle ProductNotFound state and show ProductRegistrationBottomSheet.

**Acceptance Criteria**:
- [ ] ScannerScreen observes ScannerViewModel state
- [ ] Shows ProductRegistrationBottomSheet when state is ProductNotFound
- [ ] Passes EAN, onRegister, onCancel callbacks
- [ ] Pauses camera when Bottom Sheet is open
- [ ] Resumes camera after registration or cancel

**Subtasks**:
- [ ] Update ScannerScreen.kt to handle ProductNotFound state
- [ ] Add ProductRegistrationBottomSheet integration
- [ ] Implement camera pause/resume logic
- [ ] Pass callbacks for registration
- [ ] Write UI tests for state handling

**Priority**: High
**Layer**: Presentation
**Estimated Hours**: 4

**Gherkin Scenarios**:
```gherkin
Scenario: Bottom Sheet se muestra cuando producto no existe
  Given usuario escanea EAN que no existe
  When ScannerViewModel cambia a ProductNotFound
  Then ProductRegistrationBottomSheet debe aparecer
  And cámara debe pausarse
  And EAN debe estar pre-cargado en el campo

Scenario: Cámara se reanuda después de registro
  Given Bottom Sheet está abierto
  When usuario toca Guardar y registro es exitoso
  Then Bottom Sheet debe cerrarse
  And cámara debe reanudarse
  And flujo debe avanzar a captura de fecha/cantidad
```

---

### Task M-05-Ticket-04: Implement Dashboard with Semaphore Counters

**Description**: Create dashboard Composable with semáforo counters and stock list.

**Acceptance Criteria**:
- [ ] Dashboard loads with counters in <1 second
- [ ] GREEN, YELLOW, RED counts prominently displayed
- [ ] Tap on counter filters list
- [ ] Stock list shows products for selected status
- [ ] One-handed layout optimization

**Subtasks**:
- [ ] Create DashboardViewModel.kt
- [ ] Create DashboardScreen.kt
- [ ] Implement semaphore counters display
- [ ] Implement stock list filtering
- [ ] Add tap handlers for counters
- [ ] Write UI tests for dashboard
- [ ] Write UI tests for filtering

**Priority**: High
**Layer**: Presentation
**Estimated Hours**: 5

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

---

### Task M-05-Ticket-05: Implement History Screen with Search

**Description**: Create history Composable with searchable stock list and filters.

**Acceptance Criteria**:
- [ ] Search filters list in <500ms
- [ ] Status filter (GREEN, YELLOW, RED)
- [ ] Sort by expiry date, quantity
- [ ] One-handed layout optimization

**Subtasks**:
- [ ] Create HistoryViewModel.kt
- [ ] Create HistoryScreen.kt
- [ ] Implement search functionality
- [ ] Implement status filter
- [ ] Implement sort functionality
- [ ] Write UI tests for search
- [ ] Write UI tests for filtering

**Priority**: High
**Layer**: Presentation
**Estimated Hours**: 5

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

---

### Task M-05-Ticket-06: Implement One-Handed Layout Optimizations

**Description**: Optimize all screens for one-handed use on Samsung XCover7 (thumb zone at bottom 20%).

**Acceptance Criteria**:
- [ ] Primary actions in bottom 20% of screen
- [ ] Buttons at least 48dp × 48dp
- [ ] High contrast colors (>4.5:1)
- [ ] Semáforo colors easily distinguishable
- [ ] Tested on XCover7 device

**Subtasks**:
- [ ] Review all screens for thumb zone compliance
- [ ] Update button sizes to minimum 48dp
- [ ] Verify color contrast ratios
- [ ] Test on XCover7 device
- [ ] Write accessibility tests

**Priority**: Medium
**Layer**: Presentation
**Estimated Hours**: 3

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

---

## EPIC-06: WorkManager - Daily Semaphore Updates

### Task M-06-Ticket-01: Implement Daily Semaphore Update Worker

**Description**: Create WorkManager worker for daily status recalculation at 06:00 AM.

**Acceptance Criteria**:
- [ ] Worker executes on schedule at 06:00 AM
- [ ] Queries all ActiveStock records
- [ ] Recalculates semaphore status for each record
- [ ] Updates status in database
- [ ] Completes within 10 seconds
- [ ] Skips when battery is low (<15%)

**Subtasks**:
- [ ] Create SemaphoreUpdateWorker.kt
- [ ] Configure WorkManager constraints (battery not low, network not required)
- [ ] Implement status recalculation logic
- [ ] Configure periodic work (daily at 06:00 AM)
- [ ] Write unit tests for worker
- [ ] Write integration tests

**Priority**: High
**Layer**: Data
**Estimated Hours**: 8

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

---

### Task M-06-Ticket-02: Implement Post-Boot Initialization

**Description**: Configure WorkManager to run worker 6 hours after device boot.

**Acceptance Criteria**:
- [ ] Worker runs 6 hours after device restart
- [ ] All statuses recalculated after boot
- [ ] Retry policy with exponential backoff (10s → 20s → 40s)
- [ ] Maximum 3 retry attempts

**Subtasks**:
- [ ] Configure post-boot work request
- [ ] Set delay to 6 hours
- [ ] Implement retry policy
- [ ] Write unit tests
- [ ] Test boot scenario

**Priority**: High
**Layer**: Data
**Estimated Hours**: 5

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

---

## EPIC-07: Testing Strategy - Comprehensive Testing

### Task M-07-Ticket-01: Unit Tests for Domain Layer (TDD)

**Description**: Write comprehensive unit tests for all use cases (≥90% coverage).

**Acceptance Criteria**:
- [ ] RegisterProductUseCase: 5+ test scenarios
- [ ] ParseExpiryDateUseCase: 3+ test scenarios
- [ ] CalculateStatusUseCase: 5+ test scenarios
- [ ] UpsertStockUseCase: 3+ test scenarios
- [ ] GetSemaphoreCountersUseCase: 3+ test scenarios
- [ ] Overall coverage ≥90%

**Subtasks**:
- [ ] Write tests for RegisterProductUseCase (valid registration, invalid EAN, short name, negative packSize, duplicate EAN)
- [ ] Write tests for ParseExpiryDateUseCase (valid formats, invalid formats, boundaries)
- [ ] Write tests for CalculateStatusUseCase (GREEN, YELLOW, RED, boundaries)
- [ ] Write tests for UpsertStockUseCase (aggregate quantity, create new, prevent duplicates)
- [ ] Write tests for GetSemaphoreCountersUseCase (correct counts, empty database, reactive updates)
- [ ] Verify coverage ≥90%

**Priority**: High
**Layer**: Testing
**Estimated Hours**: 8

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

---

### Task M-07-Ticket-02: Room Integration Tests

**Description**: Write integration tests for DAOs using in-memory database.

**Acceptance Criteria**:
- [ ] ProductCatalogDao: findByEan(), insertOrReplace()
- [ ] ActiveStockDao: upsertStock(), getByStatus(), getSemaphoreCounters(), updateStatus()
- [ ] DateConverter: fromTimestamp(), toTimestamp()
- [ ] All tests use in-memory database
- [ ] Tests run in <5 seconds

**Subtasks**:
- [ ] Write tests for ProductCatalogDao (findByEan returns product, findByEan returns null, insertOrReplace replaces, insertOrReplace inserts)
- [ ] Write tests for ActiveStockDao (upsert aggregates quantity, upsert creates new, getByStatus filters, getSemaphoreCounters returns counts, updateStatus updates)
- [ ] Write tests for DateConverter (fromTimestamp, toTimestamp)
- [ ] Configure Hilt for test database
- [ ] Verify all tests pass

**Priority**: Medium
**Layer**: Testing
**Estimated Hours**: 5

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

---

### Task M-07-Ticket-03: UI Tests with Compose Testing

**Description**: Write UI tests for critical flows (scanner, dashboard, product registration).

**Acceptance Criteria**:
- [ ] ProductRegistrationBottomSheet: renders correctly, auto-focus, validation, tap targets (48dp+)
- [ ] ScannerScreen: handles ProductNotFound state, shows Bottom Sheet
- [ ] DashboardScreen: displays counters, filters list
- [ ] HistoryScreen: search, filter, sort functionality
- [ ] All UI tests pass

**Subtasks**:
- [ ] Write tests for ProductRegistrationBottomSheet (renders, ean readonly, name autofocus, save button enabled/disabled, name error shown, packSize error shown, tap targets size, thumb zone layout)
- [ ] Write tests for ScannerScreen (ProductNotFound triggers Bottom Sheet, registration success advances flow)
- [ ] Write tests for DashboardScreen (counters displayed, tap filters list)
- [ ] Write tests for HistoryScreen (search filters, status filter)
- [ ] Verify all tests pass

**Priority**: Medium
**Layer**: Testing
**Estimated Hours**: 3

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

---

## EPIC-08: CI/CD & Deployment

### Task M-08-Ticket-01: GitHub Actions Build Pipeline

**Description**: Create GitHub Actions workflow for automated builds and tests.

**Acceptance Criteria**:
- [ ] Pipeline runs on push to main
- [ ] Pipeline runs on pull request
- [ ] All unit tests execute
- [ ] All integration tests execute
- [ ] Build succeeds in ≤5 minutes
- [ ] Build artifacts (APK) generated

**Subtasks**:
- [ ] Create .github/workflows/build.yml
- [ ] Configure Java/Kotlin environment
- [ ] Configure Gradle build step
- [ ] Configure test execution step
- [ ] Configure APK build step
- [ ] Add artifact upload
- [ ] Test pipeline on push
- [ ] Test pipeline on PR

**Priority**: Medium
**Layer**: Infrastructure
**Estimated Hours**: 5

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

---

### Task M-08-Ticket-02: Direct APK Deployment Flow

**Description**: Document and automate APK installation process for XCover7 devices.

**Acceptance Criteria**:
- [ ] APK installs on XCover7 without errors
- [ ] App launches successfully after installation
- [ ] Version update overwrites previous version
- [ ] Data preserved (Room database migration)
- [ ] Documentation created for deployment process

**Subtasks**:
- [ ] Configure APK output directory
- [ ] Create deployment script (optional)
- [ ] Test APK installation on XCover7
- [ ] Test version update scenario
- [ ] Document deployment process
- [ ] Create user guide for installation

**Priority**: Medium
**Layer**: Infrastructure
**Estimated Hours**: 3

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

---

## File Creation Checklist

### Build & Infrastructure (3 files)
- [ ] `build.gradle.kts` (project level)
- [ ] `app/build.gradle.kts`
- [ ] `.github/workflows/build.yml`

### Data Layer - Database (5 files)
- [ ] `AppDatabase.kt`
- [ ] `ProductCatalogEntity.kt` (@Index on EAN)
- [ ] `ActiveStockEntity.kt`
- [ ] `DateConverter.kt`
- [ ] `ProductCatalogDao.kt`

### Data Layer - Repository (3 files)
- [ ] `ProductRepository.kt` (interface)
- [ ] `ProductRepositoryImpl.kt`
- [ ] `ProductMapper.kt`

### Data Layer - DI Modules (3 files)
- [ ] `DatabaseModule.kt` (production)
- [ ] `FakeDatabaseModule.kt` (tests)
- [ ] `UseCaseModule.kt`

### Domain Layer - Models (3 files)
- [ ] `Product.kt`
- [ ] `RegisterProductRequest.kt`
- [ ] `ValidationError.kt`

### Domain Layer - Results (1 file)
- [ ] `RegisterResult.kt`

### Domain Layer - Use Cases (5 files)
- [ ] `RegisterProductUseCase.kt` (TDD)
- [ ] `ParseExpiryDateUseCase.kt` (TDD)
- [ ] `CalculateStatusUseCase.kt` (TDD)
- [ ] `UpsertStockUseCase.kt` (TDD)
- [ ] `GetSemaphoreCountersUseCase.kt` (TDD)

### Presentation Layer - ViewModels (4 files)
- [ ] `ScannerViewModel.kt` (with ProductNotFound state)
- [ ] `DashboardViewModel.kt`
- [ ] `HistoryViewModel.kt`
- [ ] `ManualEntryViewModel.kt`

### Presentation Layer - Screens (5 files)
- [ ] `ScannerScreen.kt` (integrated with ProductRegistrationBottomSheet)
- [ ] `ProductRegistrationBottomSheet.kt` (NEW - thumb zone)
- [ ] `DashboardScreen.kt`
- [ ] `HistoryScreen.kt`
- [ ] `ManualEntryScreen.kt`

### Worker Layer (1 file)
- [ ] `SemaphoreUpdateWorker.kt`

### Tests - Domain Layer (5 files)
- [ ] `RegisterProductUseCaseTest.kt` (TDD, ≥90% coverage)
- [ ] `ParseExpiryDateUseCaseTest.kt` (TDD)
- [ ] `CalculateStatusUseCaseTest.kt` (TDD)
- [ ] `UpsertStockUseCaseTest.kt` (TDD)
- [ ] `GetSemaphoreCountersUseCaseTest.kt` (TDD)

### Tests - Data Layer (3 files)
- [ ] `ProductCatalogDaoTest.kt`
- [ ] `ActiveStockDaoTest.kt`
- [ ] `ProductRepositoryImplTest.kt`

### Tests - Presentation Layer (4 files)
- [ ] `ProductRegistrationBottomSheetTest.kt` (NEW - auto-focus, validation, tap targets)
- [ ] `ScannerScreenTest.kt`
- [ ] `DashboardScreenTest.kt`
- [ `HistoryScreenTest.kt`

### Tests - Worker Layer (1 file)
- [ ] `SemaphoreUpdateWorkerTest.kt`

**Total Files**: 41

---

## Execution Roadmap

### Sprint 1 (Week 1-2): Foundation (EPIC-01, EPIC-02, EPIC-03)
**Week 1**:
- Day 1-2: M-01-Ticket-01, M-01-Ticket-02 (Project Setup)
- Day 3-4: M-02-Ticket-01, M-02-Ticket-02 (ProductCatalog, DAO)
- Day 5: M-02-Ticket-03, M-02-Ticket-04 (Repository)

**Week 2**:
- Day 1: M-02-Ticket-05 (Hilt Modules)
- Day 2-3: M-03-Ticket-01 (RegisterProductUseCase - TDD)
- Day 4: M-03-Ticket-02 (Domain Models)
- Day 5: M-04-Ticket-01, M-04-Ticket-02 (Additional Use Cases - TDD)

### Sprint 2 (Week 3-4): Core Implementation (EPIC-04, EPIC-05)
**Week 3**:
- Day 1: M-04-Ticket-03, M-04-Ticket-04 (Use Cases - TDD)
- Day 2-3: M-05-Ticket-01 (ScannerViewModel)
- Day 4-5: M-05-Ticket-02 (ProductRegistrationBottomSheet)

**Week 4**:
- Day 1-2: M-05-Ticket-03, M-05-Ticket-04 (Scanner Integration, Dashboard)
- Day 3: M-05-Ticket-05, M-05-Ticket-06 (History, One-Handed)
- Day 4-5: M-06-Ticket-01, M-06-Ticket-02 (WorkManager)

### Sprint 3 (Week 5): Testing & Deployment (EPIC-07, EPIC-08)
**Week 5**:
- Day 1-2: M-07-Ticket-01, M-07-Ticket-02, M-07-Ticket-03 (Testing)
- Day 3-4: M-08-Ticket-01, M-08-Ticket-02 (CI/CD)
- Day 5: Final integration testing and bug fixes

---

## Summary

**EPIC-01**: 2 tasks, 10 hours, Infrastructure
**EPIC-02**: 5 tasks, 21 hours, Data Layer (Dynamic Registration)
**EPIC-03**: 2 tasks, 10 hours, Domain Layer (RegisterProductUseCase - TDD)
**EPIC-04**: 4 tasks, 24 hours, Domain Layer (Additional Use Cases - TDD)
**EPIC-05**: 6 tasks, 31 hours, Presentation Layer
**EPIC-06**: 2 tasks, 13 hours, WorkManager
**EPIC-07**: 3 tasks, 16 hours, Testing Strategy
**EPIC-08**: 2 tasks, 8 hours, CI/CD & Deployment

**Total**: 26 tasks, 133 hours (17-20 days for 1 developer, 8-10 days for 2 developers)

---

## Critical Notes

**TDD MANDATORY**:
- All UseCase tasks MUST include unit tests written BEFORE implementation
- Minimum coverage: ≥90% for all use cases
- Red → Green → Refactor cycle for each use case

**Dynamic Registration ONLY**:
- NO CSV import functionality (Task-02.3 removed completely)
- NO CsvImporter.kt file
- Registration flow: Scan EAN → ProductNotFound → Bottom Sheet → Register → Save

**XCover7 Optimization**:
- Minimum tap targets: 48dp × 48dp (recommended 56dp)
- Thumb zone: Bottom 20% of screen (~138dp)
- One-handed layout for all screens
- High contrast colors (>4.5:1)

**Performance Targets**:
- Bottom Sheet render: <200ms
- findByEan() query: <100ms
- Complete flow (scan → save): <4s (excluding user input)
- Complete flow (with input): <10s

---

**Status**: ✅ Ready for implementation (sdd-apply)
**Focus**: EPIC-02, EPIC-03 (Dynamic Registration with TDD)
**Next Step**: Start M-02-Ticket-01 (Create ProductCatalogEntity with @Index)
