package com.helldeck.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.helldeck.billing.PromoCodeManager
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing

/**
 * PromoCodeDialog - Dialog for entering and redeeming promo codes
 *
 * Design: Hell's Living Room aesthetic with cyan accent for promo codes
 */
@Composable
fun PromoCodeDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(1500)
            onSuccess()
        }
    }

    fun attemptRedeem() {
        if (code.isBlank()) {
            errorMessage = "Please enter a code"
            return
        }

        isLoading = true
        errorMessage = null
        keyboardController?.hide()

        when (val result = PromoCodeManager.redeemCode(code)) {
            is PromoCodeManager.RedeemResult.Success -> {
                showSuccess = true
            }
            is PromoCodeManager.RedeemResult.Error -> {
                errorMessage = result.message
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isLoading && !showSuccess) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading && !showSuccess,
            dismissOnClickOutside = !isLoading && !showSuccess,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(HelldeckRadius.ExtraLarge),
                    spotColor = HelldeckColors.colorAccentCool.copy(alpha = 0.5f),
                    ambientColor = HelldeckColors.colorAccentCool.copy(alpha = 0.3f),
                ),
            shape = RoundedCornerShape(HelldeckRadius.ExtraLarge),
            colors = CardDefaults.cardColors(
                containerColor = HelldeckColors.surfacePrimary,
            ),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        HelldeckColors.colorAccentCool.copy(alpha = 0.5f),
                        HelldeckColors.colorAccentCool.copy(alpha = 0.2f),
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
                if (showSuccess) {
                    SuccessContent()
                } else {
                    InputContent(
                        code = code,
                        onCodeChange = {
                            code = it.uppercase()
                            errorMessage = null
                        },
                        errorMessage = errorMessage,
                        isLoading = isLoading,
                        focusRequester = focusRequester,
                        onSubmit = { attemptRedeem() },
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessContent() {
    // Success animation
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "success_scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = HelldeckSpacing.ExtraLarge.dp),
    ) {
        // Animated emoji
        Text(
            text = "🎉",
            fontSize = (72 * scale).sp,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Large.dp))

        Text(
            text = "Code Redeemed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = HelldeckColors.colorSecondary,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

        Text(
            text = "All premium games are now unlocked!",
            style = MaterialTheme.typography.bodyLarge,
            color = HelldeckColors.colorOnDark,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

        // Success indicator
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Pill),
            color = HelldeckColors.colorSecondary.copy(alpha = 0.15f),
        ) {
            Text(
                text = "✓ PREMIUM ACTIVATED",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = HelldeckColors.colorSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun InputContent(
    code: String,
    onCodeChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Icon
    Text(
        text = "🎟️",
        fontSize = 48.sp,
    )

    Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

    // Header
    Text(
        text = "Enter Promo Code",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        color = HelldeckColors.colorOnDark,
    )

    Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

    // Description
    Text(
        text = "Unlock all premium games for free",
        style = MaterialTheme.typography.bodyMedium,
        color = HelldeckColors.colorMuted,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(HelldeckSpacing.ExtraLarge.dp))

    // Code input field with enhanced styling
    OutlinedTextField(
        value = code,
        onValueChange = onCodeChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        label = { Text("Promo Code") },
        placeholder = { 
            Text(
                "PARTYMODE",
                color = HelldeckColors.colorMuted.copy(alpha = 0.5f),
            ) 
        },
        singleLine = true,
        isError = errorMessage != null,
        enabled = !isLoading,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onSubmit() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HelldeckColors.colorAccentCool,
            unfocusedBorderColor = HelldeckColors.colorMuted.copy(alpha = 0.5f),
            errorBorderColor = HelldeckColors.Error,
            focusedLabelColor = HelldeckColors.colorAccentCool,
            unfocusedLabelColor = HelldeckColors.colorMuted,
            focusedTextColor = HelldeckColors.colorOnDark,
            unfocusedTextColor = HelldeckColors.colorOnDark,
            cursorColor = HelldeckColors.colorAccentCool,
        ),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
    )

    // Error message
    errorMessage?.let { error ->
        Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))
        Surface(
            shape = RoundedCornerShape(HelldeckRadius.Small),
            color = HelldeckColors.Error.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth(),
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

    // Redeem button with glow
    GlowButton(
        text = if (isLoading) "CHECKING..." else "🔓 REDEEM CODE",
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading && code.isNotBlank(),
        accentColor = HelldeckColors.colorSecondary,
    )

    Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

    // Cancel button
    OutlineButton(
        text = "CANCEL",
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        accentColor = HelldeckColors.colorMuted,
    )
}

/**
 * Standalone promo code entry button for settings or other screens
 *
 * Design: Subtle cyan accent matching the promo code dialog theme
 */
@Composable
fun PromoCodeEntry(
    modifier: Modifier = Modifier,
    onRedeemed: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlineButton(
        text = "🎟️ Redeem Promo Code",
        onClick = { showDialog = true },
        modifier = modifier,
        accentColor = HelldeckColors.colorAccentCool,
    )

    if (showDialog) {
        PromoCodeDialog(
            onDismiss = { showDialog = false },
            onSuccess = {
                showDialog = false
                onRedeemed()
            },
        )
    }
}
