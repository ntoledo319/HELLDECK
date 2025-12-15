package com.helldeck.ui.interactions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

@Composable
fun MiniDuelRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedWinner by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Who won the duel?", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    selectedWinner = 0
                    onEvent(RoundEvent.DuelWinner(0))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedWinner == 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Player 1")
            }

            Button(
                onClick = {
                    selectedWinner = 1
                    onEvent(RoundEvent.DuelWinner(1))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedWinner == 1) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Player 2")
            }
        }
    }
}
