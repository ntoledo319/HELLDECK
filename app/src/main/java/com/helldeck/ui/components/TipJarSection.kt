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
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing

/**
 * TipJarSection - Optional tip jar for supporting the developer
 *
 * Design: Warm orange accent with subtle glow, matching Hell's Living Room aesthetic
 */
@Composable
fun TipJarSection(
    modifier: Modifier = Modifier,
) {
    var showTipDialog by remember { mutableStateOf(false) }
    val isTipPurchased by PurchaseManager.isTipPurchased.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                spotColor = HelldeckColors.colorAccentWarm.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        colors = CardDefaults.cardColors(
            containerColor = HelldeckColors.surfaceElevated,
        ),
        border = BorderStroke(
            width = 1.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    HelldeckColors.colorAccentWarm.copy(alpha = 0.4f),
                    HelldeckColors.colorAccentWarm.copy(alpha = 0.15f),
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
                            HelldeckColors.surfacePrimary.copy(alpha = 0.7f),
                        ),
                    ),
                )
                .padding(HelldeckSpacing.ExtraLarge.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Coffee icon
            Text(
                text = "☕",
                fontSize = 40.sp,
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

            // Header
            Text(
                text = "Support HELLDECK",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = HelldeckColors.colorOnDark,
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

            // Description
            Text(
                text = "Solo dev, no ads, just games.\nIf you're enjoying HELLDECK, consider buying me a coffee!",
                style = MaterialTheme.typography.bodyMedium,
                color = HelldeckColors.colorMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

            if (isTipPurchased) {
                // Already purchased - show thank you with glow
                Surface(
                    shape = RoundedCornerShape(HelldeckRadius.Pill),
                    color = HelldeckColors.colorSecondary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, HelldeckColors.colorSecondary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(HelldeckSpacing.Large.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "💚 Thank you for your support!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HelldeckColors.colorSecondary,
                        )
                    }
                }
            } else {
                // Tip button with glow
                GlowButton(
                    text = "☕ ${PurchaseManager.getTipPriceString()} BUY ME A COFFEE",
                    onClick = { showTipDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = HelldeckColors.colorAccentWarm,
                )
            }
        }
    }

    if (showTipDialog) {
        TipJarDialog(
            onDismiss = { showTipDialog = false },
            onPurchaseComplete = { showTipDialog = false },
        )
    }
}

/**
 * TipJarDialog - Confirmation dialog for tip purchase
 *
 * Design: Warm orange accent with celebration animation on success
 */
@Composable
fun TipJarDialog(
    onDismiss: () -> Unit,
    onPurchaseComplete: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val purchaseError by PurchaseManager.purchaseError.collectAsState()
    val isTipPurchased by PurchaseManager.isTipPurchased.collectAsState()

    LaunchedEffect(isTipPurchased) {
        if (isTipPurchased) {
            kotlinx.coroutines.delay(2000)
            onPurchaseComplete()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(HelldeckRadius.ExtraLarge),
                    spotColor = HelldeckColors.colorAccentWarm.copy(alpha = 0.5f),
                    ambientColor = HelldeckColors.colorAccentWarm.copy(alpha = 0.3f),
                ),
            shape = RoundedCornerShape(HelldeckRadius.ExtraLarge),
            colors = CardDefaults.cardColors(
                containerColor = HelldeckColors.surfacePrimary,
            ),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        HelldeckColors.colorAccentWarm.copy(alpha = 0.5f),
                        HelldeckColors.colorAccentWarm.copy(alpha = 0.2f),
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
                if (isTipPurchased) {
                    ThankYouContent()
                } else {
                    TipConfirmContent(
                        purchaseError = purchaseError,
                        activity = activity,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThankYouContent() {
    // Celebration animation
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "celebration_scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = HelldeckSpacing.Large.dp),
    ) {
        Text(
            text = "🎉",
            fontSize = (72 * scale).sp,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        Text(
            text = "Thank You!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = HelldeckColors.colorSecondary,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

        Text(
            text = "Your support means the world!",
            style = MaterialTheme.typography.bodyLarge,
            color = HelldeckColors.colorOnDark,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        Text(
            text = "Happy partying! 🎊",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = HelldeckColors.colorAccentWarm,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        // Success badge
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Pill),
            color = HelldeckColors.colorSecondary.copy(alpha = 0.15f),
        ) {
            Text(
                text = "💚 SUPPORTER",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.colorSecondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun TipConfirmContent(
    purchaseError: String?,
    activity: Activity?,
    onDismiss: () -> Unit,
) {
    // Icon
    Text(
        text = "☕",
        fontSize = 64.sp,
    )

    Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

    Text(
        text = "Support HELLDECK",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        color = HelldeckColors.colorOnDark,
    )

    Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

    Text(
        text = "Solo dev, no ads, just games.\nYour tip helps keep the chaos flowing!",
        style = MaterialTheme.typography.bodyMedium,
        color = HelldeckColors.colorMuted,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp,
    )

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
        text = "☕ ${PurchaseManager.getTipPriceString()} BUY ME A COFFEE",
        onClick = {
            PurchaseManager.clearError()
            activity?.let { PurchaseManager.purchaseTip(it) }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = activity != null,
        accentColor = HelldeckColors.colorAccentWarm,
    )

    Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

    // Maybe later button
    OutlineButton(
        text = "MAYBE LATER",
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        accentColor = HelldeckColors.colorMuted,
    )
}

/**
 * Compact tip jar button for inline use
 */
@Composable
fun TipJarButton(
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val isTipPurchased by PurchaseManager.isTipPurchased.collectAsState()

    if (!isTipPurchased) {
        OutlineButton(
            text = "☕ Tip Jar",
            onClick = { showDialog = true },
            modifier = modifier,
            accentColor = HelldeckColors.colorAccentWarm,
        )
    }

    if (showDialog) {
        TipJarDialog(
            onDismiss = { showDialog = false },
            onPurchaseComplete = { showDialog = false },
        )
    }
}
