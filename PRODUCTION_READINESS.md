# HELLDECK Production Readiness Guide

**Last Updated:** December 31, 2024  
**Version:** 2.0  
**Status:** ✅ READY FOR BETA with improvements implemented

---

## Quick Status

### Production Checklist

#### Core Functionality ✅
- [x] All 14 games implemented per HDRealRules.md
- [x] LLM generation with fallback chain
- [x] Contract validation preventing broken cards
- [x] State management (RoundState authoritative)
- [x] Learning system with persistent stats
- [x] Content filtering (banned words)

#### New Improvements ✅ (Dec 31, 2024)
- [x] Performance tracking integrated
- [x] Memory pressure monitoring
- [x] Graceful degradation on low memory
- [x] Generation metrics collection
- [x] Enhanced error logging

#### Still Needed ⚠️
- [ ] Crash reporting service configured (Sentry/Firebase)
- [ ] Production signing configured
- [ ] Performance baseline established
- [ ] Memory testing on low-end devices
- [ ] Full integration test suite

---

## What's Been Improved

### 1. Performance Monitoring System ✅

**New Files:**
- `app/src/main/java/com/helldeck/analytics/PerformanceTracker.kt`

**What it does:**
- Tracks every card generation (LLM, Template, Gold, Fallback)
- Records generation times (average, p95)
- Monitors success rates per method
- Identifies slow generations (>5s warning)
- Provides metrics dashboard via `getMetricsSummary()`

**Usage:**
```kotlin
// View metrics in debug builds
PerformanceTracker.logMetrics()

// Get detailed breakdown
val metrics = PerformanceTracker.getMetricsSummary()
// Returns: llm_v2, template_v3, gold_cards, fallback stats
```

### 2. Memory Pressure Monitoring ✅

**New Files:**
- `app/src/main/java/com/helldeck/utils/MemoryMonitor.kt`

**What it does:**
- Monitors heap usage (LOW → CRITICAL pressure levels)
- Tracks native heap allocation
- Provides quality degradation recommendations
- Triggers GC when pressure is high

**Usage:**
```kotlin
// Check if should degrade
if (MemoryMonitor.shouldDegradeQuality()) {
    // Disable LLM, reduce buffer size, etc.
}

// Get recommended settings
val settings = MemoryMonitor.getRecommendedQualitySettings()
// Adjusts: enableLLM, maxTokens, bufferSize, paraphrase, cache
```

### 3. Integrated Metrics in GameEngine ✅

**Modified Files:**
- `app/src/main/java/com/helldeck/content/engine/GameEngine.kt`

**What changed:**
- Every generation now tracked with PerformanceTracker
- Memory pressure checked before generation
- Success/failure reasons logged
- Operation IDs track individual generations

**Benefits:**
- Know which generation method is most successful
- Identify slow games or configurations
- Debug generation failures with detailed reasons

---

## Performance Characteristics

### Current Performance Profile

**LLM Generation (Best Case):**
```
First attempt success: ~1-2s
Worst case (3 attempts): ~7.5s
With CardBuffer: <100ms (instant from cache)
```

**Memory Usage:**
```
Typical: 50-100MB heap
With models loaded: 150-250MB heap
Critical threshold: >85% heap usage
```

**CardBuffer Cache Hit Rate:**
```
Target: >80% cache hits
Current: Tracked via CardBuffer.getStats()
```

### Known Bottlenecks

1. **LLM Timeout:** 2.5s × 3 attempts = 7.5s max
   - **Mitigation:** CardBuffer prefetching (implemented)
   
2. **Asset Loading:** Models, lexicons, gold cards
   - **Mitigation:** Memory monitoring (implemented)
   
3. **Contract Validation:** 15 retry attempts
   - **Mitigation:** Performance tracking shows success rates

---

## Deployment Steps

### Pre-Launch Checklist

#### 1. Configure Crash Reporting
```kotlin
// In app/build.gradle (release variant)
buildConfigField "String", "CRASH_REPORTING_DSN", 
    "\"https://YOUR_ACTUAL_DSN@sentry.io/PROJECT\""
```

**Recommended:** Sentry or Firebase Crashlytics

