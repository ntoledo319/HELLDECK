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
fun OddExplainRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var explanation by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Explain why this is weird", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = { Text("Your explanation") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onEvent(RoundEvent.EnterText(explanation))
                explanation = ""
            },
            enabled = explanation.isNotBlank()
        ) {
            Text("Submit")
        }
    }
}
