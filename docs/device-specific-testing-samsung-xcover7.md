# Device-Specific Testing Guide - Samsung XCover7

## Overview

This document outlines device-specific testing considerations for the Samsung XCover7 (a rugged Android device used by Decathlon staff). The focus is on battery optimization behavior and worker execution constraints.

## Device Specifications

- **Model**: Samsung Galaxy XCover7
- **Form Factor**: Rugged smartphone with durable design
- **Use Case**: Single-shift operation (6-8 hours continuous use)
- **Battery Optimization**: Aggressive by default on Samsung devices

## Battery Optimization Behavior

### Samsung Battery Optimization Feature

Samsung devices include aggressive battery optimization that may kill background WorkManager jobs. This is a known issue with WorkManager on Samsung devices.

**Symptoms**:
- Worker may not execute at scheduled time
- Worker may be killed mid-execution
- Notifications may be delayed or not sent

**Mitigation Strategies**:

1. **EXPONENTIAL Backoff**: Already implemented in `StatusCheckWorker`
   ```kotlin
   .setBackoffCriteria(
       androidx.work.BackoffPolicy.EXPONENTIAL,
       30, // initial backoff: 30 seconds
       TimeUnit.SECONDS
   )
   ```

2. **Work Constraints**: Minimal constraints to reduce battery impact
   ```kotlin
   .setConstraints(
       Constraints.Builder()
           .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network required
           .build()
   )
   ```

3. **Request Priority**: Background tasks are scheduled with appropriate priority

### User Action Required (Optional)

To ensure reliable execution, users can:

1. Open **Settings > Apps > Smart Nutri Stock > Battery**
2. Disable **"Put app to sleep"**
3. Disable **"Optimize battery usage"**

**Note**: This should be documented in the user guide, not enforced programmatically.

## Battery Impact Testing

### Test Procedure

1. **Baseline Measurement**:
   - Install app on Samsung XCover7
   - Enable background notifications
   - Close app (send to background)
   - Let device idle for 24 hours
   - Measure battery drain (target: <2%)

2. **With Active Use**:
   - Install app on Samsung XCover7
   - Enable background notifications
   - Use app for 1 full shift (6-8 hours)
   - Measure battery drain (target: <2%)

3. **Edge Case Testing**:
   - Enable battery optimization
   - Verify worker still executes (may be delayed)
   - Verify notifications are eventually sent
   - Disable battery optimization
   - Verify worker executes on time

### Expected Results

- **Idle (24h)**: <2% battery drain
- **Active Use (8h)**: <2% additional battery drain
- **With Optimization**: Worker may be delayed but should execute within 1-2 hours
- **Without Optimization**: Worker executes at scheduled time (06:00 AM)

## Worker Execution Constraints

### Low-End Device Considerations

The Samsung XCover7 is not a low-end device, but we should still consider:

1. **Database Query Performance**:
   - Query: `SELECT * FROM active_stocks WHERE deleted_at IS NULL`
   - Expected: <100ms for <10,000 records
   - Test: Add 5,000 batches, measure query time

2. **Status Calculation Performance**:
   - Calculation: Loop through all batches and call `CalculateStatusUseCase`
   - Expected: <200ms for 5,000 batches
   - Test: Measure calculation time with varying batch counts

3. **Notification Building**:
   - Build: Create grouped notification with 100 batches
   - Expected: <50ms
   - Test: Measure notification building time

### Memory Constraints

- **Worker Memory Limit**: ~256MB (typical for background workers)
- **Expected Memory Usage**: <50MB for StatusCheckWorker
- **Test**: Profile worker memory usage with 10,000 batches

## Test Scenarios

### Scenario 1: Daily Worker Execution

**Setup**:
- Device: Samsung XCover7
- Battery optimization: Enabled (default)
- Batches: 100 mixed statuses (50 GREEN, 30 YELLOW, 20 EXPIRED)

**Steps**:
1. Set device time to 05:55 AM
2. Wait until 06:00 AM (worker should trigger)
3. Verify worker completes within 2 seconds
4. Verify notifications are sent for YELLOW and EXPIRED batches
5. Verify GREEN batches do NOT generate notifications

