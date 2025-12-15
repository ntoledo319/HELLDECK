package com.helldeck.ui.interactions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

@Composable
fun PredictVoteRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var prediction by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Predict the majority vote", style = MaterialTheme.typography.headlineSmall)

        val (optA, optB) = when (val opts = roundState.options) {
            is com.helldeck.content.model.GameOptions.ABChoice -> opts.a to opts.b
            else -> "A" to "B"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    prediction = "A"
                    onEvent(RoundEvent.PreChoice("A"))
                },
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (prediction == "A") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(text = optA, fontSize = 20.sp)
            }

            Button(
                onClick = {
                    prediction = "B"
                    onEvent(RoundEvent.PreChoice("B"))
                },
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (prediction == "B") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(text = optB, fontSize = 20.sp)
            }
        }
    }
}
