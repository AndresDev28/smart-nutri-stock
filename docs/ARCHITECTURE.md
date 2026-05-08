# Architecture Documentation

Smart Nutri-Stock follows **Clean Architecture** principles to ensure scalability, testability, and maintainability. This document describes the system architecture, data flow, database schema, and layer responsibilities.

## 📐 Clean Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                   Presentation Layer                         │
│                   (Jetpack Compose + MVVM)                   │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Composable Screens (Dashboard, Scanner, History, Auth)│ │
│  │  ↓                                                      │ │
│  │  ViewModels (DashboardViewModel, ScannerViewModel)      │ │
│  │  ↓                                                      │ │
│  │  StateFlow (UI State Reactive Streams)                 │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            ↓ depends on
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│                  (Business Logic & Rules)                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Use Cases (UpsertStockUseCase, CalculateStatusUseCase)│ │
│  │  ↓                                                      │ │
│  │  Entities (Stock, Product, SemaphoreStatus)            │ │
│  │  ↓                                                      │ │
│  │  Repository Interfaces (StockRepository)                │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                            ↓ depends on
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                              │
│              (Data Access & External Services)                │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Repository Implementations (StockRepositoryImpl)         │ │
│  │  ↓                                                      │ │
│  │  Local Data Sources (Room Database, DataStore)          │ │
│  │  ↓                                                      │ │
│  │  Remote Data Sources (Supabase - Auth & PostgREST)      │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Layer Dependency Rule

**Dependencies point inward**: Presentation depends on Domain, Domain depends on Data (interfaces only). Data never depends on Domain or Presentation.

---

## 🔄 Data Flow

### Main Application Flow (Scanner → Dashboard)

```
    ┌─────────────────────────────────────────────────────────────┐
    │  1. Scanner Screen (Presentation)                            │
    │     User scans EAN barcode → ML Kit returns result            │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  2. ProductRegistrationBottomSheet (Presentation)            │
    │     User confirms product details and enters expiry date    │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  3. UpsertStockUseCase (Domain)                              │
    │     Validates data → Calls StockRepository                  │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  4. StockRepository (Data)                                   │
    │     Inserts/Updates Room database → Marks as dirty for sync  │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  5. CalculateStatusUseCase (Domain)                          │
    │     Calculates semaphore status (GREEN/YELLOW/EXPIRED)       │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  6. DashboardViewModel (Presentation)                       │
    │     Observes StateFlow → Emits updated UI state             │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  7. DashboardScreen (Presentation)                           │
    │     LazyColumn recomposes → Shows new NutriCard             │
    └─────────────────────────────────────────────────────────────┘
```

### Cloud Sync Flow (Post-Login)

```
    ┌─────────────────────────────────────────────────────────────┐
    │  1. Login Success (Presentation)                            │
    │     Supabase Auth completes → User session saved           │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  2. SyncWorker (Data)                                        │
    │     Background task triggers immediate sync                │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  3. StockRepository.sync() (Data)                           │
    │     Pull: Fetch dirty records → Push to Supabase           │
    │     Push: Fetch remote changes → Merge with Room           │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  4. Conflict Resolution (Data)                              │
    │     Optimistic locking (version field) resolves conflicts   │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  5. Product Catalog Population (Data)                        │
    │     Pull full product catalog → Populate Room               │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  6. Dashboard Updates (Presentation)                        │
    │     StateFlow recomposes → UI reflects synced data         │
    └─────────────────────────────────────────────────────────────┘
```

### Notification Flow (Daily WorkManager)

```
    ┌─────────────────────────────────────────────────────────────┐
    │  1. WorkManager Scheduler (Data)                            │
    │     Daily trigger at 09:00 → ExpiryNotificationWorker       │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  2. Query Expiring Batches (Data)                            │
    │     StockDao.findExpiringBatches() → Returns YELLOW/EXPIRED │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  3. Create Notifications (Data)                             │
    │     Group notifications by expiry status → Send to user     │
    └─────────────────────────────────────────────────────────────┘
```

---

## 🗄 Database Schema

### ProductCatalog Table

