package com.helldeck.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Onboarding flow for first-time users
 */
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to HELLDECK!",
            description = "The ultimate party game for 3-16 players using a single phone. 14 mini-games that learn what your crew finds funny.",
            icon = "🎮"
        ),
        OnboardingPage(
            title = "How to Play",
            description = "Long-press anywhere to draw a new card. Use the big touch zones to make selections. Two-finger tap to go back.",
            icon = "👆"
        ),
        OnboardingPage(
            title = "Game Modes",
            description = "Roast Consensus, Confession or Cap, Poison Pitch, Fill-In Finisher, and 10 more unique games await!",
            icon = "🎯"
        ),
        OnboardingPage(
            title = "Learning AI",
            description = "The game learns from your feedback and adapts to show funnier content over time. Your sense of humor matters!",
            icon = "🧠"
        ),
        OnboardingPage(
            title = "Export Your Brain",
            description = "Export your learned data as a .hhdb file to transfer your group's humor preferences to a new device.",
            icon = "📤"
        ),
        OnboardingPage(
            title = "Let's Get Started!",
            description = "Add 3-10 players for the best experience. The game works with teams for larger groups. Have fun!",
            icon = "🚀"
        )
    )

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = (currentPage + 1f) / pages.size,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Page content
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                OnboardingPageContent(
                    page = pages[currentPage],
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            currentPage--
                            listState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.size(100.dp))
            }

            Text(
                text = "${currentPage + 1} of ${pages.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    if (currentPage < pages.lastIndex) {
                        coroutineScope.launch {
                            currentPage++
                            listState.animateScrollToItem(0)
                        }
                    } else {
                        onComplete()
                    }
                }
            ) {
                Text(if (currentPage == pages.lastIndex) "Get Started!" else "Next")
            }
        }
    }
}

/**
 * Individual onboarding page data
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String
)

/**
 * Onboarding page content
 */
@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Text(
            text = page.icon,
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Visual hint for interaction
        if (page.title.contains("Play")) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💡 Pro Tip",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try long-pressing this card to see how it feels!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Check if user has completed onboarding
 */
class OnboardingManager(private val context: android.content.Context) {

    private val prefs = context.getSharedPreferences("helldeck_prefs", android.content.Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    fun markOnboardingCompleted() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    fun shouldShowOnboarding(): Boolean {
        return !isOnboardingCompleted()
    }

    fun resetOnboarding() {
        prefs.edit().remove("onboarding_completed").apply()
    }
}

/**
 * Onboarding wrapper composable
 */
@Composable
fun OnboardingWrapper(
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val onboardingManager = remember { OnboardingManager(context) }
    var showOnboarding by remember { mutableStateOf(onboardingManager.shouldShowOnboarding()) }

    if (showOnboarding) {
        OnboardingFlow(
            onComplete = {
                onboardingManager.markOnboardingCompleted()
                showOnboarding = false
            }
        )
    } else {
        content()
    }
}