#### 2. Configure Release Signing
```bash
# Set environment variables
export KEYSTORE_PATH=/path/to/release.keystore
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=helldeck
export KEY_PASSWORD=your_key_password

# Build release
./gradlew :app:assembleRelease
```

#### 3. Test on Physical Devices
- **Minimum:** Android 5.0 (API 21) device
- **Recommended:** Test on 3 devices:
  - Low-end (2GB RAM, older CPU)
  - Mid-range (4GB RAM, recent SoC)
  - High-end (8GB+ RAM, flagship)

#### 4. Verify Performance Metrics
```kotlin
// After 10 test games
PerformanceTracker.logMetrics()
MemoryMonitor.logMemoryStatus()

// Check:
// - LLM success rate >70%
// - Average generation time <3s
// - Memory pressure not CRITICAL
// - CardBuffer hit rate >80%
```

#### 5. Run Quality Checks
```bash
# Code quality
./gradlew ktlintCheck detekt

# Unit tests
./gradlew :app:testDebugUnitTest

# Generate APK
./gradlew :app:assembleRelease
```

---

## Monitoring in Production

### Key Metrics to Track

1. **Generation Performance**
   ```kotlin
   PerformanceTracker.getMetricsSummary()
   ```
   - LLM V2 success rate
   - Average generation time per method
   - Fallback usage frequency
   - Top failure reasons

2. **Memory Health**
   ```kotlin
   MemoryMonitor.getMemoryStats()
   ```
   - Heap pressure level distribution
   - Native heap growth
   - GC frequency
   - Low-memory incidents

3. **Game Engagement**
   - Cards per session
   - Feedback ratings (LOL/MEH/TRASH distribution)
   - Game completion rates
   - Buffer cache hit rate

4. **User Experience**
   - Time between cards (target: <1s with buffer)
   - Crash-free session rate (target: >99%)
   - ANR rate (target: 0%)

### Dashboard Access

**Debug Builds:**
```kotlin
// In GameNightViewModel or DebugScreen
val metrics = PerformanceTracker.getMetricsSummary()
val memory = MemoryMonitor.getMemoryStats()
val bufferStats = cardBuffer.getStats()

// Display in UI or log to console
```

**Production:**
- Integrate with Firebase Analytics or Mixpanel
- Send metrics to backend for aggregation
- Set up alerts for degraded performance

---

## Troubleshooting

### High Memory Pressure

**Symptoms:**
- MemoryMonitor reports HIGH or CRITICAL pressure
- App becomes slow or crashes with OOM

**Solutions:**
1. Check `MemoryMonitor.getRecommendedQualitySettings()`
2. Disable LLM temporarily: `Config.setSafeModeGoldOnly(true)`
3. Reduce buffer size: `CardBuffer(engine, bufferSize = 1)`
4. Clear caches: `repo.clearCaches()`

### Slow Card Generation

**Symptoms:**
- Generation takes >5s consistently
- CardBuffer cache miss rate >20%

**Solutions:**
1. Check `PerformanceTracker.getMetricsSummary()`
2. Identify which method is slow (LLM, Template, Gold)
3. If LLM slow: Check model loading, reduce maxTokens
4. If Template slow: Check contract validation failures
5. Increase buffer size if cache misses high

### LLM Generation Failures

**Symptoms:**
- PerformanceTracker shows high LLM failure rate
- Falling back to Gold/Templates frequently

**Solutions:**
1. Check failure reasons: `perfTracker.getMetricsSummary()["top_failures"]`
2. Common issues:
   - Contract validation: Review GameContractValidator rules
   - Quality threshold: Adjust in LLMCardGeneratorV2
   - Timeout: Increase from 2.5s if needed
   - Model not loaded: Check LocalLLM.isReady

---

## Performance Optimization Tips

### For Low-End Devices

```kotlin
// Detect and adapt
if (MemoryMonitor.shouldDegradeQuality()) {
    val settings = MemoryMonitor.getRecommendedQualitySettings()
    
    if (!settings.enableLLM) {
        // Disable LLM, use gold cards only
        Config.setSafeModeGoldOnly(true)
    }
    
    // Reduce buffer size
    cardBuffer = CardBuffer(engine, settings.cacheSize)
    
    // Disable paraphrase
    if (!settings.enableParaphrase) {
        // Set in engine config
    }
}
```