Product catalog table stores product reference data (EAN, name, pack size). This table is populated via cloud sync.

| Column | Type | Description | Constraints |
|--------|------|-------------|-------------|
| `ean` | String (PK) | 13-digit EAN code (GS1 standard) | PRIMARY KEY, INDEX |
| `name` | String | Product name (3-100 characters) | INDEX |
| `packSize` | Int | Pack size in grams | Not Null |
| `createdAt` | Long | Timestamp when product was registered | Not Null |
| `createdBy` | Long | User ID who registered this product | Not Null |
| `daysUntilExpiry` | Int | Days until expiry (negative if expired) | Default: 0 |

**Indexes**:
- `ean`: Fast lookups (O(log n) instead of O(n))
- `name`: Search by product name

**Entity Class**: `ProductCatalogEntity`

---

### ActiveStock Table

Active stocks table stores inventory batches with their workflow status, sync metadata, and expiry information.

| Column | Type | Description | Constraints |
|--------|------|-------------|-------------|
| `id` | String (PK) | Unique UUID identifier for this batch | PRIMARY KEY |
| `ean` | String | 13-digit EAN code of product | Not Null |
| `quantity` | Int | Number of units in this batch | Not Null |
| `expiryDate` | Instant | Expiry date of this batch (UTC) | Not Null |
| `createdAt` | Instant | Timestamp when this batch was first created | Not Null |
| `updatedAt` | Instant | Timestamp when this batch was last modified | Not Null |
| `deletedAt` | Instant? | Timestamp when batch was soft-deleted | Nullable |
| `actionTaken` | String | Workflow action taken (PENDING, DISCOUNTED, REMOVED) | Default: "PENDING" |
| `userId` | String? | User ID who created/modified this batch | Nullable |
| `storeId` | String | Store ID for multitenancy (default: "1620") | Default: "1620" |
| `syncedAt` | Instant? | Timestamp when this batch was last synced with Supabase | Nullable |
| `version` | Int | Optimistic lock version for conflict resolution | Default: 1 |
| `deviceId` | String? | Device ID that created/modified this batch | Nullable |
| `isDirty` | Int | Flag indicating if this batch has unsynced changes | Default: 1 |

**Indexes**:
- `(ean, expiryDate)`: UNIQUE - Prevents duplicate batches for same product and expiry
- `deletedAt`: Soft-deleted records
- `userId`: User-specific queries
- `storeId`: Store-specific queries
- `isDirty`: Dirty records for sync

**Entity Class**: `ActiveStockEntity`

---

## ☁️ Sync Pipeline (Supabase ↔ Room)

### Offline-First Architecture

The app uses an offline-first approach: all data is stored locally in Room, with Supabase serving as the remote backup and synchronization source.

### Sync Flow

```
    ┌─────────────────────────────────────────────────────────────┐
    │  1. Local Write (User Action)                               │
    │     User scans product → Upsert to Room → Mark isDirty=1   │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  2. Sync Trigger                                             │
    │     Login success OR Network available OR Periodic sync    │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  3. Push (Upload Dirty Records)                             │
    │     Query: WHERE isDirty=1                                 │
    │     For each dirty record:                                  │
    │       - POST to Supabase (PostgREST)                       │
    │       - On success: syncedAt=NOW(), isDirty=0, version++  │
    │       - On failure: Retry later (exponential backoff)     │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  4. Pull (Fetch Remote Changes)                             │
    │     GET from Supabase with updatedAt filter:               │
    │     WHERE updatedAt > lastSyncTime                          │
    │     For each remote record:                                 │
    │       - If local exists and remote.version > local.version │
    │         → Update local (remote wins)                        │
    │       - If local doesn't exist                             │
    │         → Insert local                                     │
    └─────────────────────────────────────────────────────────────┘
                                ↓
    ┌─────────────────────────────────────────────────────────────┐
    │  5. Conflict Resolution                                      │
    │     Optimistic locking via version field:                  │
    │     - If remote.version > local.version → Remote wins     │
    │     - If remote.version < local.version → Local wins      │
    │     - If versions equal → No conflict                      │
    │     - Concurrent edits → Last writer wins (rare)           │
    └─────────────────────────────────────────────────────────────┘
```

