package com.helldeck.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helldeck.ui.vm.GameNightViewModel

@Composable
fun PacksScreen(
    vm: GameNightViewModel,
    onClose: () -> Unit = {}
) {
    val availablePacks = listOf("Core", "Party", "Dark Humor", "Wholesome", "Office", "NSFW")

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Card Packs", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Select active packs:")
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(availablePacks) { pack ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        vm.selectedPacks = if (pack in vm.selectedPacks) {
                            vm.selectedPacks - pack
                        } else {
                            vm.selectedPacks + pack
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (pack in vm.selectedPacks)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pack, style = MaterialTheme.typography.bodyLarge)
                        Checkbox(
                            checked = pack in vm.selectedPacks,
                            onCheckedChange = null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}
