// The one function. Pure, deterministic given (state, event, seed).
// DESCENT_BUILD_SPEC.md Part 2.1 + Part 4. Tasks D-111, D-115, D-116 (core night machine).
//
// ===== module step protocol =====
// The core owns Phase; a GameModule owns one circle. Two ways a module drives the night:
//  1. DIRECTIVES: gameState may carry reserved keys `$phase` / `$deal` / `$spotlight`
//     (types.ts). The core consumes + strips them after every step: `$deal` runs the
//     4.5 card ceremony (backup reserved, burn window, timing-identical), `$spotlight`
//     runs the private fixed-timing performer assignment, and `$phase` moves the room
//     to INPUT / WAITING_ON / REVEAL. A core-owned REVEAL gets the full hold
//     machinery: DESCEND (host anytime, anyone past the 45s softcap), fire-decay
//     (8s of quiet past the 20s minimum hold). When the hold ends, the core calls
//     module.timer(ctx, CORE_REVEAL_DONE); after a deal completes, CORE_DEALT; after
//     a spotlight ceremony settles, CORE_SPOTLIGHT_DONE carries its final role ids.
//  2. SELF-MANAGED: a module may run sub-phases inside its own gameState and schedule
//     its own timers (any id outside the core namespaces below). The core tracks those
//     schedules; a timer whose id contains ':hold:' is treated as a reveal hold that
//     DESCEND can fire early (host anytime, anyone past the softcap).
// step.done -> LADDER (5s) -> next circle or JUDGMENT. Epoch bumps on EVERY phase change.
import type {
  DealRequest,
  Effect,
  GameEvent,
  JudgmentSummary,
  ModuleDirectives,
  PhaseDirective,
  Player,
  PlayerId,
  ReduceResult,
  RoomState,
  SpotlightRequest,
  Superlative,
} from './types.js';
import { rng } from './rng.js';
import { assignsSpotlight, buildArc } from './arc.js';
import { beginDeal, burnDeal, completeDeal } from './deal.js';
import {
  beginSpotlight,
  burnSpotlight,
  completeSpotlight,
  handoffSpotlight,
  spotlightResolution,
} from './spotlight.js';
import { awardScores } from './scoring.js';
import {
  CORE_DEALT,
  CORE_REVEAL_DONE,
  CORE_SPOTLIGHT_DONE,
  type GameCtx,
  type GameModule,
  type GameStep,
} from './games/module.js';
import { roastModule } from './games/roast.js';
import { fillinModule } from './games/fillin.js';
import { overunderModule } from './games/overunder.js';
import { confessionModule } from './games/confession.js';
import { scatterModule } from './games/scatter.js';
import { poisonModule } from './games/poison.js';
import { redflagModule } from './games/redflag.js';
import { alibiModule } from './games/alibi.js';
import { titlefightModule } from './games/titlefight.js';

// ===== tuning =====
export const SEAT_HOLD_MS = 90_000; // disconnect grace before the seat lapses (4.7)
export const LADDER_MS = 5_000;
export const INTRO_TRANSITION_MS = 8_000; // descend transition (4.2)
export const EXPLAINER_FIRST_MS = 15_000; // first time a game appears this night
export const EXPLAINER_REPEAT_MS = 5_000; // title card after
export const CLAIM_MS = 5_000; // 4.5 "WHO WANTS BLOOD?" claim window (hosted inside CIRCLE_INTRO)
export const REVEAL_MIN_HOLD_MS = 20_000;
export const REVEAL_SOFTCAP_MS = 45_000; // past this, ANYONE can DESCEND
export const FIRE_DECAY_QUIET_MS = 8_000; // quiet this long past min hold -> auto-advance
export const REVEAL_FLIP_MS = 3_000; // synced 3-2-1: every phone flips on this AT beat (3.3/5.1)
export const MAX_ACTIVE_PLAYERS = 12;

// Synthetic timer ids the core feeds to module.timer — defined in games/module.ts
// (the contract file) so modules import them cycle-free; re-exported for callers.
export { CORE_DEALT, CORE_REVEAL_DONE } from './games/module.js';
export { CORE_SPOTLIGHT_DONE } from './games/module.js';

const CORE_TIMER_PREFIXES = ['intro:', 'ladder:', 'deal:', 'spotlight:', 'reveal:', 'seathold:'] as const;
const isCoreTimer = (id: string): boolean => CORE_TIMER_PREFIXES.some((p) => id.startsWith(p));

// ===== module registry =====
const registry = new Map<string, GameModule>();
export function registerModule(m: GameModule): void {
  registry.set(m.deck, m);
}
export function getModule(deck: string): GameModule | undefined {
  return registry.get(deck);
}
registerModule(roastModule); // M1
registerModule(fillinModule); // D-121
registerModule(overunderModule); // D-122
registerModule(confessionModule); // D-123
registerModule(scatterModule); // D-131 (M3)
registerModule(poisonModule); // D-132 (M3)
registerModule(redflagModule); // D-133 (M3)
registerModule(alibiModule); // D-133 (M3)
registerModule(titlefightModule); // M3 — the Title Fight spike; every LAUNCH8 + spike now registered

export function initialRoom(code: string, createdAt: number, entitled: boolean): RoomState {
  return {
    code,
    createdAt,
    config: null,
    players: [],
    phase: { k: 'LOBBY' },
    arc: [],
    circleIdx: 0,
    gameState: null,
    usedCardIds: [],
    usedSkeletons: {},
    devilsBargain: null,
    epoch: 0,
    entitled,
    telemetry: [],
    deal: null,
    spotlight: null,
    moduleTimers: {},
    lastFireAt: null,
    nightStats: {},
    judgment: null,
    circleStartScores: {},
    volunteers: [],
  };
}