### Sync Components

- **StockRepository**: Orchestrates push/pull operations
- **SupabaseClient**: Kotlin SDK for Gotrue (Auth) and PostgREST (Database)
- **WorkManager**: Background sync tasks (scheduled and network-triggered)
- **DataStore**: Stores last sync timestamp and auth tokens

### Sync Strategy

| Scenario | Behavior |
|----------|----------|
| **Offline Edit** | Write to Room, mark isDirty=1, sync when network available |
| **Login Success** | Trigger immediate sync (push dirty records, pull remote changes) |
| **Multiple Devices** | Optimistic locking (version field) resolves conflicts |
| **Product Catalog** | Full pull on sync (product data is read-only for stores) |

---

## 📦 Layer Responsibilities

### Domain Layer (`domain/`)

**Purpose**: Business logic and enterprise rules. This layer is framework-independent and contains pure Kotlin code.

**Subpackages**:
- `model/`: Business entities (Stock, Product, SemaphoreStatus)
- `usecase/`: Use cases (single-responsibility business operations)
- `repository/`: Repository interfaces (contracts for data access)
- `validation/`: Business validation rules (EAN validation, expiry date validation)
- `export/`: Export use cases (PDF generation, CSV export)

**Key Files**:
- `UpsertStockUseCase`: Validates and inserts/updates stock records
- `CalculateStatusUseCase`: Calculates semaphore status (GREEN/YELLOW/EXPIRED)
- `StockRepository`: Interface defining stock data access operations

**Rules**:
- ✅ No Android framework dependencies (no Context, no UI classes)
- ✅ Pure Kotlin code with suspend functions for async operations
- ✅ Use cases are single-responsibility functions
- ✅ Repository interfaces defined here, implemented in Data layer
- ❌ NO direct database access
- ❌ NO UI components
- ❌ NO Android SDK dependencies

---

### Data Layer (`data/`)

**Purpose**: Data access and external service integration. Implements repository interfaces from Domain layer.

**Subpackages**:
- `entity/`: Room entities (ActiveStockEntity, ProductCatalogEntity, UserEntity)
- `dao/`: Room DAOs (StockDao, ProductCatalogDao, UserDao)
- `repository/`: Repository implementations (StockRepositoryImpl)
- `local/`: Local data sources (Room database, DataStore)
- `remote/`: Remote data sources (Supabase client)
- `worker/`: Background workers (WorkManager tasks)
- `notification/`: Local push notification logic
- `validation/`: Data validation (EAN format validation)
- `export/`: Export implementations (PDF writer, CSV writer)

**Key Files**:
- `AppDatabase`: Room database configuration
- `StockRepositoryImpl`: Implements StockRepository interface
- `ProductCatalogDao`: CRUD operations for product catalog
- `StockDao`: CRUD operations for active stocks
- `ExpiryNotificationWorker`: Daily expiry notifications

**Rules**:
- ✅ Implements repository interfaces from Domain layer
- ✅ Uses Room for local persistence (offline-first)
- ✅ Uses DataStore for key-value storage (session tokens)
- ✅ Uses Supabase SDK for cloud sync
- ✅ No business logic in this layer
- ❌ NO direct access to Domain layer (only via interfaces)
- ❌ NO UI components

---

### Presentation Layer (`presentation/`)

**Purpose**: UI and user interaction. Displays data from ViewModel and handles user events.

**Subpackages**:
- `viewmodel/`: ViewModels (DashboardViewModel, ScannerViewModel, HistoryViewModel)
- `ui/`: Jetpack Compose screens and components
  - `dashboard/`: Dashboard screen and components
  - `scanner/`: Scanner screen and OCR integration
  - `history/`: History screen and stock list
  - `auth/`: Login screen and auth UI
  - `theme/`: Theme system (colors, typography, shapes)
  - `components/`: Reusable UI components (NutriCard, PremiumButton, etc.)
- `permission/`: Runtime permission handling (Camera, Notifications)

