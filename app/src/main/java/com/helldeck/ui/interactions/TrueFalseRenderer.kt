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
fun TrueFalseRenderer(
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
        Text(text = "True or False?", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    selected = "T"
                    onEvent(RoundEvent.PickAB("T"))
                },
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == "T") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(text = "TRUE", fontSize = 20.sp)
            }

            Button(
                onClick = {
                    selected = "F"
                    onEvent(RoundEvent.PickAB("F"))
                },
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == "F") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(text = "FALSE", fontSize = 20.sp)
            }
        }
    }
}
