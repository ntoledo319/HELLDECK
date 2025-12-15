package com.helldeck.ui.interactions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Renders A/B choice interaction (A_B_CHOICE).
 * Used for binary decision games.
 */
@Composable
fun ABChoiceRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Make your choice",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Get option labels
        val (optA, optB) = when (val opts = roundState.options) {
            is com.helldeck.content.model.GameOptions.AB -> opts.optionA to opts.optionB
            else -> "A" to "B"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    selected = "A"
                    onEvent(RoundEvent.PickAB("A"))
                },
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == "A") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(text = optA, fontSize = 20.sp)
            }

            Button(
                onClick = {
                    selected = "B"
                    onEvent(RoundEvent.PickAB("B"))
                },
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == "B") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(text = optB, fontSize = 20.sp)
            }
        }
    }
}
