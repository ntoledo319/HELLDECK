package com.helldeck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helldeck.ui.vm.GameNightViewModel

@Composable
fun HouseRulesScreen(
    vm: GameNightViewModel,
    onClose: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("House Rules", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Spicy Mode")
            Switch(checked = vm.spicy, onCheckedChange = { vm.spicy = it })
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Heat Threshold: ${vm.heatThreshold.toInt()}%")
            Slider(
                value = vm.heatThreshold,
                onValueChange = { vm.heatThreshold = it },
                valueRange = 0f..100f,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onClose) {
            Text("Done")
        }
    }
}