export function reduce(s: RoomState, e: GameEvent, seed: string): ReduceResult {
  const rand = rng(`${seed}:${s.epoch}:${e.t}`);
  switch (e.t) {
    case 'JOIN':
      return handleJoin(s, e);
    case 'LEAVE':
      return handleLeave(s, e);
    case 'RECONNECT':
      return handleReconnect(s, e);
    case 'ATTEST18':
      return handleAttest(s, e);
    case 'CONFIG':
      return handleConfig(s, e);
    case 'CEILING':
      return handleCeiling(s, e);
    case 'BEGIN':
      return handleBegin(s, e, seed);
    case 'TIMER':
      return handleTimer(s, e, rand);
    case 'INPUT':
      return routeModule(s, e.at, rand, (m, ctx) => m.input(ctx, e.id, e.payload));
    case 'BURN':
      return handleBurn(s, e);
    case 'DESCEND':
      return handleDescend(s, e, rand);
    case 'FIRE':
      return handleFire(s, e);
    case 'CLAIM':
      return handleClaim(s, e);
    case 'PLEAD_FIFTH':
      return routeControl(s, e.id, 'FIFTH', e.at, rand);
    case 'VOID_ROUND':
      return isHost(s, e.id) ? routeControl(s, e.id, 'VOID', e.at, rand) : noop(s);
    case 'SKIP_EM':
      return routeControl(s, e.id, 'SKIPEM', e.at, rand);
    case 'REST_CASE':
      return routeControl(s, e.id, 'REST', e.at, rand);
    default:
      return noop(s);
  }
}

// ===== small helpers =====
const noop = (s: RoomState): ReduceResult => ({ state: s, effects: [] });
const actives = (s: RoomState): Player[] => s.players.filter((p) => p.role !== 'imp');
const impsOf = (s: RoomState): Player[] => s.players.filter((p) => p.role === 'imp');
const isHost = (s: RoomState, id: PlayerId): boolean =>
  s.players.some((p) => p.id === id && p.role === 'host');
const findPlayer = (s: RoomState, id: PlayerId): Player | undefined =>
  s.players.find((p) => p.id === id);

/** The ONLY way phase moves — every phase change bumps the epoch (3.5). */
function bumpPhase(s: RoomState, phase: RoomState['phase']): RoomState {
  return { ...s, phase, epoch: s.epoch + 1 };
}

const circleActive = (s: RoomState): boolean =>
  s.phase.k === 'DEAL' || s.phase.k === 'INPUT' || s.phase.k === 'WAITING_ON' || s.phase.k === 'REVEAL';

function mapPlayer(s: RoomState, id: PlayerId, fn: (p: Player) => Player): RoomState {
  return { ...s, players: s.players.map((p) => (p.id === id ? fn(p) : p)) };
}

const seatHoldTimer = (id: PlayerId): string => `seathold:${id}`;

// ===== lobby =====
function handleJoin(s: RoomState, e: Extract<GameEvent, { t: 'JOIN' }>): ReduceResult {
  if (s.players.some((p) => p.id === e.id)) {
    // reconnect-as-join: reseat silently
    const ns = mapPlayer(s, e.id, (p) => ({ ...p, connected: true, lastSeenAt: e.at }));
    return { state: ns, effects: [{ k: 'CANCEL', timerId: seatHoldTimer(e.id) }, { k: 'BROADCAST' }] };
  }
  const activeCount = actives(s).length;
  const joinsAsImp = s.phase.k !== 'LOBBY' || activeCount >= MAX_ACTIVE_PLAYERS;
  const name = uniqueName(e.name, s.players.map((p) => p.name));
  const player: Player = {
    id: e.id,
    name,
    avatar: e.avatar,
    role: s.players.length === 0 ? 'host' : joinsAsImp ? 'imp' : 'player',
    seat: s.players.length,
    connected: true,
    lastSeenAt: e.at,
    heatCeiling: 3,
    ceilingSet: false,
    attested18: false,
    brimstones: 2,
    score: 0,
    spotlightCount: 0,
    freshMeat: true,
  };
  return {
    state: { ...s, players: [...s.players, player] },
    effects: [{ k: 'BROADCAST' }, { k: 'SNAPSHOT' }],
  };
}

export function uniqueName(raw: string, existing: string[]): string {
  const base = raw.trim().toUpperCase().slice(0, 14) || 'SINNER';
  if (!existing.includes(base)) return base;
  for (let i = 2; ; i++) {
    const candidate = `${base} (${i})`;
    if (!existing.includes(candidate)) return candidate;
  }
}

function handleLeave(s: RoomState, e: Extract<GameEvent, { t: 'LEAVE' }>): ReduceResult {
  if (!findPlayer(s, e.id)) return noop(s);
  const ns = mapPlayer(s, e.id, (p) => ({ ...p, connected: false, lastSeenAt: e.at }));
  return {
    state: ns,
    effects: [
      { k: 'SCHEDULE', timerId: seatHoldTimer(e.id), atMs: e.at + SEAT_HOLD_MS },
      { k: 'BROADCAST' },
    ],
  };
}

function handleReconnect(s: RoomState, e: Extract<GameEvent, { t: 'RECONNECT' }>): ReduceResult {
  if (!findPlayer(s, e.id)) return noop(s);
  const ns = mapPlayer(s, e.id, (p) => ({ ...p, connected: true, lastSeenAt: e.at }));
  return { state: ns, effects: [{ k: 'CANCEL', timerId: seatHoldTimer(e.id) }, { k: 'BROADCAST' }] };
}

function handleAttest(s: RoomState, e: Extract<GameEvent, { t: 'ATTEST18' }>): ReduceResult {
  if (!findPlayer(s, e.id)) return noop(s);
  return { state: mapPlayer(s, e.id, (p) => ({ ...p, attested18: true })), effects: [{ k: 'BROADCAST' }] };
}

function handleConfig(s: RoomState, e: Extract<GameEvent, { t: 'CONFIG' }>): ReduceResult {
  if (s.phase.k !== 'LOBBY' || !isHost(s, e.id)) return noop(s);
  return { state: { ...s, config: e.cfg }, effects: [{ k: 'BROADCAST' }, { k: 'SNAPSHOT' }] };
}

