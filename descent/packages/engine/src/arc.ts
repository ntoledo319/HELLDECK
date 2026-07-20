// Arc builder — DESCENT_BUILD_SPEC.md 4.3. Deterministic given (config, players, seed).
// The grammar exists because simulations broke without it: openers are simultaneous,
// spikes land mid-descent, spotlights wait for a warm room, subject-targeting never
// stacks late-night, and the finale pays everyone.
import type { CircleSpec, DeckId, NightConfig, Player, Tier } from './types.js';
import { genericCeiling, maxLegalE } from './consent.js';
import { rng } from './rng.js';

export interface GameInfo {
  deck: DeckId;
  minN: number;
  simultaneous: boolean; // slot-0 eligible: everyone acts at once, zero spotlight pressure
  outward: boolean; // aim-outward class (anti-roast-fatigue palate cleanser)
  subjectTargeting: boolean; // content ABOUT a present person
  spotlight: boolean; // single-performer — only after the room is warm (slot index >= 3)
  allPlayersScore: boolean; // finale + Devil's Bargain eligible
  voteEmergent: boolean; // roast-class consent rule (4.4)
  repeatable: boolean; // sanctioned repeats: roast, scatter, overunder — never adjacent, max 1 repeat
  irlGated: boolean; // requires irlFamiliar crew
  available: boolean; // v1 ships LAUNCH 8 + the titlefight spike; Drop 1 flips these on
  loops: (nPlayers: number) => number;
}

export const GAMES: Record<DeckId, GameInfo> = {
  // deck            minN  simult outward subj   spot   allPS  voteEm repeat irl    avail  loops
  roast: g('roast', 3, true, false, true, false, true, true, true, false, true, () => 3),
  fillin: g('fillin', 3, true, false, false, false, true, false, false, false, true, (n) => (n <= 6 ? 1 : 2)),
  overunder: g('overunder', 3, true, false, true, false, true, false, true, false, true, () => 2),
  confession: g('confession', 3, false, false, false, true, false, false, false, false, true, () => 2),
  scatter: g('scatter', 3, false, true, false, false, false, false, true, false, true, () => 3),
  poison: g('poison', 3, false, true, false, true, false, false, false, false, true, () => 2),
  redflag: g('redflag', 3, false, true, false, true, false, false, false, false, true, () => 2),
  alibi: g('alibi', 3, false, false, false, true, false, false, false, false, true, () => 2),
  titlefight: g('titlefight', 3, false, true, false, true, false, false, false, false, true, () => 1),
  // Content Drop 1 — spec'd for the grammar, gated off until D-7xx:
  texttrap: g('texttrap', 3, false, false, false, true, false, false, false, false, false, () => 2),
  reality: g('reality', 3, false, false, true, true, false, false, false, true, false, () => 2),
  taboo: g('taboo', 4, false, true, false, true, false, false, false, false, false, () => 2),
  hotseat: g('hotseat', 5, false, false, true, true, false, false, false, true, false, () => 1),
};

function g(
  deck: DeckId,
  minN: number,
  simultaneous: boolean,
  outward: boolean,
  subjectTargeting: boolean,
  spotlight: boolean,
  allPlayersScore: boolean,
  voteEmergent: boolean,
  repeatable: boolean,
  irlGated: boolean,
  available: boolean,
  loops: (n: number) => number,
): GameInfo {
  return { deck, minN, simultaneous, outward, subjectTargeting, spotlight, allPlayersScore, voteEmergent, repeatable, irlGated, available, loops };
}

/**
 * Games that PRE-ASSIGN a spotlight victim (subject / confessor / defender / accused /
 * pitchers / fighters) — these open the 4.5 "WHO WANTS BLOOD?" volunteer claim during
 * CIRCLE_INTRO. Roast is subject-targeting but vote-emergent (the victim emerges from the
 * ballots, never pre-assigned), so it is excluded; simultaneous games (fillin/scatter) have
 * no single victim. Captures exactly {overunder, confession, poison, redflag, alibi, titlefight}.
 */
export function assignsSpotlight(game: DeckId): boolean {
  const info = GAMES[game];
  return info.spotlight || (info.subjectTargeting && !info.voteEmergent);
}

