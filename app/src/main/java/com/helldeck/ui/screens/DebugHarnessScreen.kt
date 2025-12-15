package com.helldeck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helldeck.ui.vm.GameNightViewModel
import kotlinx.coroutines.launch

@Composable
fun DebugHarnessScreen(
    vm: GameNightViewModel,
    onClose: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var generationCount by remember { mutableStateOf(0) }
    var failures by remember { mutableStateOf(listOf<String>()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Debug Harness", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Card Generation Test", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Generated: $generationCount")
                Text("Failures: ${failures.size}")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            failures = mutableListOf()
                            generationCount = 0
                            repeat(25) {
                                try {
                                    vm.startRound()
                                    generationCount++
                                } catch (e: Exception) {
                                    failures = failures + (e.message ?: "Unknown error")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate 25 Cards")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (failures.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Failures:", style = MaterialTheme.typography.headlineSmall)
                    LazyColumn {
                        items(failures.size) { index ->
                            Text("${index + 1}. ${failures[index]}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close")
        }
    }
}