function handleCeiling(s: RoomState, e: Extract<GameEvent, { t: 'CEILING' }>): ReduceResult {
  const p = findPlayer(s, e.id);
  if (!p) return noop(s);
  // Ceilings lock at BEGIN for actives; imps may still pick before conversion.
  if (s.phase.k !== 'LOBBY' && p.role !== 'imp') return noop(s);
  const ns = mapPlayer(s, e.id, (pl) => ({ ...pl, heatCeiling: e.v, ceilingSet: true }));
  // BROADCAST carries only redacted readiness — never the value (redaction, 3.4 / test D-114).
  return { state: ns, effects: [{ k: 'BROADCAST' }] };
}

// ===== BEGIN: consent completes in the lobby (private pickers), then the descent =====
// Phase CONSENT stays reserved for a future explicit consent screen; the 4.2 gate
// "host CONFIG + >=3 players + all CEILING set + entitlement" is enforced right here.
function handleBegin(s: RoomState, e: Extract<GameEvent, { t: 'BEGIN' }>, seed: string): ReduceResult {
  if (s.phase.k !== 'LOBBY' || !isHost(s, e.id)) return noop(s);
  if (!s.config || !s.entitled) return noop(s);
  const act = actives(s);
  if (act.length < 3) return noop(s);
  if (!act.every((p) => p.ceilingSet)) return noop(s);
  if (!act.every((p) => p.attested18)) return noop(s);
  const arc = buildArc(s.config, s.players, seed);
  const ns: RoomState = { ...s, arc, circleIdx: 0, devilsBargain: null, judgment: null };
  return enterCircleIntro(ns, 0, e.at, [{ k: 'AUDIO', sting: 'descend' }]);
}

// ===== circle lifecycle =====
function introDurationMs(s: RoomState, circle: number): number {
  const spec = s.arc[circle];
  const seenBefore = spec !== undefined && s.arc.slice(0, circle).some((c) => c.game === spec.game);
  const base = INTRO_TRANSITION_MS + (seenBefore ? EXPLAINER_REPEAT_MS : EXPLAINER_FIRST_MS);
  // Spotlight games host the 5s "WHO WANTS BLOOD?" claim inside the intro — never shorter than that (4.5).
  return spec !== undefined && assignsSpotlight(spec.game) ? Math.max(base, CLAIM_MS) : base;
}

function enterCircleIntro(s: RoomState, circle: number, now: number, extra: Effect[]): ReduceResult {
  // Imp conversion (4.8): in join order, at circle boundaries, up to the 12-cap.
  let nAct = actives(s).length;
  const players = [...s.players]
    .sort((a, b) => a.seat - b.seat)
    .map((p) => {
      if (p.role === 'imp' && nAct < MAX_ACTIVE_PLAYERS) {
        nAct++;
        return { ...p, role: 'player' as const };
      }
      return p;
    });
  let ns: RoomState = {
    ...s,
    players: s.players.map((p) => players.find((q) => q.id === p.id) ?? p), // keep original order
    circleIdx: circle,
    gameState: null,
    deal: null,
    spotlight: null,
    moduleTimers: {},
    lastFireAt: null,
    volunteers: [], // fresh "WHO WANTS BLOOD?" pool per circle
    // LADDER delta base: scores as this circle opens (post imp-conversion). Public data.
    circleStartScores: Object.fromEntries(s.players.map((p) => [p.id, p.score])),
  };
  // Devil's Bargain holder: current last place at this circle's start (4.3).
  const spec = ns.arc[circle];
  if (spec?.bargain && ns.devilsBargain === null) {
    const last = [...actives(ns)].sort((a, b) => a.score - b.score || a.seat - b.seat)[0];
    if (last) ns = { ...ns, devilsBargain: { holder: last.id, circle } };
  }
  ns = bumpPhase(ns, { k: 'CIRCLE_INTRO', circle });
  return {
    state: ns,
    effects: [
      ...extra,
      { k: 'SCHEDULE', timerId: `intro:${ns.epoch}`, atMs: now + introDurationMs(ns, circle) },
      { k: 'BROADCAST' },
      { k: 'SNAPSHOT' },
    ],
  };
}

function enterLadder(s: RoomState, now: number, acc: Effect[]): ReduceResult {
  const ns = bumpPhase(
    { ...s, deal: null, spotlight: null, moduleTimers: {}, lastFireAt: null },
    { k: 'LADDER', circle: s.circleIdx },
  );
  return {
    state: ns,
    effects: [
      ...acc,
      { k: 'SCHEDULE', timerId: `ladder:${ns.epoch}`, atMs: now + LADDER_MS },
      { k: 'BROADCAST' },
      { k: 'SNAPSHOT' },
    ],
  };
}

function enterJudgment(s: RoomState, acc: Effect[]): ReduceResult {
  const ns = bumpPhase({ ...s, judgment: computeJudgment(s) }, { k: 'JUDGMENT' });
  return {
    state: ns,
    effects: [...acc, { k: 'AUDIO', sting: 'judgment' }, { k: 'BROADCAST' }, { k: 'SNAPSHOT' }],
  };
}

/** "Descend again?" — same crew, fresh night. Config/ceilings/attestations survive; BEGIN re-validates. */
function resetNight(s: RoomState): ReduceResult {
  const players = s.players.map((p) => ({
    ...p,
    brimstones: 2,
    score: 0,
    spotlightCount: 0,
    freshMeat: false,
  }));
  const ns = bumpPhase(
    {
      ...s,
      players,
      arc: [],
      circleIdx: 0,
      gameState: null,
      usedCardIds: [],
      usedSkeletons: {},
      devilsBargain: null,
      telemetry: [],
      deal: null,
      spotlight: null,
      moduleTimers: {},
      lastFireAt: null,
      nightStats: {},
      judgment: null,
      circleStartScores: {},
      volunteers: [],
    },
    { k: 'LOBBY' },
  );
  return { state: ns, effects: [{ k: 'BROADCAST' }, { k: 'SNAPSHOT' }] };
}

