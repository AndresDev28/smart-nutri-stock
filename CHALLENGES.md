# 🧪 CHALLENGES.md — Technical Memory: Smart Nutri-Stock

> **Purpose**: This document serves as the institutional technical memory of the Smart Nutri-Stock project. It documents the most significant obstacles, hardware peculiarities, and key architectural decisions encountered during development. Each entry follows a consistent diagnostic structure.

---

## 🐛 1. Silent Notification Drop on Android 13+ (The Invisible Bell)

### 🩺 The Symptom
What appeared to be happening: The UI reacted correctly, the WorkManager enqueued the task, all permissions were granted, and the notification helper was invoked. However, the notification NEVER appeared in the system shade. The Logcat showed zero errors — no crash, no warning, nothing. It was as if the notification was swallowed by a black hole.

The twist: A Toast confirmation appeared successfully when pressing the debug test button, proving the ViewModel and UI layer were working perfectly. The failure was happening at the OS level, completely silently.

### 🕵️‍♂️ Root Cause
Three interrelated issues were causing Android to silently destroy the notifications:

1. **Invalid SmallIcon Resource**: The notification builder used a custom drawable `R.drawable.ic_notification` that didn't meet Android's requirements for notification icons. Starting from Android 5.0 (Lollipop), notification small icons must be monochromatic vectors with transparent backgrounds. Android renders them as silhouettes — any icon with complex colors, gradients, or an invalid resource reference results in the OS silently dropping the entire notification to prevent system crashes.

2. **NotificationChannel Not Registered Before notify()**: Starting from Android 8.0 (Oreo), every notification MUST be associated with a pre-registered `NotificationChannel`. The `NotificationHelper` was building notifications before ensuring the channel existed. Worse, it used `NotificationManagerCompat` to register channels, which is unreliable on certain OEM devices. The native `context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager` is required for reliable channel registration.

3. **Channel ID Mismatch Risk**: The `CHANNEL_ID` string used in `NotificationChannel(...)` constructor must be character-for-character identical to the one used in `NotificationCompat.Builder(context, CHANNEL_ID)`. Any mismatch (even invisible characters) causes a silent drop.

### 🛠️ The Solution
1. Replaced ALL custom SmallIcon references with bulletproof system icons (`android.R.drawable.ic_dialog_info`) for immediate validation
2. Added `createNotificationChannel()` as the FIRST instruction in every method that sends notifications — before any `NotificationCompat.Builder` is instantiated
3. Changed channel registration from `NotificationManagerCompat` to native `NotificationManager`
4. Verified CHANNEL_ID string consistency across all usages
5. Created a proper monochromatic vector `ic_notification_leaf.xml` with `android:fillColor="#FFFFFFFF"` on transparent background for production branding

### 🧠 Engram (Lesson for the Future)
> **Never assume a silent notification failure is a permissions issue.** Always check THREE things first: (1) Is the NotificationChannel registered with the NATIVE NotificationManager before the builder? (2) Is the CHANNEL_ID string identical in both channel creation and builder? (3) Is the SmallIcon a valid monochromatic vector drawable? Android will silently destroy notifications for any of these violations without logging a single error.

---

## 🐛 2. The Hilt Injection Chain Failure (The 06:00 AM Ghost)

### 🩺 The Symptom
The StatusCheckWorker was scheduled to run at 06:00 AM with a correctly calculated `initialDelay`. The schedule was mathematically verified via a Toast showing the delay in hours. But at 06:00 AM — nothing happened. No notification, no Logcat output.

When the app was opened at 13:23 and inspected via Android Studio's Background Task Inspector, the worker appeared as "Enqueued" but with a start time of 13:23 (the current moment), NOT the future scheduled time for 06:00 AM the next day. This meant the work had been re-enqueued, not preserved.

Simultaneously, the IDE showed red errors in `StatusCheckWorker.kt` around line 32 (`@HiltWorker`) and lines 35-36 (`@AssistedInject` constructor).

### 🕵️‍♂️ Root Cause
A four-link chain failure:

