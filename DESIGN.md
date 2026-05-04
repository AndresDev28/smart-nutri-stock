# Design: Premium UI Design - Industrial Elegance

## Technical Approach

Implement a premium "Industrial Elegance" theme system using Material 3 as foundation with custom design tokens (colors, typography, shapes, components) aligned with Decathlon's "Inventario Vivo" design philosophy. The migration follows an incremental 4-phase approach: foundation → signature components → screen updates → polish. All state logic (semaphore status, counters) is consumed from domain use cases (CalculateStatusUseCase) — composables RENDER state, they do NOT calculate it.

## Architecture Decisions

### Decision: Theme Foundation Architecture

**Choice**: Custom Material 3 lightColorScheme with fixed brand colors (dynamic color DISABLED), Shape.kt for organic geometry, Type.kt with Plus Jakarta Sans + JetBrains Mono fonts.

**Alternatives considered**:
- Keep default Material 3 theme with purple colors (rejected — lacks visual identity)
- Enable dynamic color on Android 12+ (rejected — conflicts with fixed "Inventario Vivo" palette)
- Use third-party design system library (rejected — Material 3 1.2.0 is sufficient)

**Rationale**: Material 3 provides the component system we need (scaffold, buttons, cards, typography) while custom color/shape/type tokens give us the premium visual identity. Disabling dynamic color ensures consistent branding across all devices, which is critical for a warehouse app used on enterprise XCover7 devices. The shape system (24dp cards, 12dp buttons, 8dp small elements) provides organic geometry that feels premium and industrial.

### Decision: State Logic SSOT (Single Source of Truth)

**Choice**: All semaphore status and counter state is calculated by CalculateStatusUseCase in domain layer and passed to UI composables as parameters.

**Alternatives considered**:
- Calculate status directly in composables (rejected — violates Clean Architecture, mixes business logic with UI)
- Cache status in ViewModel (rejected — status must be real-time based on current time, not cached)

**Rationale**: Clean Architecture requires business logic to live in domain layer. CalculateStatusUseCase provides the authoritative status calculation based on expiry date. Composables (NutriCard, Dashboard counters) are pure functions that RENDER the status passed to them — they never calculate it. This separation ensures testability and correctness: status logic is tested in domain layer (CalculateStatusUseCaseTest), UI tests only verify rendering.

### Decision: Empty State Pattern

**Choice**: EmptyStateComponent with Barcode Leaf illustration + motivational message "Listo para la recepción de hoy" for 0 products state.

**Alternatives considered**:
- Show blank screen (rejected — confusing, feels broken)
- Show error message (rejected — incorrect, 0 products is valid state)
- Show placeholder with generic text (rejected — misses opportunity for motivational UX)

**Rationale**: First-use experience is critical. The app fills up "as you scan" — day one is empty and must feel inviting, not broken. Barcode Leaf illustration creates visual connection to scanning. Motivational message frames empty state as opportunity rather than lack of data. This pattern is reusable across Dashboard and History screens.

### Decision: Quick Scan Button Epicenter

**Choice**: PremiumButton (RoyalBluePrimary, 48dp minimum, 12dp radius) placed in bottom safe zone with visual dominance (large size, primary color, shadow) as the ONLY critical entry point.

**Alternatives considered**:
- Add action buttons to TopAppBar (rejected — XCover7 physical button assigned to other app, hard to reach)
- Use FAB (FloatingActionButton) (rejected — Material 2 pattern, not premium enough, conflicts with bottom sheet in scanner)
- Multiple entry points (rejected — XCover7 ergonomics require single thumb-zone action)