const OPENERS: DeckId[] = ['roast', 'overunder', 'fillin']; // slot 0 candidates (4.3)
const ALL_PLAYERS_SCORE: DeckId[] = ['roast', 'fillin', 'overunder'];

/** First index of the last third — the Devil's Bargain hunting ground. */
export function lastThirdStart(depth: number): number {
  return depth - Math.ceil(depth / 3);
}

/**
 * Slot forced to be all-players-score so a non-finale bargain candidate always
 * exists in the last third (bargain never lands on the finale — by construction).
 */
export function bargainSlotFor(depth: 5 | 7 | 9): number {
  return depth === 5 ? 3 : depth === 7 ? 4 : 7;
}

const VIBE_RUNG: Record<NightConfig['vibe'], number> = { sober: 1, warm: 2, feral: 3 };

/** E-curve (4.3): start at the vibe rung (ceiling always wins), ramp to maxLegal by the finale. */
export function rungFor(
  circleIdx: number,
  depth: number,
  game: DeckId,
  vibe: NightConfig['vibe'],
  ceilings: readonly Tier[],
): Tier {
  const gc = genericCeiling(ceilings);
  const startRung = Math.max(1, Math.min(VIBE_RUNG[vibe] as number, gc - 2));
  const maxLegal = gc;
  const c = circleIdx + 1; // spec's rung(c) is 1-indexed
  const ramp =
    depth <= 1 ? maxLegal : startRung + Math.floor(((c - 1) * (maxLegal - startRung)) / (depth - 1));
  const tierCeil = maxLegalE({ ceilings, voteEmergent: GAMES[game].voteEmergent });
  return Math.max(1, Math.min(ramp, tierCeil)) as Tier;
}

/**
 * buildArc — deterministic, seeded. Grammar enforced (property-tested, 1000 seeds):
 *  - slot 0 is a simultaneous-input game
 *  - scatter spike at mid; titlefight second spike at mid+2 when depth >= 7
 *  - finale is all-players-score (N<=4 -> overunder)
 *  - spotlight (single-performer) games only from slot index 2 on (a 2-circle warm-up).
 *    Was 3, which — with the forced opener/scatter/titlefight/bargain/finale slots — made
 *    confession/poison/redflag/alibi UNREACHABLE at depth 5 and 7 (council finding).
 *  - from circle 5 (0-indexed i>=4) a subject-targeting circle never follows another
 *  - >=1 outward circle per ceil(depth/3) block, except the first block (structurally
 *    inward: the slot-0 mandate + spotlight gate leave no outward game placeable there
 *    — the night is coldest at the start; fatigue accrues later) and a finale-only block
 *  - repeats only {roast, scatter, overunder}, never adjacent, max 1 repeat each
 *  - exactly one bargain circle: all-players-score, in the last third, never the finale
 */