1. **Outdated Hilt Dependencies**: The `build.gradle.kts` had `hilt-work:1.1.0` and `hilt-compiler:1.1.0` — versions that had known issues with `@HiltWorker` factory generation. The annotation processor wasn't generating the required `HiltWorkerFactory` code at compile-time.

2. **Missing Factory → Runtime Crash**: Without the generated factory, WorkManager couldn't instantiate `StatusCheckWorker` via Hilt. When the 06:00 AM trigger fired, the worker CRASHED during instantiation. But since WorkManager catches worker failures internally, this crash was invisible — it just marked the work as FAILED and applied exponential backoff.

3. **ExistingPeriodicWorkPolicy.KEEP Masking**: The `KEEP` policy preserved the FAILED work without recalculating the `initialDelay`. When the app reopened at 13:23, `KEEP` saw existing work and did nothing — the dead work stayed dead. The Inspector showed "Enqueued" because WorkManager had already consumed the original initialDelay and was in a retry state.

4. **Default WorkManagerInitializer Override**: The AndroidManifest didn't have the `tools:node="remove"` directive for `WorkManagerInitializer`. This meant Android's default initializer was running alongside the custom `Configuration.Provider` in `SmartNutriStockApp`, potentially overriding the `HiltWorkerFactory` configuration.

### 🛠️ The Solution
1. Upgraded Hilt dependencies: `hilt-work:1.1.0` → `1.2.0`, `hilt-compiler:1.1.0` → `1.2.0`
2. Implemented smart conditional enqueue policy:
   ```kotlin
   val shouldReplace = workInfos.isEmpty() ||
       workInfos.all { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }
   val policy = if (shouldReplace) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP
   ```
3. Added `InitializationProvider` with `tools:node="remove"` for `WorkManagerInitializer` in AndroidManifest.xml
4. Verified `StatusCheckWorker` annotations: `@HiltWorker` on class, `@AssistedInject` on constructor, `@Assisted` only on context and workerParams

### 🧠 Engram (Lesson for the Future)
> **Hilt + WorkManager integration requires THREE non-negotiable pieces**: (1) The `hilt-work` dependency with matching `hilt-compiler` kapt processor, (2) The Application class implementing `Configuration.Provider` with injected `HiltWorkerFactory`, (3) The AndroidManifest `tools:node="remove"` on the default `WorkManagerInitializer`. Missing ANY one of these causes silent runtime failures. Additionally, `ExistingPeriodicWorkPolicy.KEEP` will preserve dead work forever — always check `WorkInfo.State` before deciding KEEP vs REPLACE.

---

## 🐛 3. The Xiaomi/HyperOS WorkManager Assassination (The Battery Killer)

### 🩺 The Symptom
On the physical test device (Xiaomi running HyperOS), the `StatusCheckWorker` was being killed at the OS level — completely removed from the JobScheduler. This wasn't a crash or a retry; the work was simply GONE. The Background Task Inspector showed no trace of the worker ever being scheduled.

This made it impossible to validate the notification infrastructure because we couldn't get the Worker to execute on the device at all.

### 🕵️‍♂️ Root Cause
Xiaomi/HyperOS implements extremely aggressive battery optimization that goes far beyond stock Android's Doze mode. The OS preemptively kills background work from apps it doesn't recognize as "important" (i.e., not a major social media or messaging app). This affects:

- `JobScheduler` jobs (which WorkManager uses under the hood)
- `AlarmManager` exact alarms
- Foreground services (in some cases)

The worker was being killed before it even had a chance to execute, regardless of backoff policies or constraints.

### 🛠️ The Solution
1. Created a **"TEST PUSH (DEBUG)" button** on DashboardScreen that bypassed WorkManager entirely and called `NotificationHelper` directly. This isolated the notification infrastructure from the scheduling mechanism.
2. The button allowed direct validation of: notification channel creation, notification building, permission handling, and visual appearance — all without relying on WorkManager.
3. Documented the Xiaomi battery optimization behavior for the team, including workarounds (disable battery optimization for the app, add app to "autostart" whitelist).
4. Added `EXPONENTIAL` backoff with 30-second initial delay as a best-effort retry mechanism.

