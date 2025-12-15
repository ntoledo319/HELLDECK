package com.helldeck.ui.interactions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.helldeck.engine.InteractionType
import com.helldeck.ui.events.RoundEvent
import com.helldeck.ui.state.RoundState

/**
 * Master interaction renderer dispatcher.
 * Routes to specialized renderers based on InteractionType.
 *
 * CRITICAL: This is the ONLY entry point for rendering round interactions.
 * UI must use RoundState.interactionType (from engine) to determine rendering.
 * NO recomputation of interaction types allowed.
 */
@Composable
fun InteractionRenderer(
    roundState: RoundState,
    onEvent: (RoundEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    when (roundState.interactionType) {
        InteractionType.VOTE_PLAYER -> VotePlayerRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.A_B_CHOICE -> ABChoiceRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.TRUE_FALSE -> TrueFalseRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.JUDGE_PICK -> JudgePickRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.SMASH_PASS -> SmashPassRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.TARGET_SELECT -> TargetSelectRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.REPLY_TONE -> ReplyToneRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.TABOO_GUESS -> TabooGuessRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.ODD_EXPLAIN -> OddExplainRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.MINI_DUEL -> MiniDuelRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.HIDE_WORDS -> HideWordsRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.SALES_PITCH -> SalesPitchRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.SPEED_LIST -> SpeedListRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.PREDICT_VOTE -> PredictVoteRenderer(
            roundState = roundState,
            onEvent = onEvent,
            modifier = modifier
        )
        InteractionType.NONE -> EmptyRenderer(
            modifier = modifier
        )
    }
}