export function buildArc(config: NightConfig, players: Player[], seed: string): CircleSpec[] {
  const actives = players.filter((p) => p.role !== 'imp');
  const n = actives.length;
  const d = config.depth;
  const rand = rng(`arc:${seed}:${config.crewId}:${d}:${n}:${config.vibe}`);
  const ceilings = actives.map((p) => p.heatCeiling);

  const pool = (Object.values(GAMES) as GameInfo[]).filter(
    (info) => info.available && info.minN <= n && (!info.irlGated || config.irlFamiliar),
  );
  const poolIds = new Set(pool.map((p) => p.deck));
  const sanctionedRepeats = pool.filter((p) => p.repeatable).length;
  if (d > pool.length + sanctionedRepeats) {
    // The lobby UI caps depth first (4.3); this is the engine's backstop.
    throw new Error(`depth ${d} exceeds legal game pool (${pool.length}+${sanctionedRepeats})`);
  }

  const slots: (DeckId | null)[] = new Array<DeckId | null>(d).fill(null);
  const mid = Math.floor(d / 2);
  slots[mid] = 'scatter';
  if (d >= 7) slots[mid + 2] = 'titlefight';

  const count = (game: DeckId): number => slots.filter((s) => s === game).length;
  const canPlace = (game: DeckId, idx: number): boolean => {
    if (!poolIds.has(game) || slots[idx] !== null) return false;
    const info = GAMES[game];
    if (count(game) >= (info.repeatable ? 2 : 1)) return false;
    if (info.spotlight && idx < 2) return false; // spotlight only after slot 1 (warm-up buffer)
    for (const nb of [idx - 1, idx + 1]) {
      const other = slots[nb];
      if (other === undefined || other === null) continue;
      if (other === game) return false; // repeats never adjacent
      if (info.subjectTargeting && GAMES[other].subjectTargeting && Math.max(idx, nb) >= 4)
        return false; // no back-to-back subject-targeting after circle 4
    }
    return true;
  };

  // Fill the rest by seeded DFS: fresh games first, sanctioned repeats as the
  // fallback, backtracking on dead ends (greedy starves — e.g. d=7 with roast
  // opener + roast finale, a fresh overunder at slot 1 leaves slot 2 with nothing).
  const fillOrder = shuffled(rand, pool);
  const fillRemaining = (from: number): boolean => {
    let idx = from;
    while (idx < d && slots[idx] !== null) idx++;
    if (idx >= d) return true;
    const fresh = fillOrder.filter((info) => count(info.deck) === 0 && canPlace(info.deck, idx));
    const repeats = fillOrder.filter((info) => count(info.deck) > 0 && canPlace(info.deck, idx));
    for (const info of [...fresh, ...repeats]) {
      slots[idx] = info.deck;
      if (fillRemaining(idx + 1)) return true;
      slots[idx] = null;
    }
    return false;
  };

  // slot 0 / finale / bargain slot are entangled with the fill (adjacency + repeat
  // budgets), so commit the first seeded triple whose remainder is satisfiable.
  const finaleIdx = d - 1;
  const bargainIdx = bargainSlotFor(d);
  const openerOrder = shuffled(rand, OPENERS.filter((x) => poolIds.has(x)));
  const finaleBase = ALL_PLAYERS_SCORE.filter((x) => poolIds.has(x));
  const finaleOrder = n <= 4
    ? ['overunder' as DeckId, ...shuffled(rand, finaleBase.filter((x) => x !== 'overunder'))]
    : shuffled(rand, finaleBase);
  const bargainOrder = shuffled(rand, ALL_PLAYERS_SCORE.filter((x) => poolIds.has(x)));
  let committed = false;
  outer: for (const opener of openerOrder) {
    if (!canPlace(opener, 0)) continue;
    slots[0] = opener;
    for (const finale of finaleOrder) {
      if (!canPlace(finale, finaleIdx)) continue;
      slots[finaleIdx] = finale;
      for (const bargainGame of bargainOrder) {
        if (!canPlace(bargainGame, bargainIdx)) continue;
        slots[bargainIdx] = bargainGame;
        if (fillRemaining(0)) {
          committed = true;
          break outer;
        }
        slots[bargainIdx] = null;
      }
      slots[finaleIdx] = null;
    }
    slots[0] = null;
  }
  if (!committed) throw new Error('arc: no legal opener/finale/bargain arrangement');

  const arc: CircleSpec[] = slots.map((game, idx) => {
    const deck = game as DeckId;
    return {
      game: deck,
      loops: GAMES[deck].loops(n),
      finale: idx === finaleIdx,
      outward: GAMES[deck].outward,
      rung: rungFor(idx, d, deck, config.vibe, ceilings),
      bargain: false,
    };
  });

  // Devil's Bargain (4.3): random all-players-score circle in the last third, never the finale.
  const candidates: number[] = [];
  for (let idx = lastThirdStart(d); idx < finaleIdx; idx++) {
    const spec = arc[idx];
    if (spec && GAMES[spec.game].allPlayersScore) candidates.push(idx);
  }
  const pickIdx = candidates[Math.floor(rand() * candidates.length)];
  if (pickIdx === undefined) throw new Error('arc: no bargain candidate'); // impossible: bargainIdx qualifies
  (arc[pickIdx] as CircleSpec).bargain = true;

  return arc;
}

function shuffled<T>(rand: () => number, arr: readonly T[]): T[] {
  const out = [...arr];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [out[i], out[j]] = [out[j] as T, out[i] as T];
  }
  return out;
}