// ===== timers =====
function handleTimer(s: RoomState, e: Extract<GameEvent, { t: 'TIMER' }>, rand: () => number): ReduceResult {
  const id = e.timerId;
  if (id.startsWith('seathold:')) return handleSeatLapse(s, id.slice('seathold:'.length), e.at, rand);
  if (id.startsWith('intro:')) {
    if (s.phase.k !== 'CIRCLE_INTRO' || id !== `intro:${s.epoch}`) return noop(s);
    // CIRCLE_INTRO -> DEAL, then hand the circle to its module.
    const ns = bumpPhase(s, { k: 'DEAL', circle: s.circleIdx });
    const acc: Effect[] = [{ k: 'BROADCAST' }];
    return routeModuleWith(ns, e.at, rand, acc, (m, ctx) => m.start(ctx));
  }
  if (id.startsWith('ladder:')) {
    if (s.phase.k !== 'LADDER' || id !== `ladder:${s.epoch}`) return noop(s);
    const next = s.circleIdx + 1;
    if (next < s.arc.length) return enterCircleIntro(s, next, e.at, []);
    return enterJudgment(s, []);
  }
  if (id.startsWith('deal:')) {
    if (s.phase.k !== 'DEAL' || !s.deal || s.deal.done || id !== s.deal.timerId) return noop(s);
    const { deal, usedCardIds } = completeDeal(s.deal);
    const sk = deal.card.skeleton;
    const ns: RoomState = {
      ...s,
      deal,
      usedCardIds: [...s.usedCardIds, ...usedCardIds],
      usedSkeletons: { ...s.usedSkeletons, [sk]: (s.usedSkeletons[sk] ?? 0) + 1 },
    };
    // Card is public (via redacted state/module view); tell the module to run its first phase.
    return routeModuleWith(ns, e.at, rand, [{ k: 'BROADCAST' }], (m, ctx) => m.timer(ctx, CORE_DEALT));
  }
  if (id.startsWith('spotlight:handoff:')) {
    const live = s.spotlight;
    if (
      s.phase.k !== 'DEAL' ||
      !live ||
      live.window !== 'primary' ||
      id !== live.handoffTimerId
    )
      return noop(s);
    const { spotlight, effects } = handoffSpotlight(live, actives(s));
    return { state: { ...s, spotlight }, effects };
  }
  if (id.startsWith('spotlight:complete:')) {
    const live = s.spotlight;
    if (
      s.phase.k !== 'DEAL' ||
      !live ||
      live.window !== 'replacement' ||
      id !== live.completionTimerId
    )
      return noop(s);
    const spotlight = completeSpotlight(live);
    const finalIds = spotlight.slots.flatMap((slot) =>
      slot.playerId === null ? [] : [slot.playerId],
    );
    const ns = bumpSpotlight({ ...s, spotlight }, finalIds);
    const result = routeModule(ns, e.at, rand, (m, ctx) => m.timer(ctx, CORE_SPOTLIGHT_DONE));
    // The completed resolution exists only for the synchronous synthetic callback.
    // A callback may immediately request a new ceremony; preserve that new state.
    if (result.state.spotlight?.ceremonyId === spotlight.ceremonyId) {
      return { ...result, state: { ...result.state, spotlight: null } };
    }
    return result;
  }
  if (id.startsWith('reveal:')) return handleRevealTimer(s, id, e.at, rand);
  // Everything else belongs to the circle's module.
  if (!circleActive(s)) return noop(s);
  const ns = removeModuleTimer(s, id);
  return routeModule(ns, e.at, rand, (m, ctx) => m.timer(ctx, id));
}

function handleRevealTimer(s: RoomState, id: string, at: number, rand: () => number): ReduceResult {
  if (s.phase.k !== 'REVEAL') return noop(s);
  if (id === `reveal:flip:${s.epoch}`) return noop(s); // phones flipped on the AT; nothing to do server-side
  if (id === `reveal:softcap:${s.epoch}`) {
    // Softcap passed: broadcast so every phone unlocks [DESCEND]. Not a phase change.
    return { state: s, effects: [{ k: 'BROADCAST' }] };
  }
  if (id === `reveal:decay:${s.epoch}`) {
    const holdSince = s.phase.holdSince;
    const quietAt = Math.max(holdSince + REVEAL_MIN_HOLD_MS, (s.lastFireAt ?? 0) + FIRE_DECAY_QUIET_MS);
    if (at >= quietAt) return endCoreReveal(s, at, rand);
    // Fires kept the room loud — push the decay check out to the new quiet horizon.
    return { state: s, effects: [{ k: 'SCHEDULE', timerId: id, atMs: quietAt }] };
  }
  return noop(s);
}

function endCoreReveal(s: RoomState, at: number, rand: () => number): ReduceResult {
  const acc: Effect[] = [
    { k: 'CANCEL', timerId: `reveal:flip:${s.epoch}` },
    { k: 'CANCEL', timerId: `reveal:softcap:${s.epoch}` },
    { k: 'CANCEL', timerId: `reveal:decay:${s.epoch}` },
  ];
  return routeModuleWith(s, at, rand, acc, (m, ctx) => m.timer(ctx, CORE_REVEAL_DONE));
}