### 🧠 Engram (Lesson for the Future)
> **On Xiaomi/HyperOS (and Samsung devices to a lesser extent), never assume WorkManager will execute reliably.** Design features so the core logic can be tested independently of the scheduling mechanism. For B2B apps deployed on specific hardware, document OEM-specific battery optimization workarounds and consider requesting `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` during onboarding.

---

## 🐛 4. Auto Backup Phantom State (The Clean Install Lie)

### 🩺 The Symptom
After uninstalling and reinstalling the app for clean testing, the WorkManager state from the previous installation persisted. This invalidated our test results because we thought we were testing a fresh install, but the system was restoring the previous WorkManager schedule.

### 🕵️‍♂️ Root Cause
Android's Auto Backup feature (`android:allowBackup="true"` by default) automatically backs up app data to the user's Google account. When the app is reinstalled, this data is restored — including:
- Room database contents
- SharedPreferences
- WorkManager state and scheduled work
- Notification channels (sometimes)

This meant every "clean install" was actually a restored state, making it impossible to test the true first-launch experience.

### 🛠️ The Solution
1. Set `android:allowBackup="false"` and `android:fullBackupContent="false"` in AndroidManifest.xml
2. This ensures every uninstall/reinstall cycle starts with a completely blank slate
3. The setting will be kept as `false` for the testing phase on Decathlon devices

### 🧠 Engram (Lesson for the Future)
> **When testing background scheduling or first-launch flows, ALWAYS disable Auto Backup.** The default `allowBackup="true"` will silently restore previous state across reinstalls, making "clean install" tests completely unreliable. Add this to your testing checklist: verify `allowBackup="false"` before any scheduling validation.

---

## 🐛 5. The Supabase Dependency Hell (The 28 AAR Metadata Errors)

### 🩺 The Symptom
After adding Supabase BOM 3.5.0 to the project, the build failed with 28 AAR metadata errors. Dependencies like `androidx.browser:browser:1.9.0`, `androidx.compose.foundation:1.9.0`, and `androidx.lifecycle:2.10.0` demanded `compileSdk 35/36` and `AGP 8.6.0+/8.9.1+` — but the project was on `compileSdk 34` and `AGP 8.2.2`. Attempting a Compose BOM containment wall (`2024.02.00`) before the Supabase BOM didn't fix it because the transitive dependencies were too deeply pulled.

### 🕵️‍♂️ Root Cause
Supabase BOM 3.5.0 pulls transitive dependencies that are incompatible with AGP 8.2.2. The BOM acts as a version catalog — ALL modules under it get resolved to the latest versions, including transitive AndroidX dependencies that require newer build tools. The containment wall approach (forcing Compose BOM before Supabase BOM) failed because the conflict wasn't limited to Compose — it affected `browser`, `lifecycle`, `core-ktx`, and other AndroidX libraries.

### 🛠️ The Solution
1. **Downgraded Supabase BOM** from 3.5.0 to 2.6.1 (stable 2.x branch compatible with AGP 8.2.2 + compileSdk 34)
2. Downgraded Ktor CIO from 3.4.0 to 2.3.12 (matching 2.x series)
3. Changed artifact name from `auth-kt` to `gotrue-kt` (2.x naming)
4. Kept `compileSdk = 34` and `AGP 8.2.2` unchanged
5. Documented the exact API differences between Supabase 2.x and 3.x

### 🧠 Engram (Lesson for the Future)
> **Never assume the latest BOM version is compatible with your build tools.** When adding a major SDK to an existing project, ALWAYS check the transitive dependency tree first: `./gradlew app:dependencies --configuration debugRuntimeClasspath`. If the BOM demands a newer compileSdk or AGP than your project supports, downgrade the BOM — don't upgrade your build tools for a single dependency. The 2.x branch is often more stable for production use anyway.

---

## 🐛 6. The Supabase 2.x API Naming Maze (Gotrue vs Auth)

### 🩺 The Symptom
Compilation errors like `Unresolved reference 'gotrue'` and `Unresolved reference 'auth'` when trying to access Supabase's authentication module. The documentation showed conflicting examples — some used `supabaseClient.gotrue.loginWith()`, others used `supabase.auth.signInWith()`. Neither worked with BOM 2.6.1.

