package com.helldeck.ui.scenes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.helldeck.content.model.Player
import com.helldeck.ui.HelldeckAnimations
import com.helldeck.ui.HelldeckColors
import com.helldeck.ui.HelldeckHeights
import com.helldeck.ui.HelldeckRadius
import com.helldeck.ui.HelldeckSpacing
import com.helldeck.ui.LocalReducedMotion
import com.helldeck.ui.components.GlowButton
import com.helldeck.ui.components.InfoBanner
import com.helldeck.ui.components.NeonCard
import com.helldeck.ui.components.OutlineButton

/**
 * Polished round interaction controls rendered inside [RoundScene].
 *
 * These flows are intentionally simple: one clear prompt, large touch targets,
 * visible progress, and an obvious lock-in action.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarVoteFlow(
    players: List<Player>,
    onVote: (voterId: String, targetId: String) -> Unit,
    onDone: () -> Unit,
    onManagePlayers: (() -> Unit)? = null,
) {
    var voterIndex by remember { mutableIntStateOf(0) }
    var chosenId by remember { mutableStateOf<String?>(null) }

    if (players.isEmpty()) {
        EmptyActivePlayers(onManagePlayers)
        return
    }

    val voter = players[voterIndex.coerceIn(0, players.lastIndex)]

    InteractionPanel(
        title = "Vote privately",
        subtitle = "Pass the phone to each player. The room only sees the final damage.",
        accentColor = HelldeckColors.colorPrimary,
    ) {
        TurnProgressHeader(
            label = "Voting now",
            current = voterIndex + 1,
            total = players.size,
            player = voter,
            accentColor = HelldeckColors.colorPrimary,
        )

        PlayerTargetGrid(
            players = players,
            selectedId = chosenId,
            onPick = { chosenId = it },
        )

        LockRow(
            canLock = chosenId != null,
            skipLabel = "Skip",
            lockLabel = if (voterIndex < players.lastIndex) "Lock & Next" else "Finish Voting",
            onSkip = {
                chosenId = null
                if (voterIndex < players.lastIndex) voterIndex++ else onDone()
            },
            onLock = {
                chosenId?.let { targetId -> onVote(voter.id, targetId) }
                chosenId = null
                if (voterIndex < players.lastIndex) voterIndex++ else onDone()
            },
            accentColor = HelldeckColors.colorPrimary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleAvatarPickFlow(
    players: List<Player>,
    onPick: (targetId: String) -> Unit,
    onManagePlayers: (() -> Unit)? = null,
    title: String = "Pick a target",
) {
    var chosenId by remember { mutableStateOf<String?>(null) }

    if (players.isEmpty()) {
        EmptyActivePlayers(onManagePlayers)
        return
    }

    InteractionPanel(
        title = title,
        subtitle = "Choose one seat and lock it in.",
        accentColor = HelldeckColors.colorSecondary,
    ) {
        PlayerTargetGrid(
            players = players,
            selectedId = chosenId,
            onPick = { chosenId = it },
        )

        GlowButton(
            text = "Lock Target",
            onClick = { chosenId?.let(onPick) },
            enabled = chosenId != null,
            modifier = Modifier.fillMaxWidth(),
            accentColor = HelldeckColors.colorSecondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionsPickFlow(
    title: String,
    options: List<String>,
    onPick: (String) -> Unit,
) {
    InteractionPanel(
        title = title,
        subtitle = "One tap decides it.",
        accentColor = HelldeckColors.colorAccentWarm,
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            maxItemsInEachRow = 2,
        ) {
            options.forEachIndexed { index, option ->
                SelectableTile(
                    label = option,
                    eyebrow = "Option ${index + 1}",
                    selected = false,
                    accentColor = HelldeckColors.colorAccentWarm,
                    onClick = { onPick(option) },
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 132.dp),
                )
            }
        }
    }
}

@Composable
fun TabooFlow(
    clue: String,
    taboos: List<String>,
    running: Boolean,
    onStart: () -> Unit,
    onDone: () -> Unit,
) {
    InteractionPanel(
        title = if (running) "Timer is live" else "Clue giver only",
        subtitle = if (running) {
            "Get guesses fast. If a forbidden word slips out, call it immediately."
        } else {
            "Do not say these words. Start the timer when the clue giver is ready."
        },
        accentColor = if (running) HelldeckColors.colorSecondary else HelldeckColors.colorAccentCool,
    ) {
        Text(
            text = clue,
            style = MaterialTheme.typography.bodyMedium,
            color = HelldeckColors.colorMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        ForbiddenWords(words = taboos)

        GlowButton(
            text = if (running) "Lock Round" else "Start Timer",
            onClick = if (running) onDone else onStart,
            modifier = Modifier.fillMaxWidth(),
            accentColor = if (running) HelldeckColors.colorSecondary else HelldeckColors.colorAccentCool,
        )
    }
}

@Composable
fun ABVoteFlow(
    players: List<Player>,
    preChoiceLabel: String,
    preChoices: List<String>,
    preChoice: String?,
    onPreChoice: (String) -> Unit,
    leftLabel: String,
    rightLabel: String,
    onVote: (voterId: String, choice: String) -> Unit,
    onDone: () -> Unit,
    onManagePlayers: (() -> Unit)? = null,
) {
    var voterIndex by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var preChoiceLocked by remember { mutableStateOf(preChoice != null || preChoices.isEmpty()) }

    InteractionPanel(
        title = if (!preChoiceLocked) "Set the table" else "Vote A or B",
        subtitle = if (!preChoiceLocked) {
            preChoiceLabel.ifBlank { "Pick the side before the room votes." }
        } else {
            "Pass the phone. Each player gets one clean vote."
        },
        accentColor = HelldeckColors.colorAccentCool,
    ) {
        if (!preChoiceLocked && preChoices.isNotEmpty()) {
            ChoicePair(
                leftLabel = preChoices[0],
                rightLabel = preChoices.getOrElse(1) { "B" },
                selected = preChoice,
                leftAccent = HelldeckColors.colorPrimary,
                rightAccent = HelldeckColors.colorAccentCool,
                onSelected = {
                    onPreChoice(it)
                    preChoiceLocked = true
                },
            )
            return@InteractionPanel
        }

        if (players.isEmpty()) {
            EmptyActivePlayers(onManagePlayers)
            return@InteractionPanel
        }

        val voter = players[voterIndex.coerceIn(0, players.lastIndex)]

        TurnProgressHeader(
            label = "Voting now",
            current = voterIndex + 1,
            total = players.size,
            player = voter,
            accentColor = HelldeckColors.colorAccentCool,
        )

        ChoicePair(
            leftLabel = leftLabel,
            rightLabel = rightLabel,
            selected = chosen,
            leftAccent = HelldeckColors.colorPrimary,
            rightAccent = HelldeckColors.colorAccentCool,
            onSelected = { chosen = it },
        )

        LockRow(
            canLock = chosen != null,
            skipLabel = "Skip",
            lockLabel = if (voterIndex < players.lastIndex) "Lock & Next" else "Finish Voting",
            onSkip = {
                chosen = null
                if (voterIndex < players.lastIndex) voterIndex++ else onDone()
            },
            onLock = {
                chosen?.let { choice -> onVote(voter.id, choice) }
                chosen = null
                if (voterIndex < players.lastIndex) voterIndex++ else onDone()
            },
            accentColor = HelldeckColors.colorAccentCool,
        )
    }
}

@Composable
fun JudgePickFlow(
    judge: Player?,
    options: List<String>,
    onPick: (String) -> Unit,
) {
    InteractionPanel(
        title = "Judge decides",
        subtitle = "The judge gets the final word. Keep it moving.",
        accentColor = HelldeckColors.colorAccentWarm,
    ) {
        JudgeHeader(judge = judge)

        options.forEachIndexed { index, option ->
            SelectableTile(
                label = option,
                eyebrow = "Pick ${index + 1}",
                selected = false,
                accentColor = HelldeckColors.colorAccentWarm,
                onClick = { onPick(option) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InteractionPanel(
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    NeonCard(
        modifier = modifier.fillMaxWidth(),
        accentColor = accentColor,
        elevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Tiny.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HelldeckColors.colorMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            content()
        }
    }
}

@Composable
private fun EmptyActivePlayers(onManagePlayers: (() -> Unit)?) {
    InfoBanner(
        message = "No active players are ready for this round.",
        icon = "!",
        modifier = Modifier.fillMaxWidth(),
    )
    if (onManagePlayers != null) {
        Spacer(modifier = Modifier.height(HelldeckSpacing.Medium.dp))
        OutlineButton(
            text = "Manage Players",
            onClick = onManagePlayers,
            modifier = Modifier.fillMaxWidth(),
            accentColor = HelldeckColors.colorAccentCool,
        )
    }
}

@Composable
private fun TurnProgressHeader(
    label: String,
    current: Int,
    total: Int,
    player: Player,
    accentColor: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        color = HelldeckColors.surfacePrimary.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = player.avatar,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                Text(
                    text = player.name.ifBlank { "Seat $current" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HelldeckColors.colorOnDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = "$current/$total",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = accentColor,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerTargetGrid(
    players: List<Player>,
    selectedId: String?,
    onPick: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        maxItemsInEachRow = 3,
    ) {
        players.forEachIndexed { index, player ->
            PlayerChoiceTile(
                player = player,
                seatNumber = index + 1,
                selected = selectedId == player.id,
                onClick = { onPick(player.id) },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 104.dp),
            )
        }
    }
}

@Composable
private fun PlayerChoiceTile(
    player: Player,
    seatNumber: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableTile(
        label = player.name.ifBlank { "Seat $seatNumber" },
        eyebrow = "Seat $seatNumber",
        selected = selected,
        accentColor = HelldeckColors.colorSecondary,
        onClick = onClick,
        modifier = modifier.heightIn(min = 104.dp),
        leading = {
            Text(
                text = player.avatar,
                style = MaterialTheme.typography.headlineLarge,
            )
        },
    )
}

@Composable
private fun ChoicePair(
    leftLabel: String,
    rightLabel: String,
    selected: String?,
    leftAccent: Color,
    rightAccent: Color,
    onSelected: (String) -> Unit,
) {
    val stacked = leftLabel.length + rightLabel.length > 28
    val arrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp)

    if (stacked) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = arrangement,
        ) {
            SelectableTile(
                label = leftLabel,
                eyebrow = "A",
                selected = selected == leftLabel,
                accentColor = leftAccent,
                onClick = { onSelected(leftLabel) },
                modifier = Modifier.fillMaxWidth(),
            )
            SelectableTile(
                label = rightLabel,
                eyebrow = "B",
                selected = selected == rightLabel,
                accentColor = rightAccent,
                onClick = { onSelected(rightLabel) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = arrangement,
        ) {
            SelectableTile(
                label = leftLabel,
                eyebrow = "A",
                selected = selected == leftLabel,
                accentColor = leftAccent,
                onClick = { onSelected(leftLabel) },
                modifier = Modifier.weight(1f),
            )
            SelectableTile(
                label = rightLabel,
                eyebrow = "B",
                selected = selected == rightLabel,
                accentColor = rightAccent,
                onClick = { onSelected(rightLabel) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SelectableTile(
    label: String,
    eyebrow: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            selected -> 1.03f
            isPressed -> 0.97f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessHigh)
        },
        label = "selectable_tile_scale",
    )

    Surface(
        modifier = modifier
            .heightIn(min = HelldeckHeights.RecommendedTapTarget.dp + 12.dp)
            .scale(scale)
            .shadow(
                elevation = if (selected) 12.dp else if (isPressed) 5.dp else 2.dp,
                shape = RoundedCornerShape(HelldeckRadius.Large),
                spotColor = accentColor.copy(alpha = if (selected) 0.45f else 0.12f),
                ambientColor = accentColor.copy(alpha = if (selected) 0.3f else 0.08f),
            )
            .semantics {
                contentDescription = "$eyebrow: $label${if (selected) ", selected" else ""}"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(HelldeckRadius.Large),
        color = if (selected) {
            accentColor.copy(alpha = 0.18f)
        } else {
            HelldeckColors.surfaceElevated.copy(alpha = 0.86f)
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else HelldeckColors.colorMuted.copy(alpha = 0.28f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (selected) 0.05f else 0.02f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(
                    horizontal = HelldeckSpacing.Medium.dp,
                    vertical = HelldeckSpacing.Small.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (selected) accentColor else HelldeckColors.colorMuted,
                    maxLines = 1,
                )
                AnimatedVisibility(visible = selected) {
                    Text(
                        text = "LOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                    )
                }
            }

            if (leading != null) {
                Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))
                leading()
            }

            Spacer(modifier = Modifier.height(HelldeckSpacing.Tiny.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) accentColor else HelldeckColors.colorOnDark,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LockRow(
    canLock: Boolean,
    skipLabel: String,
    lockLabel: String,
    onSkip: () -> Unit,
    onLock: () -> Unit,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .weight(0.8f)
                .height(HelldeckHeights.Button.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = HelldeckColors.colorMuted,
            ),
            contentPadding = PaddingValues(horizontal = HelldeckSpacing.Small.dp),
        ) {
            Text(
                text = skipLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        GlowButton(
            text = lockLabel,
            onClick = onLock,
            enabled = canLock,
            modifier = Modifier.weight(1.8f),
            accentColor = accentColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ForbiddenWords(words: List<String>) {
    val visibleWords = if (words.isEmpty()) listOf("No forbidden words loaded") else words.take(6)

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Forbidden words" },
        horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(HelldeckSpacing.Small.dp),
    ) {
        visibleWords.forEach { word ->
            Surface(
                shape = RoundedCornerShape(HelldeckRadius.Pill),
                color = HelldeckColors.Error.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, HelldeckColors.Error.copy(alpha = 0.5f)),
            ) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = HelldeckColors.colorOnDark,
                    modifier = Modifier.padding(horizontal = HelldeckSpacing.Medium.dp, vertical = HelldeckSpacing.Small.dp),
                )
            }
        }
    }
}

@Composable
private fun JudgeHeader(judge: Player?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HelldeckRadius.Large),
        color = HelldeckColors.colorAccentWarm.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, HelldeckColors.colorAccentWarm.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(HelldeckSpacing.Medium.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HelldeckSpacing.Medium.dp),
        ) {
            Text(
                text = judge?.avatar ?: "!",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.alpha(if (judge == null) 0.55f else 1f),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current judge",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HelldeckColors.colorAccentWarm,
                )
                Text(
                    text = judge?.name?.ifBlank { "Seat" } ?: "No judge assigned",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HelldeckColors.colorOnDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