// ===== seat lapse (4.7 + host failover) =====
function handleSeatLapse(s: RoomState, pid: PlayerId, at: number, rand: () => number): ReduceResult {
  const p = findPlayer(s, pid);
  if (!p || p.connected) return noop(s); // crawled back before the hold expired
  if (s.phase.k === 'LOBBY') {
    // Never began (or between nights): free the seat entirely.
    const players = s.players.filter((q) => q.id !== pid);
    let ns: RoomState = { ...s, players };
    if (p.role === 'host') ns = promoteHost(ns);
    return { state: ns, effects: [{ k: 'BROADCAST' }, { k: 'SNAPSHOT' }] };
  }
  // Mid-night: the seat stays (name, score, roastability) but the human is gone.
  let ns = s;
  const acc: Effect[] = [];
  if (p.role === 'host') {
    ns = promoteHost(mapPlayer(ns, pid, (q) => ({ ...q, role: 'player' })));
  }
  if (circleActive(ns)) {
    // Any blocking input they own auto-voids — the module decides relevance.
    // Convention: control(<lapsed player>, 'VOID') from a non-host = seat-lapse void.
    const r = routeModule(ns, at, rand, (m, ctx) => m.control(ctx, pid, 'VOID'));
    return { state: r.state, effects: [...acc, ...r.effects, { k: 'BROADCAST' }, { k: 'SNAPSHOT' }] };
  }
  return { state: ns, effects: [...acc, { k: 'BROADCAST' }, { k: 'SNAPSHOT' }] };
}

function promoteHost(s: RoomState): RoomState {
  if (s.players.some((p) => p.role === 'host')) return s;
  const heir = [...s.players]
    .filter((p) => p.role === 'player' && p.connected)
    .sort((a, b) => a.seat - b.seat)[0];
  if (!heir) return s; // ghost town; host seat stays empty until someone reconnects
  return mapPlayer(s, heir.id, (p) => ({ ...p, role: 'host' }));
}

// ===== burns (4.5) =====
function handleBurn(s: RoomState, e: Extract<GameEvent, { t: 'BURN' }>): ReduceResult {
  if (e.kind === 'spotlight') {
    if (s.phase.k !== 'DEAL' || !s.spotlight || s.spotlight.window === 'done') return noop(s);
    const p = findPlayer(s, e.id);
    if (!p || p.brimstones <= 0) return noop(s);
    const burned = burnSpotlight(s.spotlight, e.id, e.at);
    if (!burned.burned) return noop(s);
    const ns = mapPlayer({ ...s, spotlight: burned.spotlight }, e.id, (q) => ({
      ...q,
      brimstones: q.brimstones - 1,
    }));
    // PRIVATE acknowledgement only: no BROADCAST, schedule change, audio, snapshot,
    // or telemetry. Public timing and frames therefore cannot reveal the dodge.
    return { state: ns, effects: burned.effects };
  }
  if (s.phase.k !== 'DEAL' || !s.deal || s.deal.done) return noop(s);
  const p = findPlayer(s, e.id);
  if (!p || p.brimstones <= 0) return noop(s);
  const { deal, burned } = burnDeal(s.deal, e.id, e.at);
  if (!burned) return noop(s);
  const ns = mapPlayer({ ...s, deal }, e.id, (q) => ({ ...q, brimstones: q.brimstones - 1 }));
  // ZERO effects — burned and clean ceremonies are byte-identical on the wire, the
  // schedule never moves, and nothing lands in telemetry attributably (4.5 / D-115).
  return { state: ns, effects: [] };
}

// ===== descend =====
function handleDescend(s: RoomState, e: Extract<GameEvent, { t: 'DESCEND' }>, rand: () => number): ReduceResult {
  if (!findPlayer(s, e.id)) return noop(s);
  if (s.phase.k === 'JUDGMENT') {
    return isHost(s, e.id) ? resetNight(s) : noop(s);
  }
  if (s.phase.k === 'REVEAL') {
    const allowed = isHost(s, e.id) || e.at - s.phase.holdSince >= REVEAL_SOFTCAP_MS;
    return allowed ? endCoreReveal(s, e.at, rand) : noop(s);
  }
  if (circleActive(s)) {
    // Module-held reveal: fire its ':hold:' timer early (roast.ts pattern).
    const entry = Object.entries(s.moduleTimers).find(([tid]) => tid.includes(':hold:'));
    if (!entry) return noop(s);
    const [timerId, t] = entry;
    const allowed = isHost(s, e.id) || e.at - t.setAt >= REVEAL_SOFTCAP_MS;
    if (!allowed) return noop(s);
    const ns = removeModuleTimer(s, timerId);
    const r = routeModule(ns, e.at, rand, (m, ctx) => m.timer(ctx, timerId));
    return { state: r.state, effects: [{ k: 'CANCEL', timerId }, ...r.effects] };
  }
  return noop(s);
}

// ===== fire (telemetry + reveal decay fuel) =====
function handleFire(s: RoomState, e: Extract<GameEvent, { t: 'FIRE' }>): ReduceResult {
  if (!findPlayer(s, e.id)) return noop(s);
  const n = Math.max(1, Math.min(50, Math.floor(e.n)));
  const prev = s.nightStats[e.id] ?? { fires: 0, fifths: 0 };
  const cardId = s.deal?.done === true ? s.deal.card.id : undefined;
  const ns: RoomState = {
    ...s,
    nightStats: { ...s.nightStats, [e.id]: { ...prev, fires: prev.fires + n } },
    telemetry: [...s.telemetry, { t: 'fires', ...(cardId !== undefined ? { cardId } : {}), fires: n, at: e.at }],
    lastFireAt: s.phase.k === 'REVEAL' ? e.at : s.lastFireAt,
  };
  return { state: ns, effects: [] }; // HEAT coalescing (<=4Hz) is the DO's job
}

/**
 * "WHO WANTS BLOOD?" (4.5) — during a spotlight game's CIRCLE_INTRO, an active player may
 * volunteer for this circle's spotlight. The claim only registers here; the actual pick
 * happens at m.start via pickSpotlightPreferring. Imps/unknown ids and non-spotlight or
 * out-of-phase claims are silently ignored (the valve can never force or skip anyone).
 */