### 🕵️‍♂️ Root Cause
Supabase Kotlin SDK has confusing naming conventions that differ between versions:
- **2.x**: Artifact is `gotrue-kt`, package is `io.github.jan.supabase.gotrue`, extension property is `auth`, login method is `signInWith(Email)`
- **3.x**: Artifact is `auth-kt`, package is `io.github.jan.supabase.auth`, extension property is `auth`, login method is `signInWith(Email)`

The extension property is ALWAYS `auth` regardless of version, but the PACKAGE changes between `gotrue` and `auth`. The wiki documentation showed `gotrue.loginWith()` which was an older API that no longer exists. Only by inspecting the actual JAR source code could we determine the correct imports.

### 🛠️ The Solution
1. Inspected the JAR contents directly: `jar tf gotrue-kt-2.6.1-sources.jar`
2. Read `Auth.kt` source from the JAR to find the extension property: `val SupabaseClient.auth: Auth`
3. Confirmed correct imports for BOM 2.6.1:
   - `import io.github.jan.supabase.gotrue.auth` (extension property)
   - `import io.github.jan.supabase.gotrue.Auth` (plugin class)
   - `import io.github.jan.supabase.gotrue.providers.builtin.Email` (email provider)
4. Documented the full API reference for future use

### 🧠 Engram (Lesson for the Future)
> **When third-party SDK documentation is unclear or conflicting, go straight to the source JAR.** Run `jar tf artifact-sources.jar` to list files, then `unzip -p` to read the actual Kotlin source. This is the ONLY reliable way to determine correct imports, method signatures, and extension properties. Never trust wiki examples or Context7 docs blindly for version-specific APIs.

---

## 🐛 7. The Silent Sync Worker (Timber Not Planted)

### 🩺 The Symptom
The SyncWorker reported SUCCESS in the Background Task Inspector, but no data appeared in Supabase. More critically, NONE of the debug logs (`SYNC_SUCCESS`, `SYNC_ERROR`) appeared in Logcat. The worker appeared to run successfully but produced zero output.

### 🕵️‍♂️ Root Cause
Timber logging library was used throughout the sync pipeline (`Timber.d()`, `Timber.e()`) but `Timber.plant(Timber.DebugTree())` was never called in `SmartNutriStockApp.onCreate()`. Without a planted tree, ALL Timber calls are no-ops — they execute but produce zero output. This made it impossible to distinguish between "the worker ran but found nothing" and "the worker didn't run at all".

### 🛠️ The Solution
Added `Timber.plant(Timber.DebugTree())` as the first instruction in `SmartNutriStockApp.onCreate()`, wrapped in `if (BuildConfig.DEBUG)` for safety:
```kotlin
override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
    }
    // ... rest of initialization
}
```

### 🧠 Engram (Lesson for the Future)
> **Before relying on any logging framework, verify its initialization.** Timber requires an explicit `plant()` call — without it, all log calls silently vanish. This should be the FIRST thing checked when debugging "silent failures". Add Timber planting to your project bootstrap checklist, right after `super.onCreate()`.

---

## 🐛 8. The SessionManager Naming Collision (Hilt Dependency Confusion)

### 🩺 The Symptom
The SyncWorker injected via `@HiltWorker` couldn't resolve its dependencies correctly. Hilt was confused about which `SessionManager` to provide — our custom `EncryptedSharedPreferences` implementation or Supabase's built-in `SessionManager` interface.

### 🕵️‍♂️ Root Cause
Our `SessionManager` class implemented Supabase's `io.github.jan.supabase.gotrue.SessionManager` interface. Both had the same simple class name `SessionManager`. Hilt's dependency resolution couldn't distinguish between our implementation class and the interface type when injecting into workers, repositories, and viewmodels.

### 🛠️ The Solution
1. Renamed our class from `SessionManager` to `EncryptedSessionManager`
2. Created a typealias `typealias SupabaseSessionManager = io.github.jan.supabase.gotrue.SessionManager` for clarity
3. Updated ALL 7 files that referenced the old name
4. Verified zero residual references with `grep -r "SessionManager" --include="*.kt"`