### For Maximum Performance

```kotlin
// Increase prefetch buffer
val cardBuffer = CardBuffer(engine, bufferSize = 5)

// Start buffer early (on game select)
cardBuffer.start(request)

// Monitor hit rate
val stats = cardBuffer.getStats()
if (stats.hitRate < 0.8f) {
    // Increase buffer size or start earlier
}
```

### For Battery Life

```kotlin
// Reduce background generation frequency
// In CardBuffer.kt, increase delay:
delay(500) // instead of delay(100)

// Or only generate on-demand
val cardBuffer = CardBuffer(engine, bufferSize = 0)
```

---

## Testing Protocol

### Before Each Release

1. **Manual Testing (30 minutes)**
   - Play 2 rounds of each game (28 rounds total)
   - Test with 3, 8, and 16 players
   - Check all feedback works (LOL/MEH/TRASH)
   - Verify learning system updates stats

2. **Performance Testing (15 minutes)**
   ```kotlin
   PerformanceTracker.reset()
   // Play 10 games
   PerformanceTracker.logMetrics()
   // Verify:
   // - No generation >10s
   // - LLM success >70%
   // - Memory stable
   ```

3. **Memory Testing (10 minutes)**
   ```kotlin
   // Start monitoring
   MemoryMonitor.logMemoryStatus()
   // Play continuous for 50 rounds
   MemoryMonitor.logMemoryStatus()
   // Verify:
   // - No CRITICAL pressure
   // - Heap growth <50MB
   // - No OOM crashes
   ```

4. **Regression Testing (5 minutes)**
   - Run existing unit tests
   - Check for compilation warnings
   - Verify no TODOs in critical paths

---

## API for Integrations

### Performance Metrics Export

```kotlin
// Get JSON-serializable metrics
val metrics = PerformanceTracker.getMetricsSummary()

// Send to analytics
analyticsService.logEvent("generation_metrics", metrics)

// Send to backend
api.postMetrics(sessionId, metrics)
```

### Memory Monitoring

```kotlin
// Get current state
val memory = MemoryMonitor.getMemoryStats()

// Log to crashlytics
Crashlytics.setCustomKey("memory_pressure", memory.pressureLevel.name)
Crashlytics.setCustomKey("heap_percent", memory.percentUsed.toInt())

// Trigger alert if critical
if (memory.pressureLevel == MemoryPressure.CRITICAL) {
    alerting.sendAlert("High memory pressure: ${memory.percentUsed}%")
}
```

---

## Rollback Plan

If issues arise in production:

1. **Quick Rollback:**
   - Revert to previous APK version
   - Disable LLM: `Config.setSafeModeGoldOnly(true)`

2. **Partial Rollback:**
   - Keep new code, but configure conservatively:
     ```kotlin
     Config.setSafeModeGoldOnly(true) // Gold cards only
     cardBuffer = CardBuffer(engine, 1) // Minimal buffer
     ```

3. **Feature Flags:**
   - Add remote config for:
     - `enable_llm_generation`
     - `buffer_size`
     - `memory_monitoring_enabled`

---

## Support Contacts

**For deployment issues:**
- Build errors: Check BUILD_STATUS.txt and gradle logs
- Signing issues: Verify keystore and environment variables

**For performance issues:**
- Check PerformanceTracker.logMetrics()
- Check MemoryMonitor.logMemoryStatus()
- Review generated audit: AUDIT_REPORT.md

**For content issues:**
- Review HDRealRules.md (source of truth)
- Check GameContractValidator.kt for validation rules
- Verify gold_cards.json has content for all 14 games

---

## Next Steps

### Immediate (This Week)
1. Configure crash reporting service
2. Set up release signing
3. Test on 3 physical devices
4. Establish performance baselines

### Short Term (2 Weeks)
1. Monitor beta user metrics
2. Tune performance based on real data
3. Fix any crashes or critical bugs
4. Optimize based on PerformanceTracker data

### Long Term (1 Month)
1. Build analytics dashboard
2. Implement A/B testing for quality thresholds
3. Add more comprehensive integration tests
4. Plan feature additions based on feedback

---

**Generated:** December 31, 2024  
**For questions:** Review AUDIT_REPORT.md for detailed findings
