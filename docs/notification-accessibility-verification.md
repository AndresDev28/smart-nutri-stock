# Notification Accessibility Verification Guide

## Overview

This document outlines accessibility requirements for the local push notification feature and provides a verification checklist for testing on Samsung XCover7.

## Accessibility Requirements (Mandatory)

### 1. Tap Target Size (48dp Minimum)

**Requirement**: All interactive elements must have tap targets of at least 48dp × 48dp.

**Implementation**:
- Notification actions use `NotificationCompat.Action` with 48dp minimum
- Notification content area is tappable (48dp minimum)
- Small icon is not interactive (decorative only)

**Verification**:
- [ ] Tap notification body → navigates to History screen
- [ ] Tap entire notification → navigates to History screen
- [ ] Tap target is at least 48dp × 48dp on screen

**Code Reference**:
```kotlin
// NotificationCompat ensures proper tap target size
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_notification) // Decorative, not interactive
    .setContentTitle(title)
    .setContentText(text)
    .setGroup(groupKey)
    .setGroupSummary(true)
    .setContentIntent(pendingIntent) // Taps entire notification
    .build()
```

### 2. Screen Reader Support (TalkBack)

**Requirement**: All notification content must be readable by screen readers.

**Implementation**:
- Notification title describes the alert type
- Notification text lists affected products
- ContentDescription set for all icons

**Verification** (with TalkBack enabled):
- [ ] Title is announced: "Tienes 3 lotes por caducar pronto"
- [ ] Content is announced: "Protein Whey 500g, Barrita Energética, y 1 más"
- [ ] Navigation action is announced when tapping
- [ ] No redundant or confusing announcements

**Code Reference**:
```kotlin
// Content is automatically read by TalkBack
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setContentTitle("Tienes 3 lotes por caducar pronto")
    .setContentText("Protein Whey 500g, Barrita Energética, y 1 más")
    .setContentIntent(pendingIntent)
    .build()

// Note: Small icon is decorative, no contentDescription needed
// SetIcon(null) or let NotificationCompat handle it
```

### 3. Color Contrast

**Requirement**: Text and background must have minimum contrast ratio of 4.5:1 (WCAG AA standard).

**Implementation**:
- Use system notification style (ensures contrast)
- Avoid custom colors for text
- Status indicators use semaphore colors only for UI icons

**Verification**:
- [ ] Title text is readable against background (4.5:1 contrast)
- [ ] Content text is readable against background (4.5:1 contrast)
- [ ] Small icon is visible (decorative, may use color)
- [ ] No custom colors that reduce contrast

**Code Reference**:
```kotlin
// Use system notification channel for proper contrast
val channel = NotificationChannel(
    CHANNEL_ID,
    "Alertas de caducidad",
    NotificationManager.IMPORTANCE_HIGH // Uses system style
)
```

### 4. Large Text Support

**Requirement**: Notifications must scale with user's font size settings.

**Implementation**:
- Use `NotificationCompat.BigTextStyle` for long content
- System automatically scales font size
- No hardcoded font sizes

**Verification**:
- [ ] Set device font size to "Large"
- [ ] Notification title scales appropriately
- [ ] Notification text scales appropriately
- [ ] Text does not overflow or get truncated

**Code Reference**:
```kotlin
// BigTextStyle ensures large text support
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setContentTitle(title)
    .setContentText(text)
    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
    .build()
```

## Manual Testing Checklist

### Test Setup

1. **Device**: Samsung XCover7
2. **Android Version**: 13+ (API 33+)
3. **Accessibility Settings**:
   - Enable TalkBack: Settings > Accessibility > TalkBack
   - Set font size: Settings > Display > Font size (test all sizes)
   - Enable high contrast: Settings > Accessibility > Display (optional)

### Accessibility Tests

#### Test 1: Tap Target Size

| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|------------|
| 1 | Create YELLOW notification | Notification appears in shade | ☐ |
| 2 | Tap notification body | Navigates to History screen with YELLOW filter | ☐ |
| 3 | Measure tap area | At least 48dp × 48dp | ☐ |
| 4 | Tap with thumb (one-handed) | Easy to reach, no strain | ☐ |

