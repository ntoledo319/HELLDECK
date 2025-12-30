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
fun SpeedListRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var item by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<String>()) }

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "List as many as you can!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Items listed: ${items.size}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = item,
            onValueChange = { item = it },
            label = { Text("Next item") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (item.isNotBlank()) {
                        items = items + item
                        onEvent(RoundEvent.EnterText(item))
                        item = ""
                    }
                },
                enabled = item.isNotBlank(),
            ) {
                Text("Add")
            }

            Button(onClick = { onEvent(RoundEvent.LockIn) }) {
                Text("Done")
            }
        }
    }
}
