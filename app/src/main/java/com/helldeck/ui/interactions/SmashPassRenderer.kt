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
fun SmashPassRenderer(
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
        Text(text = "Smash or Pass?", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    selected = "SMASH"
                    onEvent(RoundEvent.PickAB("A"))
                },
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == "SMASH") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(text = "SMASH", fontSize = 20.sp)
            }

            Button(
                onClick = {
                    selected = "PASS"
                    onEvent(RoundEvent.PickAB("B"))
                },
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == "PASS") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(text = "PASS", fontSize = 20.sp)
            }
        }
    }
}