**Key Files**:
- `DashboardScreen`: Main dashboard with semaphore counters and stock list
- `ScannerScreen`: Camera-based barcode scanner with OCR
- `DashboardViewModel`: Exposes dashboard state via StateFlow
- `ScannerViewModel`: Manages scanner state and OCR processing
- `NutriCard`: Reusable card component for stock items

**Rules**:
- ✅ MVVM pattern with ViewModels
- ✅ StateFlow for reactive state management
- ✅ Jetpack Compose for UI
- ✅ All state logic consumed from domain use cases (not calculated in UI)
- ✅ Composables RENDER state, they do NOT calculate it
- ✅ One-handed use optimization (48dp tap targets for XCover7)
- ❌ NO business logic in composables or ViewModels
- ❌ NO direct database access (via ViewModels only)

---

## 🔗 Dependency Injection (Hilt)

### Hilt Modules

```
di/
├── AppModule.kt          # App-level dependencies (Context, Application)
├── DatabaseModule.kt    # Room database and DAOs
├── RepositoryModule.kt  # Repository implementations
├── UseCaseModule.kt     # Use case providers
└── WorkerModule.kt      # WorkManager dependencies
```

### Injection Pattern

```kotlin
// Repository Module
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideStockRepository(
        dao: StockDao,
        supabaseClient: SupabaseClient
    ): StockRepository {
        return StockRepositoryImpl(dao, supabaseClient)
    }
}

// Use Case Module
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    @Provides
    fun provideUpsertStockUseCase(
        repository: StockRepository
    ): UpsertStockUseCase {
        return UpsertStockUseCase(repository)
    }
}

// ViewModel
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getStocksUseCase: GetStocksUseCase,
    private val calculateStatusUseCase: CalculateStatusUseCase
) : ViewModel() {
    // ...
}
```

---

## 🎨 State Flow Pattern

### UI State Management

```
UseCase (Domain)
      ↓ produces State
ViewModel (Presentation)
      ↓ exposes StateFlow
Screen (Presentation)
      ↓ collects State
Composable (Presentation)
      ↓ renders State
User sees updated UI
```

### Example: Dashboard State

```kotlin
// ViewModel
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getStocksUseCase: GetStocksUseCase,
    private val calculateStatusUseCase: CalculateStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadStocks()
    }

    private fun loadStocks() {
        viewModelScope.launch {
            try {
                val stocks = getStocksUseCase()
                val greenCount = stocks.count { calculateStatusUseCase(it.expiryDate) == SemaphoreStatus.GREEN }
                val yellowCount = stocks.count { calculateStatusUseCase(it.expiryDate) == SemaphoreStatus.YELLOW }
                val expiredCount = stocks.count { calculateStatusUseCase(it.expiryDate) == SemaphoreStatus.EXPIRED }

                _uiState.value = DashboardUiState.Success(
                    stocks = stocks,
                    greenCount = greenCount,
                    yellowCount = yellowCount,
                    expiredCount = expiredCount
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Screen
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is DashboardUiState.Success -> {
            DashboardContent(
                stocks = (uiState as DashboardUiState.Success).stocks,
                greenCount = (uiState as DashboardUiState.Success).greenCount,
                // ...
            )
        }
        is DashboardUiState.Loading -> {
            ShimmerLoading()
        }
        is DashboardUiState.Error -> {
            ErrorMessage((uiState as DashboardUiState.Error).message)
        }
    }
}
```

---

## 📝 Summary

Smart Nutri-Stock's architecture follows Clean Architecture principles with three distinct layers:

1. **Domain Layer**: Pure business logic, framework-independent, defines use cases and repository interfaces
2. **Data Layer**: Data access, implements repositories, uses Room for offline-first storage and Supabase for cloud sync
3. **Presentation Layer**: UI with MVVM pattern, Jetpack Compose, and StateFlow for reactive state management

The offline-first approach ensures the app works without network connectivity, with automatic synchronization when connection is restored. The sync pipeline uses optimistic locking to resolve conflicts between multiple devices.

---

<p align="center">
  <i>Last updated: 2026-05-08 (v2.7.0)</i>
</p>