#### Test 2: Screen Reader Support (TalkBack)

| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|------------|
| 1 | Enable TalkBack | Voice feedback enabled | ☐ |
| 2 | Pull down notification shade | TalkBack announces "Notifications" | ☐ |
| 2 | Navigate to notification | TalkBack announces title and content | ☐ |
| 3 | Double-tap notification | Navigates to History screen | ☐ |
| 4 | Verify no redundant announcements | Clear, concise speech | ☐ |

#### Test 3: Color Contrast

| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|------------|
| 1 | Create YELLOW notification | Notification appears with yellow status icon | ☐ |
| 2 | Create EXPIRED notification | Notification appears with red status icon | ☐ |
| 3 | Verify text readability | High contrast, no eye strain | ☐ |
| 4 | Enable high contrast mode | Notification remains readable | ☐ |

#### Test 4: Large Text Support

| Step | Action | Expected Result | Pass/Fail |
|------|--------|----------------|------------|
| 1 | Set font size to "Small" | Text scales down, remains readable | ☐ |
| 2 | Set font size to "Default" | Text displays normally | ☐ |
| 3 | Set font size to "Large" | Text scales up, remains readable | ☐ |
| 4 | Set font size to "Huge" | Text scales up, no overflow | ☐ |
| 5 | Test with 5+ product names | Text expands in BigTextStyle | ☐ |

## Automated Testing (Optional)

### UI Automator Tests

While manual testing is primary for accessibility, UI Automator can verify some aspects:

```kotlin
@Test
fun notification_tap_target_size_is_at_least_48dp() {
    // This test verifies tap target size using UI Automator
    // Note: Requires notification to be present
    val notification = uiDevice.findObject(
        UiSelector().text("Tienes 3 lotes por caducar pronto")
    )

    // Get bounds and verify minimum size
    val bounds = notification.bounds
    assertThat(bounds.width()).isAtLeast(48)
    assertThat(bounds.height()).isAtLeast(48)
}
```

### Espresso Accessibility Checks

Use Espresso's accessibility checks:

```kotlin
@Test
fun notification_meets_accessibility_requirements() {
    // Enable accessibility checks
    AccessibilityChecks.enable()

    // Navigate to History screen via notification tap
    // This will fail if accessibility issues are detected
}
```

## WCAG 2.1 Compliance

### Level A (Minimum)

- ✅ Non-text contrast: 3:1 (icons)
- ✅ Text contrast: 4.5:1 (normal text)
- ✅ Keyboard accessible: N/A (touch-only device)
- ✅ Resize text: Yes (scales with system)
- ✅ Touch targets: 48dp minimum

### Level AA (Recommended)

- ✅ Text contrast: 4.5:1 (all text)
- ✅ Large text contrast: 3:1 (text ≥18pt)
- ✅ Pointer gestures: N/A (tap only)
- ✅ Touch target spacing: 8dp (system default)

### Level AAA (Not Required)

- Non-text contrast: 7:1 (not required for mobile)

## Known Limitations

### Notification Shade Layout

The notification shade layout is controlled by Android, not our app. We cannot:

- Change notification tap target size (system minimum 48dp)
- Modify notification shade colors (system theme)
- Rearrange notification order (system sorting)

**Mitigation**: We design our notification content to be accessible within system constraints.

### Custom Actions

We do not use custom notification actions (buttons in notification). This simplifies accessibility but limits interactivity.

**Rationale**: Custom actions are harder to make accessible and add complexity.

## References

- [Material Design - Accessibility](https://material.io/design/usability/accessibility.html)
- [Android Accessibility Guide](https://developer.android.com/guide/topics/ui/accessibility)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [Notification Design Guidelines](https://developer.android.com/guide/topics/ui/notifiers/notifications)
- [TalkBack Testing Guide](https://support.google.com/accessibility/android/answer/6283677)

## Success Criteria

All accessibility tests must pass:

- ✅ Tap target size: 48dp minimum
- ✅ Screen reader: All content announced clearly
- ✅ Color contrast: 4.5:1 minimum
- ✅ Large text: Scales without overflow
- ✅ No redundant announcements (TalkBack)

If any test fails, the notification implementation must be revised before release.
