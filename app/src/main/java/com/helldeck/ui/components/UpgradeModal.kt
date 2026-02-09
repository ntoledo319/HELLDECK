package com.helldeck.ui.components

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.helldeck.billing.PurchaseManager
import com.helldeck.engine.GameMetadata
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.gameIconFor

/**
 * UpgradeModal - Shows when user taps a locked premium game
 *
 * Design: Hell's Living Room aesthetic with neon glow, gradient accents
 */
@Composable
fun UpgradeModal(
    gameId: String,
    onDismiss: () -> Unit,
    onPurchaseComplete: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var showPromoCodeDialog by remember { mutableStateOf(false) }
    val purchaseError by PurchaseManager.purchaseError.collectAsState()
    val isPremiumUnlocked by PurchaseManager.isPremiumUnlocked.collectAsState()

    LaunchedEffect(isPremiumUnlocked) {
        if (isPremiumUnlocked) {
            onPurchaseComplete()
        }
    }

    val gameMetadata = remember(gameId) { GameMetadata.getGameMetadata(gameId) }
    val gameName = gameMetadata?.title ?: "Premium Game"
    val gameDescription = gameMetadata?.description ?: "This game requires premium access."
    val gameEmoji = gameIconFor(gameId)
    val priceString = PurchaseManager.getPremiumPriceString()

    val premiumGameCount = remember {
        GameMetadata.getAllGameIds().count { !PurchaseManager.FREE_GAMES.contains(it) }
    }

    // Subtle glow animation for the card
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(HelldeckRadius.ExtraLarge),
                    spotColor = HelldeckColors.colorPrimary.copy(alpha = glowAlpha),
                    ambientColor = HelldeckColors.colorPrimary.copy(alpha = 0.3f),
                ),
            shape = RoundedCornerShape(HelldeckRadius.ExtraLarge),
            colors = CardDefaults.cardColors(
                containerColor = HelldeckColors.surfacePrimary,
            ),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        HelldeckColors.colorPrimary.copy(alpha = 0.6f),
                        HelldeckColors.colorPrimaryVariant.copy(alpha = 0.3f),
                    ),
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                HelldeckColors.surfaceElevated,
                                HelldeckColors.surfacePrimary,
                            ),
                        ),
                    )
                    .padding(HelldeckSpacing.ExtraLarge.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Premium badge
                Surface(
                    shape = RoundedCornerShape(HelldeckRadius.Pill),
                    color = HelldeckColors.colorPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, HelldeckColors.colorPrimary.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = "🔒 PREMIUM GAME",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HelldeckColors.colorPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

                // Game emoji with glow effect
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(50),
                            spotColor = HelldeckColors.colorSecondary.copy(alpha = 0.5f),
                        )
                        .background(
                            color = HelldeckColors.surfaceElevated,
                            shape = RoundedCornerShape(50),
                        )
                        .padding(20.dp),
                ) {
                    Text(
                        text = gameEmoji,
                        fontSize = 56.sp,
                    )
                }

                Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

                // Game name
                Text(
                    text = gameName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = HelldeckColors.colorOnDark,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

                // Game description
                Text(
                    text = gameDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HelldeckColors.colorMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = HelldeckSpacing.Small.dp),
                    maxLines = 3,
                )

                Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

                // Premium benefits card
                Surface(
                    shape = RoundedCornerShape(HelldeckRadius.Large),
                    color = HelldeckColors.colorSecondary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, HelldeckColors.colorSecondary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(HelldeckSpacing.Large.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
                    ) {
                        Text(
                            text = "🎮 Unlock $premiumGameCount Premium Games",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HelldeckColors.colorSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "One-time purchase · No ads · No subscriptions",
                            style = MaterialTheme.typography.bodySmall,
                            color = HelldeckColors.colorMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Error message
                purchaseError?.let { error ->
                    Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
                    Surface(
                        shape = RoundedCornerShape(HelldeckRadius.Medium),
                        color = HelldeckColors.Error.copy(alpha = 0.1f),
                    ) {
                        Text(
                            text = "⚠️ $error",
                            style = MaterialTheme.typography.bodySmall,
                            color = HelldeckColors.Error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

                // Purchase button with glow
                GlowButton(
                    text = "🔓 $priceString UNLOCK ALL",
                    onClick = {
                        PurchaseManager.clearError()
                        activity?.let { PurchaseManager.purchasePremium(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activity != null,
                    accentColor = HelldeckColors.colorPrimary,
                )

                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

                // Promo code link
                TextButton(
                    onClick = { showPromoCodeDialog = true },
                ) {
                    Text(
                        text = "🎟️ Have a code? TAP HERE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = HelldeckColors.colorAccentCool,
                    )
                }

                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

                // Back button
                OutlineButton(
                    text = "MAYBE LATER",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = HelldeckColors.colorMuted,
                )
            }
        }
    }

    if (showPromoCodeDialog) {
        PromoCodeDialog(
            onDismiss = { showPromoCodeDialog = false },
            onSuccess = {
                showPromoCodeDialog = false
                onPurchaseComplete()
            },
        )
    }
}

/**
 * Compact upgrade prompt banner for inline use (e.g., in game picker)
 *
 * Design: Subtle neon accent with gradient border, matches Hell's Living Room aesthetic
 */
@Composable
fun UpgradePromptBanner(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val priceString = PurchaseManager.getPremiumPriceString()
    val premiumGameCount = remember {
        GameMetadata.getAllGameIds().count { !PurchaseManager.FREE_GAMES.contains(it) }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                spotColor = HelldeckColors.colorPrimary.copy(alpha = 0.4f),
            ),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        color = HelldeckColors.surfaceElevated,
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    HelldeckColors.colorPrimary.copy(alpha = 0.5f),
                    HelldeckColors.colorPrimaryVariant.copy(alpha = 0.3f),
                ),
            ),
        ),
        onClick = onUpgradeClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            HelldeckColors.colorPrimary.copy(alpha = 0.08f),
                            HelldeckColors.surfaceElevated,
                        ),
                    ),
                )
                .padding(HelldeckSpacing.Large.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            // Icon
            Text(
                text = "✨",
                fontSize = 28.sp,
            )

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Unlock $premiumGameCount More Games",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HelldeckColors.colorOnDark,
                )
                Text(
                    text = "One-time purchase · No subscriptions",
                    style = MaterialTheme.typography.bodySmall,
                    color = HelldeckColors.colorMuted,
                )
            }

            // Price button
            Surface(
                shape = RoundedCornerShape(HelldeckRadius.Pill),
                color = HelldeckColors.colorPrimary,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = priceString,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = HelldeckColors.background,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}
