package com.helldeck.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helldeck.content.reporting.ContentReport
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckSpacing

@Composable
fun ReportContentDialog(
    cardText: String,
    onDismiss: () -> Unit,
    onReport: (ContentReport.ReportReason) -> Unit,
) {
    var selectedReason by remember { mutableStateOf<ContentReport.ReportReason?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    selectedReason?.let { onReport(it) }
                    onDismiss()
                },
                enabled = selectedReason != null,
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = "Report Offensive Content",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
            ) {
                Text(
                    text = "Help us improve by reporting inappropriate AI-generated content.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HelldeckColors.LightGray,
                )

                Spacer(modifier = Modifier.height(HelldeckSpacing.Small.dp))

                Text(
                    text = "Card content:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = HelldeckColors.surfacePrimary,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = cardText,
                        modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))

                Text(
                    text = "Select reason:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )

                ContentReport.ReportReason.entries.forEach { reason ->
                    ReportReasonOption(
                        reason = reason,
                        isSelected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                    )
                }
            }
        },
        containerColor = HelldeckColors.DarkGray,
    )
}

@Composable
private fun ReportReasonOption(
    reason: ContentReport.ReportReason,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val (label, description) = when (reason) {
        ContentReport.ReportReason.OFFENSIVE_LANGUAGE -> 
            "Offensive Language" to "Contains profanity or insults"
        ContentReport.ReportReason.HATE_SPEECH -> 
            "Hate Speech" to "Targets protected groups"
        ContentReport.ReportReason.SEXUALLY_EXPLICIT -> 
            "Sexually Explicit" to "Inappropriate sexual content"
        ContentReport.ReportReason.VIOLENCE -> 
            "Violence" to "Promotes or depicts violence"
        ContentReport.ReportReason.HARASSMENT -> 
            "Harassment" to "Bullying or personal attacks"
        ContentReport.ReportReason.OTHER -> 
            "Other" to "Other offensive content"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = if (isSelected) HelldeckColors.colorPrimary.copy(alpha = 0.2f) 
                else HelldeckColors.surfacePrimary,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HelldeckSpacing.Medium.dp),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = HelldeckColors.colorPrimary,
                ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = HelldeckColors.LightGray,
                )
            }
        }
    }
}