**Rationale**: Samsung XCover7 has physical side button assigned to another app. Bottom third is thumb-reachable zone. PremiumButton with RoyalBluePrimary (#2563EB) provides strong visual hierarchy (primary action). 48dp minimum ensures reliable touch targets. Bottom safe zone placement enables one-handed operation. Visual dominance communicates this is THE critical action.

### Decision: LazyColumn with Keys for Performance

**Choice**: Dashboard and History screens use LazyColumn with `key(batchId)` parameter for stable item identity during state changes.

**Alternatives considered**:
- LazyColumn without keys (rejected — poor performance with thousands of items, janky recomposition)
- LazyRow (rejected — list format better for inventory data)
- Vertical scroll Column (rejected — renders all items at once, performance catastrophe)

**Rationale**: Database is cumulative (0 to thousands of products). LazyColumn renders only visible items, enabling smooth scrolling. Key parameter ensures Compose tracks items correctly during updates (inserts, deletes, status changes). This is critical for performance: status changes daily (semaphore recalculation), and without keys Compose would recompose all items on every state change.

## Data Flow

```
    CalculateStatusUseCase (Domain)
              ↓
         SemaphoreStatus (GREEN/YELLOW/EXPIRED)
              ↓
    DashboardViewModel / HistoryViewModel
              ↓
         SemaphoreCounters (state)
              ↓
    DashboardScreen / HistoryScreen (UI)
              ↓
    NutriCard (renders status from param)
```

**State Flow**: Domain layer calculates status → ViewModel exposes StateFlow → Screen collects state → NutriCard renders status. No backward data flow (UI never calculates status).

**Camera Flow**: ScannerScreen launches camera → User scans EAN → ML Kit returns result → ProductRegistrationBottomSheet shows confirmation → UpsertStockUseCase upserts to database → CalculateStatusUseCase recalculates status → Dashboard updates with new semaphore counters.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `presentation/ui/theme/Color.kt` | Rewrite | Replace purple scheme with "Inventario Vivo" palette (CreamBackground #F8FAFC, RoyalBluePrimary #2563EB, ShadowColor #000000@8%, etc.). Status colors as ColorScheme extension properties (statusTeal, statusAmber, statusDeepRed). |
| `presentation/ui/theme/Type.kt` | Modified | Add Plus Jakarta Sans (Bold, Medium, Regular) for UI text, JetBrains Mono (Medium) for EAN/data codes |
| `presentation/ui/theme/Shape.kt` | New | Create shape tokens: 8dp (small), 12dp (medium), 24dp (large), soft shadows (6-8% alpha) |
| `presentation/ui/theme/Theme.kt` | Modified | Update to use new color scheme, disable dynamic color, add statusBar color sync |
| `presentation/ui/components/` | New | Create signature component library package structure |
| `presentation/ui/components/NutriCard.kt` | New | Signature card with 6dp status line, 24dp radius, shadow, text hierarchy, status circle, EAN in JetBrains Mono |
| `presentation/ui/components/InfoChip.kt` | New | Reusable chip for quantity/date display (12dp radius, subtle border) |
| `presentation/ui/components/PremiumButton.kt` | New | RoyalBluePrimary button, 48dp minimum, 12dp radius, 300ms color transitions |
| `presentation/ui/components/ShimmerLoading.kt` | New | Shimmer effect for async loading from Supabase (infinite animation, gradient sweep) |
| `presentation/ui/components/EmptyState.kt` | New | Barcode Leaf illustration + motivational message pattern |
| `presentation/ui/components/StatusCircle.kt` | New | Circular status indicator with 0.1f alpha background (8dp diameter) |
| `presentation/ui/dashboard/DashboardScreen.kt` | Modified | Implement Premium Dashboard pattern: header (logo + greeting), Summary Cards (3 counters with elevation), Quick Scan epicenter button, LazyColumn with NutriCards, EmptyState for 0 products |
| `presentation/ui/scanner/ScannerScreen.kt` | Modified | Add ImmersiveScanner (blur overlay via Modifier.blur() with API 31+ guardrail), Bottom Sheet confirmation (40% screen coverage), haptic feedback (LocalHapticFeedback.current) |
| `presentation/ui/history/HistoryScreen.kt` | Modified | Replace basic cards with NutriCard, use LazyColumn with keys, EmptyState for 0 products, use theme colors (remove hardcoded semaphore values) |
| `presentation/auth/LoginScreen.kt` | Modified | Update button to PremiumButton pattern, typography to Plus Jakarta Sans, spacing alignment |
| `res/font/plus_jakarta_sans_bold.ttf` | New | Plus Jakarta Sans Bold font for headlines |
| `res/font/plus_jakarta_sans_medium.ttf` | New | Plus Jakarta Sans Medium font for titles |
| `res/font/plus_jakarta_sans_regular.ttf` | New | Plus Jakarta Sans Regular font for body text |
| `res/font/jetbrains_mono_medium.ttf` | New | JetBrains Mono Medium font for EAN codes and data |
| `res/drawable/ic_barcode_leaf_logo.xml` | New | "Barcode Leaf" logo vector drawable for screens |

## Interfaces / Contracts

### Color.kt (Theme System)

```kotlin
package com.decathlon.smartnutristock.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Inventario Vivo" Palette - Fixed brand colors (dynamic color DISABLED)
val CreamBackground = Color(0xFFF8FAFC)
val Surface = Color(0xFFFFFFFF)
val RoyalBluePrimary = Color(0xFF2563EB)
val RoyalBlueDark = Color(0xFF1E40AF)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)

// Status colors - private base values
private val _StatusTeal = Color(0xFF14B8A6)     // GREEN (>7 days)
private val _StatusAmber = Color(0xFFF59E0B)    // YELLOW (1-7 days)
private val _StatusDeepRed = Color(0xFFDC2626)  // EXPIRED (≤0 days)

// Extension properties on ColorScheme
@Composable
val ColorScheme.statusTeal: Color get() = _StatusTeal

@Composable
val ColorScheme.statusAmber: Color get() = _StatusAmber

@Composable
val ColorScheme.statusDeepRed: Color get() = _StatusDeepRed

// Custom shadow color for Industrial Elegance look
val ShadowColor = Color(0xFF000000).copy(alpha = 0.08f)

val SuccessGreen = Color(0xFF10B981)
```

### Type.kt (Typography)

```kotlin
package com.decathlon.smartnutristock.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMono,  // EAN codes
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
```

### Shape.kt (Organic Geometry)

```kotlin
package com.decathlon.smartnutristock.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // Chips, tags
    small = RoundedCornerShape(8.dp),         // Small elements
    medium = RoundedCornerShape(12.dp),       // Buttons
    large = RoundedCornerShape(24.dp),        // Cards, NutriCard
    extraLarge = RoundedCornerShape(28.dp)    // Bottom sheets
)
```

### NutriCard.kt (Signature Component)

```kotlin
@Composable
fun NutriCard(
    productName: String,
    ean: String,
    quantity: Int,
    expiryDate: String,
    status: SemaphoreStatus,  // From CalculateStatusUseCase (SSOT)
    onClick: () -> Unit
) {
    val statusColor = when (status) {
        SemaphoreStatus.GREEN -> MaterialTheme.colorScheme.statusTeal
        SemaphoreStatus.YELLOW -> MaterialTheme.colorScheme.statusAmber
        SemaphoreStatus.EXPIRED -> MaterialTheme.colorScheme.statusDeepRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = MaterialTheme.shapes.large,
                ambientColor = ShadowColor,  // Custom token for Industrial Elegance
                spotColor = ShadowColor
            ),
        shape = MaterialTheme.shapes.large,  // 24dp radius
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,  // Shadow handled by Modifier.shadow
            pressedElevation = 0.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 6dp status line at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(statusColor)  // Status color from MaterialTheme.colorScheme
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Product name (Plus Jakarta Sans Bold)
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // EAN (JetBrains Mono Medium)
                Text(
                    text = ean,
                    style = MaterialTheme.typography.labelMedium,  // JetBrains Mono
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    // Quantity chip
                    InfoChip(label = "Cantidad", value = quantity.toString())

                    // Expiry date chip
                    InfoChip(label = "Caducidad", value = expiryDate)

                    // Status circle
                    StatusCircle(status = status)
                }
            }
        }
    }
}
```

### PremiumButton.kt (Epicenter Action)

```kotlin
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (enabled)
            MaterialTheme.colorScheme.royalBluePrimary
        else
            MaterialTheme.colorScheme.textSecondary.copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp),  // XCover7 thumb zone optimized
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = MaterialTheme.shapes.medium,  // 12dp radius
        enabled = enabled
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
```

## Technical Constraints

### Scanner Blur Overlay (API Guardrail)

**IMPORTANT:** `Modifier.blur()` requires API level 31+ (Android 12+). The app targets minSdk 26, so always wrap blur modifier with version check and fallback for older devices:

```kotlin
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// IMPORTANT: Modifier.blur() requires API 31+
// Always wrap in version check with fallback
val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    Modifier.blur(20.dp)
} else {
    Modifier.background(Color.Black.copy(alpha = 0.4f)) // Fallback for older devices
}
```

**Reason:** `Modifier.blur()` is API 31+ only. The Samsung XCover7 runs Android 14 (API 34), but minSdk 26 supports older devices. The fallback ensures visual consistency (dark overlay) across all supported API levels while providing blur enhancement on supported devices.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Color values, font loading, shape tokens | Write tests for Color.kt, Type.kt, Shape.kt verifying exact hex values and font families |
| Unit | NutriCard component | Compose test verifying NutriCard renders correctly for all 3 statuses (GREEN/YELLOW/EXPIRED) with correct status line color |
| Unit | PremiumButton component | Compose test verifying 300ms animation, 48dp minimum height, RoyalBluePrimary color |
| Unit | EmptyState component | Compose test verifying Barcode Leaf illustration renders and motivational message displays |
| Integration | Dashboard + NutriCard | Compose test verifying Dashboard renders NutriCard list with keys, EmptyState for 0 products |
| Integration | Scanner + BottomSheet | Compose test verifying blur overlay effect, 40% bottom sheet coverage, haptic feedback |
| Integration | History + NutriCard | Compose test verifying History renders NutriCard with LazyColumn keys, filters work correctly |
| Manual | XCover7 ergonomics | Physical device test: 48dp tap targets reachable, Quick Scan button in bottom third, haptic feedback timing |
| Performance | LazyColumn with thousands | Instrumented test verifying smooth scrolling with 1000+ items using `key(batchId)` |

**Coverage Target**: 90% domain layer (existing CalculateStatusUseCaseTest), 80% UI layer (NutriCard, PremiumButton, EmptyState components).

## Migration / Rollout

### Phase-by-Phase Rollout (4 Phases)

**Phase 1: Theme Foundation** (Week 1)
1. Download fonts to `res/font/` directory
2. Create font resource XMLs in `res/font/` (if needed for font loading)
3. Rewrite `Color.kt` with "Inventario Vivo" palette (10 colors)
4. Modify `Type.kt` to add Plus Jakarta Sans + JetBrains Mono
5. Create `Shape.kt` with 8dp/12dp/24dp tokens
6. Update `Theme.kt` to use new scheme, disable dynamic color
7. Create preview composables to verify theme in isolation
8. Commit: "feat: implement premium theme foundation"

**Phase 2: Signature Components** (Week 1-2)
1. Create `presentation/ui/components/` package
2. Implement `NutriCard.kt` with 6dp status line, 24dp radius, JetBrains Mono EAN
3. Implement `PremiumButton.kt` with RoyalBluePrimary, 48dp minimum, 300ms transitions
4. Implement `ShimmerLoading.kt` with infinite gradient sweep animation
5. Implement `EmptyState.kt` with Barcode Leaf illustration + motivational message
6. Implement `InfoChip.kt` for quantity/date display
7. Implement `StatusCircle.kt` with 0.1f alpha background
8. Add Compose preview tests for each component
9. Commit: "feat: implement signature component library"

**Phase 3: Screen Updates** (Week 2-3)
1. Update `DashboardScreen.kt`: header (logo + greeting), Summary Cards, Quick Scan epicenter button, LazyColumn with NutriCard, EmptyState for 0 products
2. Update `ScannerScreen.kt`: add blur overlay (Modifier.blur()), 40% bottom sheet, haptic feedback using `LocalHapticFeedback.current`
3. Update `HistoryScreen.kt`: replace cards with NutriCard, LazyColumn with keys, EmptyState, remove hardcoded semaphore colors (use `MaterialTheme.colorScheme.statusX`)
4. Update `LoginScreen.kt`: PremiumButton, Plus Jakarta Sans typography
5. Test each screen individually before moving to next
6. Commit per screen: "feat: update dashboard with premium ui", "feat: update scanner with immersive design", etc.

**Phase 4: Polish & Validation** (Week 3)
1. Add micro-interactions: 300ms color transitions (animateColorAsState), shimmer effect during sync
2. Implement haptic feedback: Use `LocalHapticFeedback.current` with `HapticFeedbackConstants.SHORT_CLICK` for scan confirm, `DOUBLE_CLICK` for error/duplicate
3. Update UI tests: remove hardcoded color assertions, verify theme usage (`MaterialTheme.colorScheme.statusX`), add component tests
4. Manual validation on XCover7: touch targets, thumb zone reachability, haptic timing
5. Performance validation: ensure animations don't impact barcode scanning (<2s response)
6. Fix any bugs or visual polish issues
7. Commit: "feat: add micro-interactions and polish"

### Rollback Plan

1. **Git Branch Strategy**: All work in `feature/premium-ui-design` branch, main branch remains stable
2. **Phase-by-Phase Commits**: Each phase creates atomic commit, can revert individual phases
3. **Feature Flag (Optional)**: Add `BuildConfig.PREMIUM_UI_ENABLED` to toggle between old/new theme (not required but provides safety net)
4. **Backup Current Theme**: Copy existing `Color.kt`, `Type.kt`, `Theme.kt` to `ui/theme/legacy/` before rewrite
5. **Validation Before Merge**: Manual smoke test on all 4 screens + automated UI test suite pass

**No data migration required** — this is a UI-only change, database schema remains unchanged.

## Open Questions

None — all technical decisions are documented with rationale. Dependencies (fonts, logo) are identified as resources to be added. Material 3 1.2.0 supports all required features without external animation libraries.

---

## Appendix A: Color Palette "Inventario Vivo" (Full Specification)

| Semantic Name | Hex Value | Usage |
|--------------|-----------|-------|
| `CreamBackground` | #F8FAFC | Screen background (light cream) |
| `Surface` | #FFFFFF | Cards, buttons, elevated elements |
| `RoyalBluePrimary` | #2563EB | Primary action buttons (Quick Scan) |
| `RoyalBlueDark` | #1E40AF | Hover states, pressed states |
| `TextPrimary` | #1E293B | Headlines, titles, important text |
| `TextSecondary` | #64748B | Body text, labels, secondary information |
| `StatusTeal` | #14B8A6 | Semaphore GREEN (>7 days safe) - ColorScheme extension |
| `StatusAmber` | #F59E0B | Semaphore YELLOW (1-7 days urgent) - ColorScheme extension |
| `StatusDeepRed` | #DC2626 | Semaphore EXPIRED (≤0 days critical) - ColorScheme extension |
| `SuccessGreen` | #10B981 | Success states, confirmation UI |
| `ShadowColor` | #000000@8% | Custom shadow token for Industrial Elegance (ambientColor & spotColor) |

## Appendix B: Typography Hierarchy

| Text Style | Font Family | Weight | Size | Usage |
|------------|--------------|--------|------|-------|
| `displayLarge` | Plus Jakarta Sans | Bold | 57sp | Hero titles (rare) |
| `headlineMedium` | Plus Jakarta Sans | Bold | 28sp | Screen titles, dashboard headers |
| `titleLarge` | Plus Jakarta Sans | Medium | 22sp | Product names in NutriCard |
| `bodyLarge` | Plus Jakarta Sans | Regular | 16sp | Body text, descriptions |
| `bodySmall` | Plus Jakarta Sans | Regular | 12sp | Labels, helper text |
| `labelMedium` | JetBrains Mono | Medium | 12sp | EAN codes, data values |

## Appendix C: Shape System Mapping

| Shape | Radius | Usage |
|-------|--------|-------|
| `extraSmall` | 8dp | InfoChip, tags, small badges |
| `small` | 8dp | Small UI elements, dividers |
| `medium` | 12dp | Buttons (PremiumButton), text fields |
| `large` | 24dp | Cards (NutriCard, Summary Cards) |
| `extraLarge` | 28dp | Bottom sheets, dialogs |

## Appendix D: Screen-by-Screen Ergonomics Map (XCover7)

### Safe Zone Diagram

```
┌─────────────────────────────┐
│       Top Zone              │ ← Status bar, TopAppBar (hard to reach)
│      (ignore thumb)         │
├─────────────────────────────┤
│                             │
│      Middle Zone            │ ← Content: NutriCard lists, product info
│   (thumb stretch OK)        │
│                             │
├─────────────────────────────┤
│       Bottom Zone           │ ← Quick Scan button, primary actions
│    (thumb epicenter)        │ ← 100% reachable, minimal wrist rotation
└─────────────────────────────┘
```

**Key Ergonomics**:
- Bottom 30% of screen = thumb epicenter (all primary actions)
- Quick Scan button in bottom safe zone with 48dp minimum height
- One-handed operation: user can reach all critical actions without shifting grip
- Haptic feedback provides confirmation without visual check (important in warehouse environment with glare)

### Touch Target Enforcement

| Component | Minimum Size | Actual Size | Notes |
|-----------|--------------|-------------|-------|
| PremiumButton | 48dp | 56dp (height) × full width | Bottom safe zone |
| InfoChip | 44dp | 48dp (height) | NutriCard internal |
| History item | 48dp | 80dp (height) | LazyColumn row |
| Scanner overlay | 48dp | 64dp (diameter) | Camera focus target |

### Haptic Feedback Strategy

| Event | Haptic Type | Implementation |
|-------|-------------|----------------|
| Scan successful | SHORT_CLICK | `val hapticFeedback = LocalHapticFeedback.current`<br>`hapticFeedback.performHapticFeedback(HapticFeedbackConstants.SHORT_CLICK)` |
| Scan error / duplicate | DOUBLE_CLICK | `val hapticFeedback = LocalHapticFeedback.current`<br>`hapticFeedback.performHapticFeedback(HapticFeedbackConstants.DOUBLE_CLICK)` |
| Batch saved | SHORT_CLICK | `val hapticFeedback = LocalHapticFeedback.current`<br>`hapticFeedback.performHapticFeedback(HapticFeedbackConstants.SHORT_CLICK)` |
| Sync complete | SHORT_CLICK | `val hapticFeedback = LocalHapticFeedback.current`<br>`hapticFeedback.performHapticFeedback(HapticFeedbackConstants.SHORT_CLICK)` |

**Required imports:**
```kotlin
import androidx.compose.foundation.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackConstants
```

## Appendix E: Implementation Checklist (Phase 4)

- [ ] Phase 1: Color.kt, Type.kt, Shape.kt, Theme.kt implemented
- [ ] Phase 1: Fonts downloaded and integrated (4 TTF files)
- [ ] Phase 1: Theme verified with preview composables
- [ ] Phase 2: NutriCard component implemented with all parameters
- [ ] Phase 2: PremiumButton implemented with RoyalBluePrimary
- [ ] Phase 2: ShimmerLoading implemented with infinite animation
- [ ] Phase 2: EmptyState implemented with Barcode Leaf illustration
- [ ] Phase 2: InfoChip, StatusCircle implemented
- [ ] Phase 3: Dashboard updated with Premium Dashboard pattern
- [ ] Phase 3: Scanner updated with blur overlay and haptic feedback
- [ ] Phase 3: History updated with NutriCard and LazyColumn keys
- [ ] Phase 3: Login updated with PremiumButton and Plus Jakarta Sans
- [ ] Phase 4: Micro-interactions added (300ms transitions)
- [ ] Phase 4: Haptic feedback implemented
- [ ] Phase 4: UI tests updated (remove hardcoded colors)
- [ ] Phase 4: Manual validation on XCover7 completed
- [ ] Phase 4: Performance validation (<2s scan response)
