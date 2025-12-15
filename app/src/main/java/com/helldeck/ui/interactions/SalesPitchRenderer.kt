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
fun SalesPitchRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var pitch by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Make your sales pitch", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pitch,
            onValueChange = { pitch = it },
            label = { Text("Your pitch") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onEvent(RoundEvent.EnterText(pitch))
                pitch = ""
            },
            enabled = pitch.isNotBlank()
        ) {
            Text("Pitch It!")
        }
    }
}