**Expected Result**: Worker executes, notifications sent, battery impact minimal.

### Scenario 2: Worker Killed by Battery Optimization

**Setup**:
- Device: Samsung XCover7
- Battery optimization: Aggressive
- Batches: 10 YELLOW batches

**Steps**:
1. Enqueue worker
2. Kill worker process (simulate battery optimization)
3. Wait for retry (30s initial backoff)
4. Verify worker retries and completes
5. Verify notifications are sent

**Expected Result**: Worker retries with exponential backoff, eventually completes.

### Scenario 3: App Never Opened

**Setup**:
- Device: Samsung XCover7 (fresh install)
- App installed but never opened
- Device idle for 24 hours

**Steps**:
1. Install app (grant notification permission during first open)
2. Close app
3. Wait 24 hours
4. Verify worker executes at 06:00 AM

**Expected Result**: Worker executes even if app is not open (WorkManager handles this).

### Scenario 4: Device Reboot

**Setup**:
- Device: Samsung XCover7
- Worker scheduled
- Batches: 20 YELLOW batches

**Steps**:
1. Enqueue worker
2. Reboot device
3. Wait until 06:00 AM
4. Verify worker executes

**Expected Result**: WorkManager handles reboots automatically, worker executes.

## Battery Impact Measurement

### Manual Testing

Use Android's battery stats to measure impact:

1. **Settings > Battery > Battery Usage**
2. Select "Smart Nutri Stock"
3. View usage over last 24 hours
4. Note background usage

**Acceptable Values**:
- <1%: Excellent
- 1-2%: Acceptable
- >2%: Investigate

### Automated Testing (Optional)

For automated testing, use:
- Android Battery Historian: `python historian.py -i bugreport.txt`
- Battery stats via ADB: `adb shell dumpsys batterystats`

## Logging and Debugging

### Enable WorkManager Logging

For testing, enable verbose logging:

```kotlin
// In SmartNutriStockApp.onCreate()
if (BuildConfig.DEBUG) {
    WorkManager.initialize(
        this,
        Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.VERBOSE)
            .build()
    )
}
```

### Monitor Worker Execution

Use adb to monitor worker execution:

```bash
# Check work status
adb shell dumpsys jobscheduler | grep StatusCheckWorker

# Check work manager queue
adb shell dumpsys workmanager

# View work history
adb shell dumpsys workmanager --all
```

## Success Criteria

The notification feature is considered successful on Samsung XCover7 if:

1. ✅ Worker executes daily at 06:00 AM (±5 minutes)
2. ✅ Battery impact is <2% per day (idle)
3. ✅ Battery impact is <2% per shift (active use)
4. ✅ Notifications are delivered within 2 seconds of worker completion
5. ✅ Worker retries successfully when killed by battery optimization
6. ✅ Deep link navigation works correctly (notification tap → filtered History)
7. ✅ No memory leaks or crashes over 24-hour period

## Known Issues

### Issue: Worker Not Executing on Time

**Symptoms**: Worker executes late or not at all.

**Root Cause**: Samsung battery optimization killed worker process.

**Workaround**:
1. Disable battery optimization for app (user action)
2. Wait for exponential backoff retry
3. Verify worker eventually completes

### Issue: Notifications Not Showing

**Symptoms**: Worker completes but no notifications appear.

**Root Cause**:
- Notification permission denied
- Notification channel not created
- App notification settings disabled

**Workaround**:
1. Check permission: Settings > Apps > Smart Nutri Stock > Notifications
2. Enable notifications
3. Reboot device
4. Trigger worker manually for testing

## References

- [WorkManager on Samsung Devices](https://stackoverflow.com/questions/50643761/workmanager-not-working-on-samsung-devices)
- [Battery Optimization Guidelines](https://developer.android.com/topic/performance/battery/battery-optimization)
- [Samsung Battery Management](https://www.samsung.com/us/support/answer/ANS00063947/)