function handleClaim(s: RoomState, e: Extract<GameEvent, { t: 'CLAIM' }>): ReduceResult {
  if (s.phase.k !== 'CIRCLE_INTRO') return noop(s);
  const spec = s.arc[s.circleIdx];
  if (!spec || !assignsSpotlight(spec.game)) return noop(s);
  const p = findPlayer(s, e.id);
  if (!p || p.role === 'imp' || s.volunteers.includes(e.id)) return noop(s);
  const ns: RoomState = { ...s, volunteers: [...s.volunteers, e.id] };
  return { state: ns, effects: [{ k: 'BROADCAST' }] };
}

// ===== module routing =====
function makeCtx(s: RoomState, now: number, rand: () => number): GameCtx | null {
  const spec = s.arc[s.circleIdx];
  if (!spec) return null;
  const completed = s.spotlight?.window === 'done' ? spotlightResolution(s.spotlight) : undefined;
  return {
    state: s,
    circle: spec,
    circleIdx: s.circleIdx,
    players: actives(s),
    imps: impsOf(s),
    now,
    rand,
    finaleMult: spec.finale ? 3 : 1,
    volunteers: actives(s).filter((p) => s.volunteers.includes(p.id)),
    ...(completed !== undefined ? { spotlight: completed } : {}),
  };
}

function routeModule(
  s: RoomState,
  now: number,
  rand: () => number,
  fn: (m: GameModule, ctx: GameCtx) => GameStep,
): ReduceResult {
  return routeModuleWith(s, now, rand, [], fn);
}

function routeModuleWith(
  s: RoomState,
  now: number,
  rand: () => number,
  acc: Effect[],
  fn: (m: GameModule, ctx: GameCtx) => GameStep,
): ReduceResult {
  if (!circleActive(s)) return { state: s, effects: acc };
  const spec = s.arc[s.circleIdx];
  const m = spec ? registry.get(spec.game) : undefined;
  if (!m) {
    // No module registered yet (M1 ships roast only): skip the circle rather than
    // deadlock the night. Registration of D-121..D-133 modules retires this path.
    return enterLadder(s, now, acc);
  }
  const ctx = makeCtx(s, now, rand);
  if (!ctx) return { state: s, effects: acc };
  const step = fn(m, ctx);
  return applyStep(s, step, now, rand, acc);
}

function routeControl(
  s: RoomState,
  playerId: PlayerId,
  kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID',
  at: number,
  rand: () => number,
): ReduceResult {
  if (!circleActive(s) || !findPlayer(s, playerId)) return noop(s);
  const r = routeModule(s, at, rand, (m, ctx) => m.control(ctx, playerId, kind));
  // A FIFTH that actually landed announces itself (AUDIO sting) — count it for Judgment.
  if (kind === 'FIFTH' && r.effects.some((ef) => ef.k === 'AUDIO' && ef.sting === 'fifth')) {
    const prev = r.state.nightStats[playerId] ?? { fires: 0, fifths: 0 };
    return {
      state: { ...r.state, nightStats: { ...r.state.nightStats, [playerId]: { ...prev, fifths: prev.fifths + 1 } } },
      effects: r.effects,
    };
  }
  return r;
}

/**
 * Apply a GameStep: strip + honor directives, track module timers, award scores
 * (finale x3 / Bargain x2 applied here — modules return pre-multiplier), advance.
 */
/** D-134: bump Player.spotlightCount for players who just took the spotlight. Ids not
 * among the active players (or imps, who are never spotlit) are simply ignored. */
function bumpSpotlight(s: RoomState, ids: readonly PlayerId[]): RoomState {
  const set = new Set(ids);
  return {
    ...s,
    players: s.players.map((p) => (set.has(p.id) ? { ...p, spotlightCount: p.spotlightCount + 1 } : p)),
  };
}

function applyStep(s: RoomState, step: GameStep, now: number, rand: () => number, acc: Effect[]): ReduceResult {
  const effects: Effect[] = [...acc, ...step.effects];
  // Track module-owned schedules so DESCEND can fire holds early (roast.ts pattern).
  let moduleTimers = s.moduleTimers;
  for (const ef of step.effects) {
    if (ef.k === 'SCHEDULE' && !isCoreTimer(ef.timerId)) {
      moduleTimers = { ...moduleTimers, [ef.timerId]: { atMs: ef.atMs, setAt: now } };
    } else if (ef.k === 'CANCEL' && !isCoreTimer(ef.timerId) && ef.timerId in moduleTimers) {
      const { [ef.timerId]: _gone, ...rest } = moduleTimers;
      moduleTimers = rest;
    }
  }
  // Reserved directive keys ($phase/$deal/$spotlight) are consumed here, never persisted.
  let gameState = step.gameState;
  let dirs: ModuleDirectives = {};
  if (gameState !== null && typeof gameState === 'object' && !Array.isArray(gameState)) {
    const g = gameState as Record<string, unknown> & ModuleDirectives;
    if (g.$phase !== undefined || g.$deal !== undefined || g.$spotlight !== undefined) {
      const { $phase, $deal, $spotlight, ...rest } = g;
      dirs = {
        ...($phase !== undefined ? { $phase } : {}),
        ...($deal !== undefined ? { $deal } : {}),
        ...($spotlight !== undefined ? { $spotlight } : {}),
      };
      gameState = rest;
    }
  }
  let ns: RoomState = { ...s, gameState, moduleTimers };
  // Burnable assignments are counted at CORE_SPOTLIGHT_DONE, after replacements
  // settle. This explicit channel remains for elected/non-burnable spotlights.
  const spotlit = step.spotlight ?? [];
  if (spotlit.length > 0) ns = bumpSpotlight(ns, spotlit);
  if (step.scores) ns = awardScores(ns, step.scores, ns.circleIdx);
  if (step.done === true) return enterLadder(ns, now, effects);
  if (dirs.$spotlight) return startSpotlight(ns, dirs.$spotlight, now, rand, effects);
  if (dirs.$deal) return startDeal(ns, dirs.$deal, now, effects);
  if (dirs.$phase) return applyPhaseDirective(ns, dirs.$phase, now, effects);
  return { state: ns, effects };
}

