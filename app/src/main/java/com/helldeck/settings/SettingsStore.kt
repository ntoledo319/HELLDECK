package com.helldeck.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.helldeck.AppCtx
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "helldeck_prefs")

object SettingsStore {
    // Generator flags
    private val KEY_SAFE_GOLD_ONLY = booleanPreferencesKey("gen_safe_gold_only")
    private val KEY_ENABLE_V3 = booleanPreferencesKey("gen_enable_v3")
    
    // Game settings
    private val KEY_LEARNING_ENABLED = booleanPreferencesKey("learning_enabled")
    private val KEY_ROLLCALL_ON_LAUNCH = booleanPreferencesKey("rollcall_on_launch")
    private val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    
    // Device settings
    private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    private val KEY_PERFORMANCE_MODE = booleanPreferencesKey("performance_mode")
    
    // AI Enhancement
    private val KEY_AI_ENHANCEMENT = booleanPreferencesKey("ai_enhancement_enabled")

    // Accessibility / safety
    private val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    private val KEY_HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    private val KEY_NO_FLASH = booleanPreferencesKey("no_flash")

    // Generator Flags
    suspend fun readFlags(): Pair<Boolean?, Boolean?> {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return Pair(prefs[KEY_SAFE_GOLD_ONLY], prefs[KEY_ENABLE_V3])
    }

    suspend fun writeSafeGoldOnly(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_SAFE_GOLD_ONLY] = value }
    }

    suspend fun writeEnableV3(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_ENABLE_V3] = value }
    }
    
    // Game Settings
    suspend fun readLearningEnabled(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_LEARNING_ENABLED] ?: true
    }
    
    suspend fun writeLearningEnabled(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_LEARNING_ENABLED] = value }
    }
    
    suspend fun readRollcallOnLaunch(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_ROLLCALL_ON_LAUNCH] ?: true
    }
    
    suspend fun writeRollcallOnLaunch(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_ROLLCALL_ON_LAUNCH] = value }
    }

    suspend fun readHasSeenOnboarding(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_HAS_SEEN_ONBOARDING] ?: false
    }

    suspend fun writeHasSeenOnboarding(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_HAS_SEEN_ONBOARDING] = value }
    }

    // Device Settings
    suspend fun readHapticsEnabled(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_HAPTICS_ENABLED] ?: true
    }
    
    suspend fun writeHapticsEnabled(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_HAPTICS_ENABLED] = value }
    }
    
    suspend fun readSoundEnabled(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_SOUND_ENABLED] ?: true
    }
    
    suspend fun writeSoundEnabled(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_SOUND_ENABLED] = value }
    }
    
    // Performance mode (fewer generation attempts for faster rounds)
    suspend fun readPerformanceMode(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_PERFORMANCE_MODE] ?: false
    }
    
    suspend fun writePerformanceMode(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_PERFORMANCE_MODE] = value }
    }
    
    // AI Enhancement
    suspend fun readAIEnhancement(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_AI_ENHANCEMENT] ?: false
    }
    
    suspend fun writeAIEnhancement(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_AI_ENHANCEMENT] = value }
    }

    // Accessibility / safety
    suspend fun readReducedMotion(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_REDUCED_MOTION] ?: false
    }

    suspend fun writeReducedMotion(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_REDUCED_MOTION] = value }
    }

    fun reducedMotionFlow(): Flow<Boolean> {
        return AppCtx.ctx.settingsDataStore.data.map { prefs -> prefs[KEY_REDUCED_MOTION] ?: false }
    }

    suspend fun readHighContrast(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_HIGH_CONTRAST] ?: false
    }

    suspend fun writeHighContrast(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_HIGH_CONTRAST] = value }
    }

    fun highContrastFlow(): Flow<Boolean> {
        return AppCtx.ctx.settingsDataStore.data.map { prefs -> prefs[KEY_HIGH_CONTRAST] ?: false }
    }

    suspend fun readNoFlash(): Boolean {
        val prefs = AppCtx.ctx.settingsDataStore.data.first()
        return prefs[KEY_NO_FLASH] ?: true
    }

    suspend fun writeNoFlash(value: Boolean) {
        AppCtx.ctx.settingsDataStore.edit { it[KEY_NO_FLASH] = value }
    }

    fun noFlashFlow(): Flow<Boolean> {
        return AppCtx.ctx.settingsDataStore.data.map { prefs -> prefs[KEY_NO_FLASH] ?: true }
    }
    
    // Reset all settings to defaults
    suspend fun resetToDefaults() {
        AppCtx.ctx.settingsDataStore.edit { prefs ->
            prefs.clear()
            // Set safe defaults
            prefs[KEY_SAFE_GOLD_ONLY] = false
            prefs[KEY_ENABLE_V3] = true
            prefs[KEY_LEARNING_ENABLED] = true
            prefs[KEY_ROLLCALL_ON_LAUNCH] = true
            prefs[KEY_HAPTICS_ENABLED] = true
            prefs[KEY_SOUND_ENABLED] = true
            prefs[KEY_AI_ENHANCEMENT] = false
            prefs[KEY_PERFORMANCE_MODE] = false
            // Accessibility / safety defaults: readable, low-risk.
            prefs[KEY_REDUCED_MOTION] = false
            prefs[KEY_HIGH_CONTRAST] = false
            prefs[KEY_NO_FLASH] = true
        }
    }
}
