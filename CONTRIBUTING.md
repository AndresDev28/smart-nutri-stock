# Contributing to Smart Nutri-Stock

Thank you for your interest in contributing to Smart Nutri-Stock! This document provides guidelines for code standards, workflow, testing, and release processes.

## 📋 Table of Contents

- [Code Standards](#code-standards)
- [Workflow](#workflow)
- [Testing](#testing)
- [Release Checklist](#release-checklist)
- [PR Review Checklist](#pr-review-checklist)

---

## 🏛 Code Standards

### Clean Architecture

Smart Nutri-Stock follows strict **Clean Architecture** principles. The code is organized into three layers:

#### Domain Layer (`domain/`)
- **Purpose**: Business logic and enterprise rules
- **Contains**: Entities, Use Cases, Repository Interfaces
- **Rules**:
  - No Android framework dependencies
  - Pure Kotlin code (no Context, no UI classes)
  - Use Cases are single-responsibility functions
  - Repository interfaces are defined here

#### Data Layer (`data/`)
- **Purpose**: Data access and external service integration
- **Contains**: Repository implementations, Room database, DataStore, Supabase client
- **Rules**:
  - Implements repository interfaces from domain layer
  - Uses Room for local persistence (offline-first)
  - Uses DataStore for key-value storage (session tokens)
  - Uses Supabase SDK for cloud sync
  - No business logic in this layer

#### Presentation Layer (`presentation/`)
- **Purpose**: UI and user interaction
- **Contains**: ViewModels, Screens (Compose), UI Components
- **Rules**:
  - MVVM pattern with ViewModels
  - StateFlow for reactive state management
  - Jetpack Compose for UI
  - All state logic consumed from domain use cases (not calculated in UI)
  - Composables RENDER state, they do NOT calculate it

### Kotlin & Jetpack Compose Guidelines

- **Naming**: Follow Kotlin naming conventions (camelCase for variables/functions, PascalCase for classes)
- **Immutability**: Prefer `val` over `var`, use data classes for immutable data
- **Coroutines**: Use `suspend` functions for async operations, `viewModelScope` for ViewModels
- **Flow**: Use `StateFlow` for UI state, `SharedFlow` for events
- **Compose**: Remember composables with `@Composable`, use `remember` for local state, `rememberSaveable` for state that survives recomposition

### Dependency Injection (Hilt)

All dependencies are injected via Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideStockRepository(
        dao: StockDao,
        supabaseClient: SupabaseClient
    ): StockRepository {
        return StockRepositoryImpl(dao, supabaseClient)
    }
}
```

**Rules**:
- Use `@Module` and `@InstallIn` for module definitions
- Use `@Provides` for dependency provision
- Use `@Singleton` for dependencies that should be singletons
- Inject ViewModels with `@HiltViewModel`
- Use constructor injection wherever possible

---

## 🔄 Workflow

### Branch Naming

Follow this pattern for branch names:

- **Feature branches**: `feature/feature-name` (e.g., `feature/ocr-scanner`)
- **Bugfix branches**: `bugfix/description` (e.g., `bugfix/crash-on-login`)
- **Documentation branches**: `docs/description` (e.g., `docs/update-readme`)
- **Refactor branches**: `refactor/component-name` (e.g., `refactor/mvvm-cleanup`)

### Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic changes)
- `refactor`: Code refactoring (no new features, no bug fixes)
- `perf`: Performance improvements
- `test`: Test additions or modifications
- `chore`: Maintenance tasks (build, dependencies)
- `ci`: CI/CD changes

**Examples**:
- `feat(scanner): add OCR expiry date scanning`
- `fix(dashboard): crash when opening empty stock list`
- `docs(readme): update version to 2.7.0`
- `chore(deps): upgrade Gradle to 8.5`

### Pull Requests

- No direct pushes to `main` branch
- All changes must go through Pull Requests
- Ensure all CI checks pass (Lint and Tests)
- Include a clear description of changes
- Reference related issues (e.g., `Fixes #123`)

---

## 🧪 Testing

### TDD Approach

**Unit tests for business logic must be written BEFORE implementation.** This is a mandatory requirement for the Domain layer.

### Coverage Targets

- **Domain Layer**: Aim for **90% coverage**
- **Data Layer**: Aim for **80% coverage** (focus on repository implementations)
- **Presentation Layer**: Aim for **60% coverage** (focus on ViewModels and key UI components)

### Testing Structure

```
app/src/test/java/com/decathlon/smartnutristock/
├── domain/
│   ├── usecase/
│   │   ├── UpsertStockUseCaseTest.kt
│   │   └── CalculateStatusUseCaseTest.kt
│   └── validation/
│       └── EanValidatorTest.kt
└── data/
    └── repository/
        └── StockRepositoryImplTest.kt
```

### Testing Guidelines

- Use **JUnit 5** for unit tests
- Use **MockK** for mocking dependencies
- Use **Truth** for assertions (or `assertThat` style)
- Test both happy paths and edge cases
- Test coroutines with `runTest` and `advanceUntilIdle`
- Mock Room DAOs for repository tests (not the database itself)

### Example Test

```kotlin
@Test
fun `calculate status returns GREEN when expiry is more than 7 days away`() = runTest {
    // Given
    val expiryDate = Instant.now().plus(10, ChronoUnit.DAYS)
    val useCase = CalculateStatusUseCase()

    // When
    val result = useCase(expiryDate)

    // Then
    assertThat(result).isEqualTo(SemaphoreStatus.GREEN)
}
```

---

## ✅ Release Checklist

Follow these steps when releasing a new version:

1. **Update Build Version**
   - Bump `versionName` in `app/build.gradle.kts` (line 35)
   - Update `versionCode` as needed (increment by 1 for each release)

2. **Add CHANGELOG Entry**
   - Add version header: `## [X.Y.Z] - YYYY-MM-DD`
   - List all changes under appropriate sections (Features, Bug Fixes, etc.)
   - Follow conventional commits format for entries

3. **Update README Badges**
   - Update version badge in line 28 (if applicable)
   - Add new features to "Key Features" section
   - Update "Tech Stack" section if new dependencies added

4. **Update MASTER_SPEC.md** (if spec changes)
   - Document any specification changes
   - Update acceptance criteria if needed

5. **Validate Sync**
   - Run `.github/pre-commit-docs.sh` to verify version consistency
   - Check that all docs reference the same version number

6. **Commit Changes**
   - **IMPORTANT**: Commit all version-related changes together in a single commit
   - Use commit message: `docs: bump version to X.Y.Z`
   - Do NOT split version updates across multiple commits

### Pre-commit Validation (Optional but Recommended)

Before committing version changes, run:

```bash
chmod +x .github/pre-commit-docs.sh && .github/pre-commit-docs.sh
```

This script validates that `app/build.gradle.kts`, `CHANGELOG.md`, and `README.md` all reference the same version.

### Weekly Documentation Review (Optional)

Perform a weekly review to prevent documentation drift:

1. **Check Version Consistency**
   - Run `.github/pre-commit-docs.sh` to verify all docs reference current version
   - Search for outdated version references: `grep -r "v2\.[0-9]\.[0-9]" .`

2. **Validate Links**
   - Click through all documentation links (README, CONTRIBUTING, ARCHITECTURE)
   - Verify archived files (docs/archived/) are still accessible
   - Check for broken external links (Supabase, ML Kit, etc.)

3. **Archive Obsolete Docs**
   - Move outdated documentation to `docs/archived/`
   - Create redirect stub files at original location
   - Update CONTRIBUTING.md with archive location

4. **Update Feature Documentation**
   - Verify new features are documented in README.md "Key Features" section
   - Check "Tech Stack" section reflects current dependencies
   - Ensure architecture changes are in ARCHITECTURE.md

---

## 👀 PR Review Checklist

Before merging a Pull Request, ensure:

### Code Quality
- [ ] Code follows Clean Architecture principles
- [ ] No Android dependencies in Domain layer
- [ ] All business logic is in Use Cases, not ViewModels or composables
- [ ] Hilt injection is used correctly
- [ ] Code is properly formatted (no unnecessary whitespace, consistent indentation)

### Testing
- [ ] Unit tests are written for business logic (TDD for Domain layer)
- [ ] Tests pass locally
- [ ] Coverage meets targets (90% for Domain, 80% for Data)
- [ ] Edge cases are tested

### Documentation
- [ ] New features are documented in README.md
- [ ] Complex logic has inline comments
- [ ] Public API has KDoc comments

### Hardware Optimization (Samsung XCover7)
- [ ] Tap targets are minimum 48dp (one-handed use)
- [ ] Barcode scanning responds in <2 seconds
- [ ] Background tasks (WorkManager) minimize battery drain

### CI/CD
- [ ] All CI checks pass (Lint and Tests)
- [ ] No merge conflicts with `main` branch
- [ ] Version references are consistent (if version changed)

---

## 📚 Additional Resources

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Hilt Dependency Injection](https://dagger.dev/hilt/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)

---

<p align="center">
  <i>Questions? Contact <a href="https://github.com/AndresDev28">AndresDev</a></i>
</p>