function startSpotlight(
  s: RoomState,
  req: SpotlightRequest,
  now: number,
  rand: () => number,
  acc: Effect[],
): ReduceResult {
  let ns = s;
  const effects = [...acc];
  if (ns.phase.k !== 'DEAL') {
    ns = bumpPhase(ns, { k: 'DEAL', circle: ns.circleIdx });
    effects.push({ k: 'BROADCAST' });
  }
  // Epoch is stable throughout both windows. Include the event time because a
  // completion callback may request the next loop's ceremony while Phase remains
  // DEAL (and therefore without an epoch bump). Role/player ids never enter ids.
  const ceremonyKey = `${ns.epoch}:${now}`;
  const begun = beginSpotlight(
    req,
    actives(ns),
    actives(ns).filter((p) => ns.volunteers.includes(p.id)),
    rand,
    ceremonyKey,
    now,
  );
  return {
    state: { ...ns, deal: null, spotlight: begun.spotlight },
    effects: [...effects, ...begun.effects, { k: 'BROADCAST' }],
  };
}

function startDeal(s: RoomState, req: DealRequest, now: number, acc: Effect[]): ReduceResult {
  let ns = s;
  const effects = [...acc];
  if (ns.phase.k !== 'DEAL') {
    ns = bumpPhase(ns, { k: 'DEAL', circle: ns.circleIdx });
    effects.push({ k: 'BROADCAST' });
  }
  const { deal, effects: dealFx } = beginDeal(req, `deal:${ns.epoch}`, now);
  return { state: { ...ns, deal, spotlight: null }, effects: [...effects, ...dealFx, { k: 'BROADCAST' }] };
}

function applyPhaseDirective(s: RoomState, d: PhaseDirective, now: number, acc: Effect[]): ReduceResult {
  const circle = s.circleIdx;
  if (d.k === 'INPUT') {
    const ns = bumpPhase({ ...s, spotlight: null }, { k: 'INPUT', circle, sub: d.sub, deadline: d.deadline });
    return { state: ns, effects: [...acc, { k: 'BROADCAST' }] };
  }
  if (d.k === 'WAITING_ON') {
    const ns = bumpPhase({ ...s, spotlight: null }, { k: 'WAITING_ON', circle, who: d.who, since: d.since });
    return { state: ns, effects: [...acc, { k: 'BROADCAST' }] };
  }
  // REVEAL: core-owned hold — softcap + fire-decay armed, DESCEND handled in core.
  // The flip beat is a pure AT broadcast (3.3): phones run the 3-2-1 against it and
  // flip simultaneously; the timer itself is consumed as a no-op when it comes back.
  const ns = bumpPhase({ ...s, spotlight: null, lastFireAt: null }, { k: 'REVEAL', circle, holdSince: now });
  return {
    state: ns,
    effects: [
      ...acc,
      { k: 'SCHEDULE', timerId: `reveal:flip:${ns.epoch}`, atMs: now + REVEAL_FLIP_MS },
      { k: 'SCHEDULE', timerId: `reveal:softcap:${ns.epoch}`, atMs: now + REVEAL_SOFTCAP_MS },
      { k: 'SCHEDULE', timerId: `reveal:decay:${ns.epoch}`, atMs: now + REVEAL_MIN_HOLD_MS + FIRE_DECAY_QUIET_MS },
      { k: 'BROADCAST' },
    ],
  };
}

function removeModuleTimer(s: RoomState, timerId: string): RoomState {
  if (!(timerId in s.moduleTimers)) return s;
  const { [timerId]: _gone, ...rest } = s.moduleTimers;
  return { ...s, moduleTimers: rest };
}

// ===== judgment (winner + superlatives from real night telemetry) =====
export function computeJudgment(s: RoomState): JudgmentSummary {
  const act = actives(s);
  const top = Math.max(0, ...act.map((p) => p.score));
  const winners = act
    .filter((p) => p.score === top)
    .sort((a, b) => a.seat - b.seat)
    .map((p) => p.id);
  const superlatives: Superlative[] = [];
  const stat = (id: PlayerId): { fires: number; fifths: number } =>
    s.nightStats[id] ?? { fires: 0, fifths: 0 };
  const bySeat = (a: Player, b: Player): number => a.seat - b.seat;

  const arsonist = [...act].sort((a, b) => stat(b.id).fires - stat(a.id).fires || bySeat(a, b))[0];
  if (arsonist && stat(arsonist.id).fires > 0)
    superlatives.push({ title: 'THE ARSONIST', playerId: arsonist.id });

  const wanted = [...act].sort((a, b) => b.spotlightCount - a.spotlightCount || bySeat(a, b))[0];
  if (wanted && wanted.spotlightCount > 0)
    superlatives.push({ title: 'MOST WANTED', playerId: wanted.id });

  const coward = [...act].sort((a, b) => stat(b.id).fifths - stat(a.id).fifths || bySeat(a, b))[0];
  if (coward && stat(coward.id).fifths > 0)
    superlatives.push({ title: 'COWARDICE NOTED', playerId: coward.id });

  const stoneCold = [...act]
    .filter((p) => p.brimstones === 2)
    .sort((a, b) => b.score - a.score || bySeat(a, b))[0];
  if (stoneCold) superlatives.push({ title: 'STONE COLD', playerId: stoneCold.id });

  // Residual imps: guaranteed superlative (4.8).
  for (const imp of impsOf(s).sort(bySeat)) {
    superlatives.push({ title: "STILL IN HELL'S WAITING ROOM", playerId: imp.id });
  }
  return { winners, superlatives, bargain: s.devilsBargain };
}