### 🧠 Engram (Lesson for the Future)
> **When implementing a third-party interface, NEVER use the same class name.** Prefix with your implementation detail: `EncryptedSessionManager`, `CachedUserRepository`, `LocalDataStore`. This avoids Hilt/Dagger confusion, makes imports unambiguous, and prevents future maintainers from mixing up the interface with the implementation.

---

## 🐛 9. The storeId Literal Quotes Bug ("1620" vs 1620)

### 🩺 The Symptom
The SyncWorker found 0 dirty records to push to Supabase, despite the Database Inspector showing records with `isDirty = 1` and `storeId = "1620"`. The query `getDirtyRecords(storeId = "1620")` returned an empty list every time.

### 🕵️‍♂️ Root Cause
In `EncryptedSessionManager.saveSession()`, the storeId was extracted from Supabase's JWT user metadata using `.toString()` on a `JsonElement`:
```kotlin
// BUG: .toString() on JsonElement returns "\"1620\"" with literal quotes
val storeId = user.userMetadata?.get("store_id")?.toString()
```
This produced `"\"1620\""` (a string containing literal quote characters) instead of `"1620"`. The quoted value propagated through SharedPreferences → getStoreId() → Batch.toEntity() → Room database. When the DAO queried `WHERE storeId = '1620'`, it found nothing because Room had `"\"1620\""` stored.

### 🛠️ The Solution
1. Changed extraction to use `.jsonPrimitive.content`:
```kotlin
val storeId = user.userMetadata?.get("store_id")?.jsonPrimitive?.content ?: "1620"
```
2. Added defensive sanitization in `getStoreId()`: `.trim('"')`
3. Created `StockDao.sanitizeStoreIds()` to clean existing corrupted records:
```sql
UPDATE active_stocks SET storeId = REPLACE(storeId, '"', '') WHERE storeId LIKE '%"%'
```
4. Called sanitizer in `SyncRepositoryImpl.pushDirtyRecords()` before querying dirty records

### 🧠 Engram (Lesson for the Future)
> **ALWAYS use `.jsonPrimitive.content` to extract strings from kotlinx.serialization JsonElement.** NEVER use `.toString()` — it wraps the value in JSON quotes, producing `"\"value\""` instead of `"value"`. This is a silent data corruption bug that propagates through your entire app. Apply `.trim('"')` defensively as a safety net in getters.

---

## 🐛 10. The Supabase Schema Mismatch (The Phantom Columns)

### 🩺 The Symptom
After fixing the storeId quotes, dirty records were found and the upsert was attempted. But Supabase rejected the request with: `Could not find the 'updated_at' column of 'active_stocks' in the schema cache`. The DTO was sending columns that didn't exist in the real Supabase table.

### 🕵️‍♂️ Root Cause
The `SupabaseActiveStock` DTO was designed based on the Room schema (which has `updated_at`, `deleted_at`, `device_id`) without verifying the actual Supabase table structure. The real table had different columns:
- **Missing in Supabase**: `updated_at`, `deleted_at`, `device_id`
- **Present in Supabase but missing in DTO**: `product_name`, `status`
- **Type mismatch**: `is_dirty` was `Boolean` in DTO but `integer` in Supabase

### 🛠️ The Solution
1. Queried Supabase schema: `SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'active_stocks'`
2. Aligned DTO with real schema:
   - Removed: `updated_at`, `deleted_at`, `device_id`
   - Added: `product_name` (text NOT NULL), `status` (text NOT NULL)
   - Changed: `is_dirty` from Boolean to Int
3. Injected `ProductCatalogDao` into `SyncRepositoryImpl` to resolve `product_name` from the catalog
4. Changed conflict resolution from `updatedAt` (non-existent) to `version` (exists in both)
5. Updated both `toSupabaseFormat()` and `toEntity()` mappers

### 🧠 Engram (Lesson for the Future)
> **ALWAYS verify the target database schema BEFORE writing DTOs.** Run `information_schema.columns` query on the remote database first, then design your DTO to match exactly. Assumption-driven DTO design is a guaranteed source of runtime errors. Keep a schema reference document alongside your DTO definitions.

---

