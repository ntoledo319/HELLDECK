// botlogic.ts — the shared, view-driven decision function for headless bots.
// A bot is a thin renderer just like a real phone: it reads its own per-socket
// redacted `gameView` and emits the wire messages a real player would tap. It
// knows NOTHING the server didn't hand it (own-ballot echoes, `youAreScribe`,
// `youAreSubject`, `youAreConfessor`, teleprompter/assignment) — so driving a bot
// night is also a redaction proof: a game a bot can complete is a game whose view
// carried every seat the mechanic needs, and nothing it didn't.
//
// Covers the 4 registered M2 games (roast, fillin, overunder, confession) across
// every sub-phase, INCLUDING the two blocking inputs (over/under TRUTH number,
// confession TRUE/FALSE lock) — the ones the arc's other games can't fake. Used by
// BOTH packages/server/test/protocol.test.ts (fake-socket unit night) and
// packages/server/test/bots.ts (live wrangler-dev night).

export interface RosterMember {
  id: string;
  role?: string;
}
export type WireMsg = { t: string; [k: string]: unknown };

// ---- defensive readers (gameView is `unknown` off the wire) ----
const obj = (v: unknown): Record<string, unknown> | null =>
  typeof v === 'object' && v !== null ? (v as Record<string, unknown>) : null;
const str = (v: unknown): string | null => (typeof v === 'string' ? v : null);
export const deckOf = (gv: unknown): string | null => str(obj(gv)?.['deck']);
export const subOf = (gv: unknown): string | null => str(obj(gv)?.['sub']);

const input = (p: Record<string, unknown>): WireMsg => ({ t: 'INPUT', p });
const fire = (): WireMsg => ({ t: 'FIRE', n: 1 + Math.floor(Math.random() * 3) });

/**
 * Decide this bot's wire messages for its CURRENT gameView. Pure w.r.t. the view
 * (the small random in FIRE is cosmetic telemetry). Returns [] for phases a bot
 * has nothing to do in (INTRO / DEAL / LADDER / null view / already-acted).
 *
 * REVEAL: everyone fires; the host DESCENDs to end the hold early (4.2) so a bot
 * night doesn't idle through 20s holds — real reveals hold for the roast.
 */
export function botMoves(
  gv: unknown,
  youId: string,
  roster: readonly RosterMember[],
  isHost: boolean,
): WireMsg[] {
  const g = obj(gv);
  if (!g) return [];
  const deck = deckOf(gv);
  const sub = subOf(gv);
  if (sub === 'REVEAL') return isHost ? [fire(), { t: 'DESCEND' }] : [fire()];
  const someoneElse = (): string | null => roster.find((p) => p.id !== youId)?.id ?? null;

  if (deck === 'roast') {
    if (sub === 'VOTE' && g['youVoted'] == null) {
      const target = someoneElse();
      return target ? [input({ vote: target })] : [];
    }
    return [];
  }

  if (deck === 'fillin') {
    if (sub === 'WRITE') {
      const you = obj(g['you']);
      if (!you) return [];
      const msgs: WireMsg[] = [];
      if (you['isReader'] === true && you['yourTone'] == null) msgs.push(input({ tone: 0 }));
      if (you['setup'] != null && you['yourAnswer'] == null) msgs.push(input({ answer: `${youId} did it and would again` }));
      return msgs;
    }
    if (sub === 'PERFORM') {
      // The one phone holding the teleprompter drives the read; in derange each
      // performer signals when they've sold their assigned line.
      if (obj(g['teleprompter'])) {
        return g['mode'] === 'derange' || g['stage'] === 'faceoff'
          ? [input({ read: true })]
          : [input({ next: true })];
      }
      if (obj(g['assignment'])) return [input({ read: true })]; // derange performer, own line
      return [fire()]; // audience
    }
    if (sub === 'VOTE' && g['youVoted'] == null) {
      const ballot = Array.isArray(g['ballot']) ? (g['ballot'] as Record<string, unknown>[]) : [];
      const pickable = ballot.find((e) => e['yours'] !== true);
      return pickable ? [input({ vote: pickable['id'] })] : [];
    }
    return [];
  }

  if (deck === 'overunder') {
    if (sub === 'DEBATE' && g['youAreScribe'] === true) return [input({ line: 5, lock: true })];
    if (sub === 'BET' && g['youAreSubject'] !== true && g['youBet'] == null) return [input({ bet: 'over' })];
    // THE blocking truth number — only the subject can end the pause; never fabricated (4.7/D-115).
    if (sub === 'TRUTH' && g['youAreSubject'] === true) return [input({ truth: 7 })];
    return [];
  }

  if (deck === 'confession') {
    if (sub === 'PICK' && g['youAreConfessor'] === true) return [input({ pick: 0 })];
    // THE blocking truth lock — only the confessor can end the pause.
    if (sub === 'LOCK' && g['youAreConfessor'] === true) return [input({ truth: true })];
    if (sub === 'PERFORM' && g['youAreConfessor'] === true) return [{ t: 'REST' }]; // I REST MY CASE — end the clock early
    if (sub === 'JURY' && g['youAreConfessor'] !== true && g['youVoted'] == null) return [input({ vote: 'believe' })];
    return [];
  }

  // ===== M3 games =====
  if (deck === 'scatter') {
    // BOMB self-advances on its hidden fuse (no input). At BOOM, everyone taps who died.
    if (sub === 'BOOM' && g['youTapped'] == null) {
      const target = someoneElse();
      return target ? [input({ tap: target })] : [];
    }
    return [];
  }

  if (deck === 'redflag') {
    if (sub === 'DEFENSE' && g['youAreDefender'] === true) return [{ t: 'REST' }]; // sell it, then rest
    if (sub === 'VOTE' && g['youAreDefender'] !== true && g['youVoted'] == null) return [input({ vote: 'smash' })];
    return [];
  }

  if (deck === 'poison') {
    if (sub === 'PITCH' && g['youArePitcher'] != null) return [{ t: 'REST' }]; // pitch made, rest
    if (sub === 'VOTE' && g['youArePitcher'] == null && g['youVoted'] == null) {
      return g['ratingMode'] === true ? [input({ rate: { A: 4, B: 2 } })] : [input({ vote: 'A' })]; // N=3 judge rates
    }
    return [];
  }

  if (deck === 'alibi') {
    if (sub === 'ALIBI' && g['youAreAccused'] === true) return [{ t: 'REST' }];
    if (sub === 'HUNT' && g['youAreAccused'] !== true && g['youPicked'] == null) {
      const lineup = Array.isArray(g['lineup']) ? (g['lineup'] as string[]) : [];
      return lineup.length >= 3 ? [input({ picks: lineup.slice(0, 3) })] : [];
    }
    return [];
  }

  if (deck === 'titlefight') {
    if (sub === 'BOUT' && g['youAreFighter'] === true) return [{ t: 'REST' }];
    if (sub === 'VOTE' && g['youAreFighter'] !== true && g['youVoted'] == null) {
      const a = str(g['fighterA']);
      return a ? [input({ vote: a })] : [];
    }
    return [];
  }

  return [];
}
