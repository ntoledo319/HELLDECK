package com.helldeck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helldeck.ui.vm.GameNightViewModel

@Composable
fun GroupDnaScreen(
    vm: GameNightViewModel,
    onClose: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Group DNA", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        val currentProfile = vm.groupDnaProfile ?: "Not set"
        Text("Group profile: $currentProfile")
        Spacer(modifier = Modifier.height(16.dp))

        val profiles = listOf("Chaos Crew", "Wholesome Gang", "Roast Masters", "Intellectual Squad")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            profiles.forEach { profile ->
                Button(
                    onClick = { vm.groupDnaProfile = profile },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vm.groupDnaProfile == profile)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(profile)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onClose) {
            Text("Done")
        }
    }
}
