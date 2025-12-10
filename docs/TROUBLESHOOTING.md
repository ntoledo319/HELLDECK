# HELLDECK Troubleshooting Guide

## Common Issues

### Installation & Launch Issues

#### App Won't Install
**Symptoms:** Installation fails or aborts
**Solutions:**
1. Enable "Unknown Sources" in Settings → Security
2. Check available storage (need 500MB minimum)
3. Verify APK is not corrupted (re-download if needed)
4. Uninstall older versions first
5. Check Android version compatibility (8.0+)

#### App Crashes on Launch
**Symptoms:** App opens then immediately closes
**Solutions:**
1. Clear app cache: Settings → Apps → HELLDECK → Clear Cache
2. Clear app data (will reset settings): Clear Storage
3. Reinstall the app
4. Check logcat for errors: `adb logcat | grep HELLDECK`
5. Verify device has sufficient RAM (2GB+ recommended)

#### Infinite Loading Screen
**Symptoms:** App stuck on loading or initializing
**Solutions:**
1. Wait 2-3 minutes (first launch can be slow)
2. Clear app data and restart
3. Check asset files are properly included in APK
4. Restart device
5. Reinstall app

### Game Content Issues

#### No Cards Generated
**Symptoms:** Empty card screen or "No content available"
**Solutions:**
1. Verify assets directory contains template and lexicon files
2. Check logs for validation errors
3. Enable safe mode (gold cards only) in Settings
4. Clear generated text cache
5. Reinstall app to restore default assets

#### Poor Quality Cards
**Symptoms:** Nonsensical or low-quality content
**Solutions:**
1. Lower spice level in settings
2. Disable V3 generator temporarily
3. Clear generation cache
4. Check lexicon files for corrupted entries
5. Report specific examples for content improvement

#### Repetitive Content
**Symptoms:** Same cards appearing frequently
**Solutions:**
1. Expand lexicon files with more variety
2. Export/import brainpack to reset learning
3. Increase template pool diversity
4. Adjust UCB exploration parameter
5. Clear template statistics

### Performance Issues

#### Slow Card Generation
**Symptoms:** Long delays between rounds (>5 seconds)
**Solutions:**
1. Disable LLM augmentation in Config
2. Reduce spice level to simplify content
3. Close background apps
4. Clear generation cache
5. Use simpler game modes

#### High Battery Drain
**Symptoms:** Excessive battery consumption
**Solutions:**
1. Disable haptic feedback
2. Disable torch/flash features
3. Lower screen brightness
4. Disable LLM features
5. Use power saving mode

#### Memory Issues
**Symptoms:** App running slowly or crashing after extended use
**Solutions:**
1. Restart app periodically
2. Clear caches regularly
3. Reduce number of active players
4. Disable background processes
5. Upgrade device RAM if possible

### Kiosk Mode Issues

#### Can't Enable Kiosk Mode
**Symptoms:** Kiosk setup fails or doesn't persist
**Solutions:**
1. Device must be factory reset for device owner setup
2. Use ADB method: `adb shell dpm set-device-owner`
3. Alternative: Use Screen Pinning instead
4. Verify no other device admin apps active
5. Check USB debugging is enabled

#### Can't Exit Kiosk Mode
**Symptoms:** Stuck in lock task mode
**Solutions:**
1. Use admin unlock code (check settings)
2. Power button + Volume Down for 5 seconds
3. ADB: `adb shell am task lock stop`
4. Remove device owner via desktop tool
5. Factory reset as last resort (data loss)

### Data & Sync Issues

#### Brainpack Export Fails
**Symptoms:** Can't export learning data
**Solutions:**
1. Check storage permissions
2. Verify sufficient storage space
3. Use different export location
4. Clear export cache
5. Check file system permissions

#### Brainpack Import Fails
**Symptoms:** Import fails or corrupts data
**Solutions:**
1. Verify brainpack file is valid JSON
2. Check file is not corrupted
3. Ensure compatible version
4. Clear existing data before import
5. Use fresh install + import

### UI/UX Issues

#### Text Not Readable
**Symptoms:** Font too small or blurry
**Solutions:**
1. Adjust device display scaling
2. Use landscape orientation
3. Increase font size in accessibility settings
4. Check screen resolution compatibility
5. Report specific screens for UI fixes

#### Touch Controls Not Responsive
**Symptoms:** Buttons don't respond to taps
**Solutions:**
1. Clean screen thoroughly
2. Remove screen protector temporarily
3. Restart app
4. Check touch sensitivity settings
5. Test with different fingers/stylus

## Debug Information

### Collecting Logs
```bash
# Collect app logs
adb logcat -d > helldeck_log.txt

# Filter HELLDECK logs only
adb logcat | grep -E "HELLDECK|GameEngine|ContentEngine"

# Export debug info from app
Settings → Developer → Export Debug Info
```

### Reporting Issues
Include the following when reporting problems:
1. Device model and Android version
2. App version (Settings → About)
3. Steps to reproduce
4. Logcat output if available
5. Screenshots or screen recording

### Developer Options
Enable developer mode for additional debugging:
1. Settings → About → Tap version 7 times
2. Settings → Developer Options
3. Enable options as needed:
   - Template selection logging
   - Quality inspector verbose mode
   - Force V3 generator
   - Bypass quality gates (not recommended)

## Advanced Troubleshooting

### Resetting App State
Complete reset while preserving custom content:
1. Export brainpack (if desired)
2. Clear app data
3. Reinstall app
4. Import brainpack
5. Reconfigure settings

### Content Validation
Check asset file integrity:
1. Settings → Developer → Validate Assets
2. Review validation report
3. Fix reported issues in JSON files
4. Restart app to reload

### Performance Profiling
Analyze performance issues:
1. Enable profiling in Config
2. Run Card Lab with large sample size
3. Review generation time metrics
4. Check memory usage patterns
5. Optimize based on findings

## Getting Help

### Resources
- [API Documentation](API.md)
- [Architecture Guide](ARCHITECTURE.md)
- [User Guide](USERGUIDE.md)
- [Developer Guide](DEVELOPER.md)

### Contact Support
For issues not covered here:
1. Check existing GitHub issues
2. Review discussions/FAQ
3. Create detailed bug report
4. Include debug information
5. Be patient for response

---
*Last updated: 2025-12-10*