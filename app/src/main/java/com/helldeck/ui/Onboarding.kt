package com.helldeck.ui

import androidx.compose.runtime.*
import com.helldeck.settings.SettingsStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Onboarding state management for HELLDECK.
 * Manages user onboarding completion status and flow control.
 * 
 * @ai_prompt Use OnboardingManager to check/mark onboarding status
 * @context_boundary Onboarding state is stored in SettingsStore via DataStore
 */
class OnboardingManager {
    fun shouldShowOnboardingFlow() = SettingsStore.hasSeenOnboardingFlow().map { seen -> !seen }

    suspend fun isOnboardingCompleted(): Boolean = SettingsStore.readHasSeenOnboarding()

    suspend fun markOnboardingCompleted() {
        SettingsStore.writeHasSeenOnboarding(true)
    }

    suspend fun resetOnboarding() {
        SettingsStore.writeHasSeenOnboarding(false)
    }
}

/**
 * Wrapper that shows onboarding flow for first-time users.
 * Uses the streamlined 3-step onboarding from components.OnboardingFlow.
 * 
 * Design: Welcome → Core Gesture Demo → Ready to Play (~30 seconds total)
 */
@Composable
fun OnboardingWrapper(
    content: @Composable () -> Unit,
) {
    val onboardingManager = remember { OnboardingManager() }
    val scope = rememberCoroutineScope()
    val showOnboarding by onboardingManager.shouldShowOnboardingFlow().collectAsState(initial = true)

    if (showOnboarding) {
        com.helldeck.ui.components.OnboardingFlow(
            onComplete = {
                scope.launch {
                    onboardingManager.markOnboardingCompleted()
                }
            },
        )
    } else {
        content()
    }
}
