package com.helldeck.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utilities for sharing and exporting content
 */
object ShareUtils {
    /**
     * Copies text to clipboard
     */
    fun copyToClipboard(context: Context, text: String, label: String = "HELLDECK") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    /**
     * Opens share sheet with text content
     */
    fun shareText(context: Context, text: String, title: String = "Share Session Summary") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    /**
     * Shares session summary with appropriate formatting
     */
    fun shareSessionSummary(context: Context, summaryText: String) {
        shareText(context, summaryText, "Share HELLDECK Session")
    }

    /**
     * Shares an image file
     *
     * @param context Android context
     * @param imageUri URI to the image file
     * @param title Share dialog title
     */
    fun shareImage(context: Context, imageUri: Uri, title: String = "Share Card") {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, imageUri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Logger.e("Failed to share image", e)
            Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares a card as an image
     *
     * @param context Android context
     * @param cardText Card text to share
     * @param gameName Game name
     * @param playerName Optional player name
     */
    fun shareCardAsImage(
        context: Context,
        cardText: String,
        gameName: String,
        playerName: String? = null
    ) {
        val imageUri = CardImageGenerator.generateCardImage(
            context = context,
            cardText = cardText,
            gameName = gameName,
            playerName = playerName
        )

        if (imageUri != null) {
            shareImage(context, imageUri, "Share HELLDECK Card")
        } else {
            Toast.makeText(context, "Failed to generate card image", Toast.LENGTH_SHORT).show()
        }
    }
}
