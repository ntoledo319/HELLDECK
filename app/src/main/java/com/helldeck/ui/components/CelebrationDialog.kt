package com.helldeck.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.helldeck.ui.HelldeckColors
import kotlinx.coroutines.delay

/**
 * Milestone achievements for celebration
 */
sealed class Milestone(
    val emoji: String,
    val title: String,
    val message: String
) {
    object FirstWin : Milestone("🎉", "First Win!", "You're off to a great start!")
    object TenRounds : Milestone("💯", "10 Rounds!", "The party's getting started!")
    object TwentyFiveRounds : Milestone("🎯", "25 Rounds!", "You're on fire!")
    object FiftyRounds : Milestone("🔥", "50 Rounds!", "Unstoppable!")
    object HundredRounds : Milestone("👑", "100 Rounds!", "Absolute legend!")
    object PerfectScore : Milestone("⭐", "Perfect Score!", "Everyone loved that card!")
    object FiveInRow : Milestone("🎪", "5 in a Row!", "You're on a streak!")
    object AllGamesPlayed : Milestone("🎮", "Game Master!", "You've played them all!")
    object FavoritesCollector : Milestone("❤️", "Collector!", "10 favorite cards saved!")
    object SessionMarathon : Milestone("⏰", "Marathon!", "1 hour of non-stop fun!")
}

/**
 * Celebration dialog with animations
 */
@Composable
fun CelebrationDialog(
    milestone: Milestone,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Pulse animation for emoji
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Play milestone sound
    LaunchedEffect(milestone) {
        com.helldeck.audio.SoundManager.play(context, com.helldeck.audio.GameSound.MILESTONE)
    }

    // Auto-dismiss after 3 seconds
    LaunchedEffect(milestone) {
        delay(3000)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                HelldeckColors.colorPrimary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated emoji
                Text(
                    text = milestone.emoji,
                    fontSize = 80.sp,
                    modifier = Modifier.scale(scale)
                )

                // Title
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = HelldeckColors.colorPrimary,
                    textAlign = TextAlign.Center
                )

                // Message
                Text(
                    text = milestone.message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dismiss button
                TextButton(onClick = onDismiss) {
                    Text("Continue")
                }
            }
        }
    }
}

/**
 * Milestone tracker and checker
 */
object MilestoneTracker {
    /**
     * Checks if any milestones were reached and returns them
     */
    fun checkMilestones(
        totalRounds: Int,
        consecutiveWins: Int,
        gamesPlayedSet: Set<String>,
        favoritesCount: Int,
        sessionDurationMs: Long,
        isFirstWin: Boolean,
        isPerfectScore: Boolean
    ): List<Milestone> {
        val achievements = mutableListOf<Milestone>()

        // First win
        if (isFirstWin) {
            achievements.add(Milestone.FirstWin)
        }

        // Round milestones
        when (totalRounds) {
            10 -> achievements.add(Milestone.TenRounds)
            25 -> achievements.add(Milestone.TwentyFiveRounds)
            50 -> achievements.add(Milestone.FiftyRounds)
            100 -> achievements.add(Milestone.HundredRounds)
        }

        // Perfect score
        if (isPerfectScore) {
            achievements.add(Milestone.PerfectScore)
        }

        // Consecutive wins
        if (consecutiveWins >= 5) {
            achievements.add(Milestone.FiveInRow)
        }

        // All games played
        if (gamesPlayedSet.size >= 14) { // Assuming 14 games total
            achievements.add(Milestone.AllGamesPlayed)
        }

        // Favorites collector
        if (favoritesCount >= 10) {
            achievements.add(Milestone.FavoritesCollector)
        }

        // Marathon session (1 hour)
        if (sessionDurationMs >= 3600000) {
            achievements.add(Milestone.SessionMarathon)
        }

        return achievements
    }
}
