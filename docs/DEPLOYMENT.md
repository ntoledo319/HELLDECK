# HELLDECK Deployment Guide

## 🚀 Quick Deployment

### Prerequisites
- Android Studio Giraffe (2023.3.1) or later
- Android device with USB debugging enabled
- Python 3.7+ (for desktop loader)
- Java 17 JDK

### 1. Build Release APK
```bash
# Clean and build release APK
./gradlew clean :app:assembleProductionRelease

# APK location
ls app/build/outputs/apk/production/release/app-production-release.apk
```

### 2. Install Using Desktop Loader
```bash
# Install Python dependencies
pip install -r loader/requirements.txt

# Run the installer
python loader/helldeck_loader.py
```

### 3. Desktop Loader Steps
- Click **Browse** to select your built APK
- Click **Install APK** to push to device
- Click **Set Device Owner** (freshly reset device only)
- Click **Reboot Device**

## 🔧 Environment Setup

### Required Environment Variables
Create a `.env` file or set these environment variables:

```bash
# Signing Configuration
KEYSTORE_PATH=path/to/your/keystore.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password

# Play Store Deployment (Optional)
SERVICE_ACCOUNT_JSON=path/to/service-account.json
```

### Gradle Properties
Sensitive configuration is managed in `gradle.properties`:

```properties
# Release signing
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=store_password
RELEASE_KEY_ALIAS=key_alias
RELEASE_KEY_PASSWORD=key_password

# Build configuration
BUILD_TOOLS_VERSION=34.0.0
COMPILE_SDK_VERSION=34
MIN_SDK_VERSION=21
TARGET_SDK_VERSION=34
```

## 🏗️ Build Process

### Local Development Build
```bash
# Internal debug build (all content unlocked)
./gradlew :app:assembleInternalDebug

# Install to connected device
./gradlew :app:installInternalDebug
```

### Release Build
```bash
# Clean build
./gradlew clean

# Build the production release APK (release signing must be configured first)
./gradlew :app:assembleProductionRelease

# Build release bundle (for Play Store)
./gradlew :app:bundleProductionRelease
```

### Build Verification
```bash
# Run all checks
./gradlew ktlintCheck detekt spotlessCheck

# Run tests
./gradlew :app:testProductionDebugUnitTest

# Check for lint issues
./gradlew :app:lintProductionDebug

# Verify APK signature
jarsigner -verify -verbose -certs app/build/outputs/apk/production/release/app-production-release.apk
```

## 📦 Distribution

### Prebuilt Tester APK

