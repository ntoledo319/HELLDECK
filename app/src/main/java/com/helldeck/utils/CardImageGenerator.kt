package com.helldeck.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.theme.HelldeckTheme
import java.io.File
import java.io.FileOutputStream

/**
 * Utilities for generating and sharing card images
 */
object CardImageGenerator {

    /**
     * Generates a shareable image of a card
     *
     * @param context Android context
     * @param cardText The card text to display
     * @param gameName The game name
     * @param playerName Optional player name
     * @return URI to the generated image file, or null if failed
     */
    fun generateCardImage(
        context: Context,
        cardText: String,
        gameName: String,
        playerName: String? = null
    ): Uri? {
        return try {
            // Create bitmap (720x1280 for good quality)
            val width = 720
            val height = 1280
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Create ComposeView to render the card
            val composeView = ComposeView(context).apply {
                setContent {
                    HelldeckTheme {
                        CardImageContent(
                            cardText = cardText,
                            gameName = gameName,
                            playerName = playerName
                        )
                    }
                }
            }

            // Measure and layout the view
            composeView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
            )
            composeView.layout(0, 0, width, height)

            // Draw to canvas
            composeView.draw(canvas)

            // Save to cache directory
            val cacheDir = File(context.cacheDir, "card_images")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val imageFile = File(cacheDir, "helldeck_card_${System.currentTimeMillis()}.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Return URI using FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            Logger.e("Failed to generate card image", e)
            null
        }
    }
}

/**
 * Composable card design for image generation
 */
@Composable
private fun CardImageContent(
    cardText: String,
    gameName: String,
    playerName: String?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Game name badge
            Text(
                text = gameName.uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = HelldeckColors.colorPrimary,
                letterSpacing = 2.sp
            )

            // Card text (main content)
            Text(
                text = cardText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Player attribution (if provided)
            playerName?.let {
                Text(
                    text = "$it's turn",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // HELLDECK branding
            Text(
                text = "HELLDECK",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = HelldeckColors.colorSecondary,
                letterSpacing = 4.sp
            )
        }
    }
}