## 🐛 11. DEX Format Rejects Backtick Function Names with Spaces

### 🩺 The Symptom
Test function names using backticks with spaces (e.g., `fun "test name with spaces"()`) compiled fine in JVM tests but failed in Android instrumented tests with: `Space characters in SimpleName are not allowed prior to DEX version 040`. This made tests pass in local unit tests (`app/src/test`) but crash during CI or when running on physical devices (`app/src/androidTest`).

### 🕵️‍♂️ Root Cause
DEX format (Dalvik Executable) doesn't support spaces in identifiers, while JVM bytecode does. The Kotlin compiler allows backtick-wrapped function names for readability, but the DEX converter (`dx` or `d8`) rejects them during Android build. This is a fundamental difference between JVM bytecode and Android's DEX format.

### 🛠️ The Solution
1. Replaced ALL backtick-wrapped function names with underscores across affected files
2. StockDaoSyncTest.kt: 10 methods renamed from `"test case description"` to `test_case_description`
3. NotificationPermissionHandlerTest.kt: 20 methods renamed similarly
4. Verified tests compile and run in both `test` and `androidTest` sourcesets

### 🧠 Engram (Lesson for the Future)
> **NEVER use backtick function names in `androidTest/`.** They work perfectly in `app/src/test/` (JVM tests) but crash the DEX converter in instrumented tests. This is a silent build failure that only reveals itself when running on Android. Use underscores exclusively for test function names in `androidTest/` sourcesets. Check your test suite with `./gradlew connectedAndroidTest` early in development to catch this before CI.

---

## 🐛 12. HyperOS/Xiaomi Blocks Test APK Installation (INSTALL_FAILED_USER_RESTRICTED)

### 🩺 The Symptom
Running `connectedAndroidTest` on Xiaomi 14T failed consistently with: `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`. The Gradle task would reach 80% progress, then crash with this error despite having USB debugging enabled. This made it impossible to run any instrumented tests on the primary test device.

