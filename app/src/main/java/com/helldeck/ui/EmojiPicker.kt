package com.helldeck.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class EmojiCategory(val name: String, val items: List<String>)

// Curated emoji sets (200+ total) across categories for broad choice
private val Faces = listOf(
    "😀","😃","😄","😁","😆","😅","😂","🤣","😊","😇","🙂","🙃","😉","😌","😍","🥰","😘","😗","😙","😚",
    "😋","😛","😝","😜","🤪","🤨","🧐","🤓","😎","🥸","🤩","🥳","😏","😒","😞","😔","😟","😕","🙁","☹️",
    "😣","😖","😫","😩","🥱","😤","😠","😡","🤬","😳","🥵","🥶","😱","😨","😰","😥","😓","🤗","🤔","🫣",
    "🤭","🤫","🤥","😶","🫥","😐","🫤","😑","😬","🙄","😮","😯","😲","😴","🤤","😪","😵","🤐","🥴","🤢",
    "🤮","🤧","😷","🤒","🤕","🤑","🤠","😈","👿","💀","☠️","👻","👽","🤖","💩"
)

private val Animals = listOf(
    "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐵","🐔","🐧","🐦","🦆","🦅","🦉",
    "🦇","🦋","🐞","🐙","🦑","🦐","🐠","🐟","🐬","🐳","🦈","🐊","🐢","🦎","🐍","🐅","🐆","🦓","🦍","🦧",
    "🦣","🐘","🦬","🐪","🐫","🦒","🦘","🐃","🐂","🐄","🐎","🐖","🐏","🐑","🫏","🫎"
)

private val Food = listOf(
    "🍏","🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🍆","🥑","🥦","🥬",
    "🥒","🌶️","🌽","🥕","🧄","🧅","🥔","🍞","🥐","🥖","🫓","🥨","🧀","🥚","🍳","🥞","🧇","🥓","🥩","🍗",
    "🍖","🌭","🍔","🍟","🍕","🥪","🌮","🌯","🫔","🥙","🥗","🍝","🍜","🍣","🍱","🍤","🍙","🍚","🍘","🍥"
)

private val Activities = listOf(
    "⚽","🏀","🏈","⚾","🎾","🏐","🏉","🎱","🏓","🏸","🥅","🏒","🏑","🏏","🥍","⛳","🪁","🎣","🤿","🥊",
    "🥋","🎽","🛹","🛼","🎿","⛷️","🏂","🪂","🏋️","🤼","🤸","⛹️","🤺","🤾","🏌️","🏇","🚴","🚵","🧗","🏊"
)

private val Objects = listOf(
    "📱","💻","⌚","🖥️","🖨️","🖱️","💡","🔦","📷","🎥","🎙️","🎧","📀","📦","📫","📬","📎","✂️","🧷","🧵",
    "🧶","🔧","🪛","🔩","⚙️","🧰","🔨","⛏️","🪓","🪚","🪜","🧪","🧫","🧬","🔬","🔭","📡","🧯","🚪","🪑"
)

private val Symbols = listOf(
    "❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","🔞","✅",
    "☑️","✔️","❌","➕","➖","➗","✳️","✴️","❇️","⚠️","🚸","⛔","🚫","🔞","♻️","🔆","🔅","🆕","🆙","🆒"
)

private val emojiCategories = listOf(
    EmojiCategory("Faces", Faces),
    EmojiCategory("Animals", Animals),
    EmojiCategory("Food", Food),
    EmojiCategory("Activities", Activities),
    EmojiCategory("Objects", Objects),
    EmojiCategory("Symbols", Symbols)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPicker(
    show: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    if (!show) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tabIndex by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    val allEmojis = remember { emojiCategories.flatMap { it.items } }
    val keywordMap = remember {
        mapOf(
            "face" to Faces,
            "smile" to Faces.take(20),
            "laugh" to Faces.filter { it in listOf("😆","😅","😂","🤣","😄","😁") },
            "animal" to Animals,
            "cat" to listOf("🐱"),
            "dog" to listOf("🐶"),
            "fox" to listOf("🦊"),
            "panda" to listOf("🐼"),
            "lion" to listOf("🦁"),
            "tiger" to listOf("🐯"),
            "food" to Food,
            "pizza" to listOf("🍕"),
            "burger" to listOf("🍔"),
            "apple" to listOf("🍎","🍏"),
            "sports" to Activities,
            "ball" to listOf("⚽","🏀","🏈","⚾","🎾","🏐","🎱"),
            "soccer" to listOf("⚽"),
            "tech" to Objects,
            "phone" to listOf("📱"),
            "laptop" to listOf("💻"),
            "camera" to listOf("📷","🎥"),
            "heart" to Symbols.filter { it.contains("❤") || it in listOf("❤️","💖","💘","💝","💕","💞","💓","💗","💔") },
            "star" to listOf("✳️","✴️"),
            "check" to listOf("✅","✔️","☑️"),
            "warn" to listOf("⚠️"),
            "cool" to listOf("🆒"),
            "new" to listOf("🆕")
        )
    }

    fun filterEmojis(q: String): List<String> {
        val t = q.trim().lowercase()
        if (t.isBlank()) return emojiCategories[tabIndex].items
        // If user pasted an emoji, show it if we have it
        val pasted = t.filter { it.toString() in allEmojis }
        val pastedList = pasted.map { it.toString() }
        val byKeyword = keywordMap.filter { (k, _) -> k.contains(t) || t.contains(k) }.flatMap { it.value }
        val byCategory = emojiCategories.filter { it.name.lowercase().contains(t) }.flatMap { it.items }
        val combined = (pastedList + byKeyword + byCategory).ifEmpty { allEmojis }
        return combined.distinct()
    }

    val itemsToShow = filterEmojis(query)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp)
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                emojiCategories.forEachIndexed { idx, cat ->
                    Tab(selected = tabIndex == idx, onClick = { tabIndex = idx }, text = { Text(cat.name) })
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search (name or emoji)") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(itemsToShow) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clickable {
                                onPick(emoji)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Close") }
        }
    }
}
