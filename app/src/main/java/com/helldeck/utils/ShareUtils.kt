package com.helldeck.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
}