### 🕵️‍♂️ Root Cause
HyperOS (Xiaomi's Android skin) implements aggressive security that requires explicit user approval for EACH APK installation via USB. When `adb install` triggers, a popup appears on the phone with a 10-second timeout. If the user doesn't tap "Install" within 10 seconds, the system auto-rejects the installation. This happens EVERY time Gradle installs a new test APK, which can be multiple times per test run (APK splits, test APK, instrumentation APK).

### 🛠️ The Solution
1. Enabled "Install via USB" in Developer Options (this shows the popup)
2. Enabled "USB debugging (security settings)" in Developer Options (this allows auto-approve)
3. Keep the phone screen UNLOCKED during test runs
4. Watch for the popup and tap "Install" immediately — you get 10 seconds, not more
5. Consider running all tests in a single Gradle invocation (`./gradlew connectedAndroidTest`) to minimize popup count vs running individual tests repeatedly

### 🧠 Engram (Lesson for the Future)
> **When testing on Xiaomi/HyperOS devices, ALWAYS keep the screen unlocked and watch for installation prompts.** The 10-second timeout is non-negotiable and auto-rejects without user action. This is an OEM security feature, not a bug. Consider maintaining a dedicated test device without OEM restrictions for CI/CD, or budget extra time for manual popup approval during Xiaomi testing. Document this in your device testing playbook.

---

## 🐛 13. Room Migration Index Names Can Differ from Column Names

### 🩺 The Symptom
Migration5To6Test failed when asserting index names. The test expected index names to match column names (e.g., `index_active_stocks_userId`), but the actual CREATE INDEX statements used snake_case names (`index_active_stocks_user_id`). This caused test failures even though the migration itself worked correctly.

### 🕵️‍♂️ Root Cause
Index names are explicitly specified in CREATE INDEX SQL statements and are COMPLETELY INDEPENDENT of column naming conventions. The codebase uses camelCase for columns (`userId`, `createdAt`) but the migration SQL author chose snake_case for index names (`user_id`, `created_at`). The test assumed Room would auto-generate index names based on column names, but Room uses whatever name is explicitly provided in the migration SQL.

### 🛠️ The Solution
1. Read the actual migration SQL file (`DatabaseMigration.kt`) to verify CREATE INDEX statements
2. Aligned test assertions with the actual index names from migration SQL (snake_case)
3. Updated Migration5To6Test to assert: `index_active_stocks_user_id` instead of `index_active_stocks_userId`
4. Documented the index naming convention in the migration test docstring

### 🧠 Engram (Lesson for the Future)
> **When testing Room migrations, ALWAYS verify index names against the actual CREATE INDEX statements, NOT inferred from column names.** Room does not auto-generate index names from column conventions — it uses whatever you explicitly write in migration SQL. The test should validate the migration AS WRITTEN, not as you assume it should be. Keep a reference list of actual index names from your migration files to avoid test assertion errors.

---

## 🐛 14. Compose Test Ambiguity When Same Text Appears Multiple Times

### 🩺 The Symptom
LoginScreen had "Iniciar Sesión" as both a subtitle Text component AND a Button content label. Tests using `onNodeWithText("Iniciar Sesión")` failed with: `Multiple nodes found that match this selector: Found 2 nodes`. The test framework couldn't distinguish which UI element to interact with, causing all login tests to crash.

### 🕵️‍♂️ Root Cause
Compose semantics merge text from child nodes into the parent's semantics tree. When the same text string appears in multiple places (subtitle vs button), `onNodeWithText()` matches ALL of them. Compose requires explicit test tags to disambiguate elements when text isn't unique. This is by design — Compose wants you to tag interactive elements explicitly for reliable testing.

### 🛠️ The Solution
1. Added `.testTag("login_button")` to the Login Button
2. Added `.testTag("login_subtitle")` to the subtitle Text
3. Changed tests from `onNodeWithText("Iniciar Sesión")` to `onNodeWithTag("login_button")`
4. Updated all login screen tests to use tag-based selectors for disambiguation

### 🧠 Engram (Lesson for the Future)
> **ALWAYS add testTag to interactive elements (buttons, text fields) in Compose UI when text might repeat.** Use `onNodeWithTag()` for reliable test selectors instead of `onNodeWithText()`. Text-based selectors are fragile and ambiguous — tag-based selectors are explicit and deterministic. Make testTag assignment a part of your Compose component checklist: if it's interactive (button, input), it needs a tag. This prevents entire test suites from breaking due to text duplication.

---

## 🐛 15. Compose setContent Can Only Be Called Once Per Test

### 🩺 The Symptom
A test using `forEach` to iterate over error types attempted to call `composeTestRule.setContent()` inside the loop. This crashed with: `Cannot call setContent twice per test!` The test was trying to verify multiple error scenarios (network error, invalid credentials, etc.) by reusing the same test method with different error states.

### 🕵️‍♂️ Root Cause
`AndroidComposeTestRule` restricts `setContent()` to exactly ONE call per test method lifecycle. The Compose test framework establishes the composition once at the start of the test and expects it to persist until `onActivity()` destroys it. Calling `setContent()` multiple times attempts to re-establish the composition, which violates the test rule's invariant.

### 🛠️ The Solution
1. Removed the `forEach` loop from the test method
2. Created separate test methods for each error case: `testLogin_showNetworkError()`, `testLogin_showInvalidCredentialsError()`, `testLogin_showGenericError()`
3. Each test method calls `setContent()` exactly once
4. Considered using `@ParameterizedTest` for future parametrized scenarios (requires additional setup)

### 🧠 Engram (Lesson for the Future)
> **In Compose UI tests, each test method gets ONE setContent call.** Don't try to loop through scenarios with multiple `setContent()` invocations — the framework will crash. Use separate test methods for each scenario or invest in `@ParameterizedTest` infrastructure if you have many similar cases. The tradeoff is more test methods, but each test is clearer and more isolated. This is a framework constraint, not a bug — respect the one-composition-per-test lifecycle.

---

## 🐛 16. Value-Controlled OutlinedTextField Can't Be Verified via Text Content in Tests

### 🩺 The Symptom
Tests tried to verify email input by checking `onNodeWithText("test@decathlon.com")` after calling `performTextInput("test@decathlon.com")`. The assertion failed because the field was empty. The test appeared to type the text but the field showed nothing.

### 🕵️‍♂️ Root Cause
The OutlinedTextField is value-controlled (state comes from ViewModel). In tests with mocked ViewModels, the UI state is FROZEN — the mock returns empty strings (`""`) regardless of what `onValueChange` is called with. When `performTextInput()` types text, it triggers the `onValueChange` callback, but the mock doesn't update its return value, so the UI doesn't re-render with the typed text. The UI and test are out of sync because the mock doesn't emulate state transitions.

### 🛠️ The Solution
1. Changed test strategy from verifying VISUAL STATE to verifying INTERACTIONS
2. Instead of asserting `onNodeWithText("test@decathlon.com")`, verify the ViewModel method was called:
   ```kotlin
   verify { mockViewModel.onEmailChange("test@decathlon.com") }
   ```
3. This validates that the Compose UI correctly triggers the callback without relying on visual updates
4. If visual verification is needed, use `StateFlow.collectAsState()` with a mutable state variable in tests, not mocks

### 🧠 Engram (Lesson for the Future)
> **When testing Compose UIs with mocked ViewModels, verify INTERACTIONS (ViewModel method calls) not VISUAL STATE (displayed text).** The mock controls state, so visual updates won't reflect unless the mock is set up to return specific values. Use `verify()` assertions on ViewModel methods to prove the UI triggers the right callbacks. If you need to test visual state, don't mock — use a real ViewModel or a test double that actually holds state. The interaction-first approach is cleaner and more reliable for component-level tests.

---

## 🚀 Architectural Conclusion

Overcoming these 10 challenges has demonstrated that **Clean Architecture** isn't just about code organization — it's about **resilience**. By separating concerns into distinct layers (Domain, Data, Presentation), we were able to:

- **Isolate failures**: When the notification didn't appear, we could test the Domain logic (status calculation) independently from the Data layer (notification building) and the Presentation layer (permission UI)
- **Debug systematically**: The debug test button bypassed WorkManager entirely, proving the notification infrastructure worked while revealing the scheduling was the issue
- **Adapt to OEM quirks**: The smart enqueue policy handles Samsung and Xiaomi's aggressive battery optimization without requiring per-device code

The sync pipeline challenges (bugs 5–10) reinforced this resilience from a different angle:

- **Logging infrastructure is non-negotiable**: Bug 7 (Timber not planted) showed that without visible logs, debugging becomes guesswork. A properly initialized logging framework is the FIRST line of defense — without it, silent failures are invisible
- **Debug buttons save hours**: The "TEST PUSH (DEBUG)" and "FORZAR SYNC" buttons on the Dashboard allowed us to bypass scheduling and test the sync pipeline directly, isolating whether the problem was in the worker scheduling or the sync logic itself
- **Third-party SDK version compatibility is a critical risk**: Bugs 5 and 6 demonstrated that adding Supabase to an existing project required careful BOM version selection and source JAR inspection. The latest version isn't always compatible — and documentation can't always be trusted
- **Data integrity requires defensive serialization**: Bug 9 (storeId quotes) was a silent data corruption that propagated through SharedPreferences, Room, and into the sync query. Using `.jsonPrimitive.content` instead of `.toString()` and adding `.trim('"')` as a safety net prevents an entire class of bugs
- **Schema verification is a prerequisite, not an afterthought**: Bug 10 proved that designing DTOs based on assumptions (matching the local Room schema) instead of verifying the remote database schema leads to guaranteed runtime failures. Always query the target schema first
- **Naming collisions with third-party interfaces create subtle DI bugs**: Bug 8 showed that sharing a class name with a library interface confuses Hilt's dependency resolution. Prefixing implementations with their distinguishing characteristic (`EncryptedSessionManager`) prevents this entire category of issues

The key takeaway: **Build your architecture so that each layer can be tested independently.** When a Xiaomi device kills your Worker at 06:00 AM, you need to know immediately whether the problem is the Worker, the notification, the permission, or the channel registration. When Supabase rejects your sync, you need to know whether it's the storeId, the schema, the auth, or the network. Clean Architecture gave us that diagnostic precision across all 10 challenges.