// ============================================================================
// Blocking-input terminal machine (4.7) — a reusable helper modules invoke.
// The module owns WHAT the input is; this helper owns the shame choreography:
// paused deadline -> 12s grace -> WAITING_ON -> 30s pit vote (>=60% = void) ->
// host VOID / owner FIFTH / seat-lapse auto-void. NEVER fabricates a truth value:
// the only way `resolved` becomes 'input' is the module calling blockingResolveInput
// with a real human answer in hand (test D-115).
// ============================================================================
export const BLOCKING_GRACE_MS = 12_000;
export const BLOCKING_PIT_MS = 30_000;
export const PIT_THRESHOLD = 0.6;

export interface BlockingJuror {
  id: PlayerId;
  weight: number; // imps 0.5 (4.8)
}

export interface BlockingState {
  owner: PlayerId;
  sub: string; // module sub-phase label, echoed into the INPUT directive
  key: string; // caller-supplied uniqueness (use the epoch)
  startedAt: number;
  pitOpen: boolean;
  pitVotes: Record<PlayerId, 'drag' | 'pit'>;
  resolved: 'input' | 'fifth' | 'voided' | null;
  voidReason: 'pit' | 'host' | 'lapse' | null;
}

export const blockingGraceTimer = (key: string): string => `blk:grace:${key}`;
export const blockingPitTimer = (key: string): string => `blk:pit:${key}`;

export interface BlockingStepOut {
  blocking: BlockingState;
  $phase?: PhaseDirective;
  effects: Effect[];
}

/** Owner faces a blocking input: timer pauses (deadline null), shame machinery arms. */
export function blockingBegin(owner: PlayerId, sub: string, key: string, now: number): BlockingStepOut {
  return {
    blocking: {
      owner,
      sub,
      key,
      startedAt: now,
      pitOpen: false,
      pitVotes: {},
      resolved: null,
      voidReason: null,
    },
    $phase: { k: 'INPUT', sub, deadline: null },
    effects: [
      { k: 'SCHEDULE', timerId: blockingGraceTimer(key), atMs: now + BLOCKING_GRACE_MS },
      { k: 'SCHEDULE', timerId: blockingPitTimer(key), atMs: now + BLOCKING_PIT_MS },
    ],
  };
}

/** Route grace/pit timer fires here. Grace -> public WAITING_ON; pit -> the room gets the buttons. */
export function blockingTimerFired(b: BlockingState, timerId: string, now: number): BlockingStepOut {
  if (b.resolved !== null) return { blocking: b, effects: [] };
  if (timerId === blockingGraceTimer(b.key)) {
    return {
      blocking: b,
      $phase: { k: 'WAITING_ON', who: b.owner, since: now },
      effects: [{ k: 'BROADCAST' }],
    };
  }
  if (timerId === blockingPitTimer(b.key)) {
    return { blocking: { ...b, pitOpen: true }, effects: [{ k: 'BROADCAST' }] };
  }
  return { blocking: b, effects: [] };
}

/** DRAG THEM BACK / FEED THEM TO THE PIT. >=60% pit (weighted) voids the loop. */
export function blockingPitVote(
  b: BlockingState,
  voter: PlayerId,
  choice: 'drag' | 'pit',
  jurors: readonly BlockingJuror[],
): BlockingStepOut {
  if (b.resolved !== null || !b.pitOpen || voter === b.owner) return { blocking: b, effects: [] };
  if (!jurors.some((j) => j.id === voter)) return { blocking: b, effects: [] };
  const pitVotes = { ...b.pitVotes, [voter]: choice };
  const total = jurors.reduce((sum, j) => sum + j.weight, 0);
  const pitWeight = jurors.reduce((sum, j) => sum + (pitVotes[j.id] === 'pit' ? j.weight : 0), 0);
  if (total > 0 && pitWeight >= PIT_THRESHOLD * total) {
    return {
      blocking: { ...b, pitVotes, resolved: 'voided', voidReason: 'pit' },
      effects: [...cancelBlocking(b), { k: 'AUDIO', sting: 'void' }],
    };
  }
  return { blocking: { ...b, pitVotes }, effects: [{ k: 'BROADCAST' }] };
}

/**
 * Terminal controls. FIFTH: owner only ("THE WITNESS TAKES THE FIFTH"). VOID from
 * the host kills the loop; VOID carrying the OWNER's id is the seat-lapse convention
 * the core uses when a disconnected owner's 90s hold expires ("THE WITNESS FLED").
 */
export function blockingControl(
  b: BlockingState,
  playerId: PlayerId,
  kind: 'FIFTH' | 'VOID',
  hostId: PlayerId | null,
): BlockingStepOut {
  if (b.resolved !== null) return { blocking: b, effects: [] };
  if (kind === 'FIFTH') {
    if (playerId !== b.owner) return { blocking: b, effects: [] };
    return {
      blocking: { ...b, resolved: 'fifth' },
      effects: [...cancelBlocking(b), { k: 'AUDIO', sting: 'fifth' }],
    };
  }
  if (playerId === hostId) {
    return {
      blocking: { ...b, resolved: 'voided', voidReason: 'host' },
      effects: [...cancelBlocking(b), { k: 'AUDIO', sting: 'void' }],
    };
  }
  if (playerId === b.owner) {
    return {
      blocking: { ...b, resolved: 'voided', voidReason: 'lapse' },
      effects: [...cancelBlocking(b), { k: 'AUDIO', sting: 'fled' }],
    };
  }
  return { blocking: b, effects: [] };
}

/** The owner delivered a REAL answer — the only path to 'input' (never fabricated). */
export function blockingResolveInput(b: BlockingState): BlockingStepOut {
  if (b.resolved !== null) return { blocking: b, effects: [] };
  return { blocking: { ...b, resolved: 'input' }, effects: cancelBlocking(b) };
}

function cancelBlocking(b: BlockingState): Effect[] {
  return [
    { k: 'CANCEL', timerId: blockingGraceTimer(b.key) },
    { k: 'CANCEL', timerId: blockingPitTimer(b.key) },
  ];
}