A CI-verified, debug-signed Android test build is available from the
[HELLDECK Android 1.0.1 prerelease](https://github.com/ntoledo319/HELLDECK/releases/tag/android-test-v1.0.1-0093e26).
It is intended for fresh-install exploratory testing only. The release page includes the APK,
checksum, automated-test evidence, installation steps, and signing/model limitations.

### Play Store Deployment
1. **Prepare Assets**
   - Store listing screenshots (minimum 2, maximum 8)
   - Feature graphic (1024x500px)
   - App icon (512x512px)
   - Promotional video (optional)

2. **Create Release**
   ```bash
   # Build release bundle
   ./gradlew :app:bundleProductionRelease

   # Upload to Play Store via Play Console
   # or use fastlane for automation
   ```

3. **Release Tracks**
   - **Internal**: For initial testing (up to 100 users)
   - **Alpha**: For broader testing
   - **Beta**: For production testing
   - **Production**: Full release

### Sideload Distribution
```bash
# Generate download link
adb install -r app/build/outputs/apk/production/release/app-production-release.apk

# Share APK file directly
# APK located at: app/build/outputs/apk/production/release/app-production-release.apk
```

## 🔒 Security Considerations

### App Signing
- Use different keystores for debug and release
- Store release keystore securely (not in version control)
- Use strong passwords (minimum 8 characters)
- Enable APK signature verification
- The manual GitHub release environment expects `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, and `KEY_PASSWORD` secrets. Play uploads additionally require
  `SERVICE_ACCOUNT_JSON`.

### Permissions
The app requests these permissions:
- `VIBRATE`: Haptic feedback
- `CAMERA`: Flash/torch functionality
- `RECORD_AUDIO`: Voice features (future)
- `FOREGROUND_SERVICE`: Background operations
- `WAKE_LOCK`: Keep screen on during gameplay
- `BIND_DEVICE_ADMIN`: Kiosk mode (optional)

### Data Security
- All game data stored locally in Room database
- No personal data transmitted to external servers
- Brainpack exports are encrypted ZIP files
- Database uses SQLCipher for encryption (recommended for production)

## 🧪 Testing Before Deployment

### Automated Tests
```bash
# Run all tests
./gradlew :app:testProductionDebugUnitTest :app:connectedProductionDebugAndroidTest
```

### Manual Testing Checklist
- [ ] App launches without crashes
- [ ] All 14 game modes function correctly
- [ ] Kiosk mode works (if enabled)
- [ ] Export/Import functionality works
- [ ] Settings persistence works
- [ ] Performance is acceptable on target devices
- [ ] Battery usage is reasonable
- [ ] Network permissions are not abused

### Device Testing
Test on multiple devices:
- Minimum API 21 (Android 5.0)
- Target API 34 (Android 14)
- Various screen sizes and densities
- Different manufacturers (Samsung, Google, OnePlus, etc.)

## 🚨 Troubleshooting

### Build Issues

**APK Installation Fails**
```bash
# Check device connection
adb devices

# Enable unknown sources
adb shell settings put secure install_non_market_apps 1

# Clear app data if needed
adb shell pm clear com.helldeck
```

**Gradle Build Fails**
```bash
# Clean build cache
./gradlew cleanBuildCache

# Update dependencies
./gradlew build --refresh-dependencies

# Check for dependency conflicts
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

**Kiosk Mode Issues**
- Device must be freshly reset for device owner setup
- Alternative: Use system settings → Security → Screen pinning
- Check device admin permissions in settings

### Runtime Issues

**App Crashes on Launch**
1. Check device logs: `adb logcat | grep HELLDECK`
2. Verify minimum API level support
3. Check for storage permissions
4. Look for database corruption

**Game Not Learning**
1. Check that feedback is being recorded in rounds table
2. Verify template scores are updating
3. Export/Import brainpack to transfer learned data
4. Check database integrity

**Performance Issues**
1. Monitor memory usage
2. Check for database query optimization
3. Verify image and asset sizes
4. Profile method execution times

## 📊 Monitoring & Analytics

### Crash Reporting
The app includes built-in crash reporting. Configure with:
```kotlin
// In app/build.gradle
buildConfigField "String", "CRASH_REPORTING_DSN", "\"your-sentry-dsn\""
```

### Performance Monitoring
- Database query performance
- Memory usage tracking
- UI rendering performance
- Battery impact assessment

### User Analytics
- Game mode popularity
- Session duration
- Player retention
- Feature usage patterns

## 🔄 Updates & Maintenance

### Version Management
Follow semantic versioning:
- **Major**: Breaking changes, API changes
- **Minor**: New features, backward compatible
- **Patch**: Bug fixes, security updates

### Update Process
1. Update version in `app/build.gradle`
2. Update changelog
3. Build and test release APK
4. Deploy to Play Store
5. Monitor crash reports and user feedback

### Rollback Plan
- Keep previous APKs archived
- Have database migration strategy
- Monitor crash reports for new issues
- Be prepared to pull from Play Store if critical issues arise

## 📋 Deployment Checklist

### Pre-Deployment
- [ ] All tests pass
- [ ] Code review completed
- [ ] Security audit passed
- [ ] Performance testing completed
- [ ] Accessibility testing done
- [ ] Localization testing completed
- [ ] Battery impact assessed

### Deployment Day
- [ ] Build final release APK
- [ ] Verify APK signature
- [ ] Test installation on target devices
- [ ] Deploy to Play Store (if applicable)
- [ ] Monitor initial crash reports
- [ ] Verify analytics tracking

### Post-Deployment
- [ ] Monitor user feedback
- [ ] Track app performance metrics
- [ ] Address any critical issues
- [ ] Plan next update based on user data

## 📞 Support & Communication

### User Support
- In-app feedback mechanism
- Email support channel
- FAQ section in app
- Social media presence

### Developer Documentation
- API documentation (`docs/API.md`)
- Architecture decisions record
- Deployment runbook
- Troubleshooting guide

---

*For support and questions, check the troubleshooting section or review the example code in the test cases.*
