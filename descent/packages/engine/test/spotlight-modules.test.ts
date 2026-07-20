// Module-side contract for the core-owned, burnable spotlight ceremony.
import { describe, expect, it } from 'vitest';
import type {
  CircleSpec,
  ModuleDirectives,
  Player,
  RoomState,
  SpotlightResolution,
} from '../src/types.js';
import type { GameCtx, GameModule, GameStep } from '../src/games/module.js';
import { CORE_SPOTLIGHT_DONE } from '../src/games/module.js';
// Import the registry root before individual modules; overunder/confession share
// blocking helpers with engine.ts and otherwise form an ESM initialization cycle.
import { registerModule } from '../src/engine.js';
import { overunderModule } from '../src/games/overunder.js';
import { confessionModule } from '../src/games/confession.js';
import { redflagModule } from '../src/games/redflag.js';
import { alibiModule } from '../src/games/alibi.js';
import { poisonModule } from '../src/games/poison.js';
import { titlefightModule } from '../src/games/titlefight.js';
import { rng } from '../src/rng.js';
import { mkNightRoom, mkPlayers, mkSpec } from './helpers.js';

const modules = [
  overunderModule,
  confessionModule,
  redflagModule,
  alibiModule,
  poisonModule,
  titlefightModule,
] as const;
for (const module of modules) registerModule(module);

function ctxFor(
  game: CircleSpec['game'],
  over: { loops?: number; players?: Player[]; gameState?: unknown; spotlight?: SpotlightResolution } = {},
): GameCtx {
  const players = over.players ?? mkPlayers(4);
  const circle = mkSpec(game, { loops: over.loops ?? 2, rung: 5 });
  const state: RoomState = mkNightRoom(circle, players, {
    phase: { k: 'DEAL', circle: 0 },
    gameState: over.gameState ?? null,
  });
  return {
    state,
    circle,
    circleIdx: 0,
    players: players.filter((p) => p.role !== 'imp'),
    imps: players.filter((p) => p.role === 'imp'),
    now: 1_000_000,
    rand: rng(`spotlight-module:${game}`),
    finaleMult: 1,
    volunteers: [],
    ...(over.spotlight ? { spotlight: over.spotlight } : {}),
  };
}

function directive(step: GameStep): { pending: unknown; dirs: ModuleDirectives } {
  const raw = step.gameState as Record<string, unknown> & ModuleDirectives;
  const { $phase, $deal, $spotlight, ...pending } = raw;
  return { pending, dirs: { $phase, $deal, $spotlight } };
}

function finishAssignment(
  module: GameModule,
  ctx: GameCtx,
  resolution: SpotlightResolution,
): GameStep {
  const started = module.start(ctx);
  const { pending } = directive(started);
  const callback = ctxFor(ctx.circle.game, {
    loops: ctx.circle.loops,
    players: ctx.players,
    gameState: pending,
    spotlight: resolution,
  });
  return module.timer(callback, CORE_SPOTLIGHT_DONE);
}

describe('spotlight module assignment boundary', () => {
  it.each(modules)('$deck starts with a private ASSIGN step whose game view is always null', (module) => {
    const ctx = ctxFor(module.deck as CircleSpec['game']);
    const started = module.start(ctx);
    const { pending, dirs } = directive(started);
    expect(pending).toMatchObject({ sub: 'ASSIGN', loop: 0, loops: 2 });
    expect(dirs.$spotlight?.eligibleIds).toEqual(ctx.players.map((p) => p.id));
    expect(dirs.$deal).toBeUndefined();
    expect(dirs.$phase).toBeUndefined();
    const waiting = ctxFor(ctx.circle.game, { players: ctx.players, gameState: pending });
    for (const player of ctx.players) expect(module.view(waiting, player.id)).toBeNull();
  });

  it('recomputes subject-specific content for the final replacement', () => {
    const players = mkPlayers(4).map((p) => (p.id === 'P3' ? { ...p, heatCeiling: 2 as const } : p));

    const over = finishAssignment(overunderModule, ctxFor('overunder', { players }), {
      assignments: [{ role: 'subject', playerId: 'P3' }],
    });
    const overDeal = directive(over).dirs.$deal;
    expect(overDeal?.subjectId).toBe('P3');
    expect(overDeal?.primary.exposure).toBeLessThanOrEqual(2);

    const confession = finishAssignment(confessionModule, ctxFor('confession', { players }), {
      assignments: [{ role: 'confessor', playerId: 'P3' }],
    });
    const confessionState = confession.gameState as { confessorId: string; hand: { exposure: number }[] };
    expect(confessionState.confessorId).toBe('P3');
    expect(confessionState.hand.every((card) => card.exposure <= 2)).toBe(true);

    const alibi = finishAssignment(alibiModule, ctxFor('alibi', { players }), {
      assignments: [{ role: 'accused', playerId: 'P3' }],
    });
    const alibiDeal = directive(alibi).dirs.$deal;
    expect(alibiDeal?.subjectId).toBe('P3');
    expect(alibiDeal?.primary.exposure).toBeLessThanOrEqual(2);
  });

  it('Red Flag and Poison SKIPEM re-deal the card without reassigning performers', () => {
    const redCtx = ctxFor('redflag');
    const redDeal = finishAssignment(redflagModule, redCtx, {
      assignments: [{ role: 'defender', playerId: 'P1' }],
    });
    const red = directive(redDeal);
    const redLive = {
      ...(red.pending as Record<string, unknown>),
      sub: 'DEFENSE',
      card: red.dirs.$deal?.primary,
      defenseDeadline: redCtx.now + 1_000,
    };
    const redSkipped = redflagModule.control(ctxFor('redflag', { gameState: redLive }), 'P2', 'SKIPEM');
    const redRaw = redSkipped.gameState as Record<string, unknown> & ModuleDirectives;
    expect(redRaw).toMatchObject({ sub: 'DEAL', defenderId: 'P1', defendersUsed: ['P1'], skipUsed: true });
    expect(redRaw.$deal?.subjectId).toBe('P1');
    expect(redRaw.$spotlight).toBeUndefined();

    const poisonCtx = ctxFor('poison');
    const poisonDeal = finishAssignment(poisonModule, poisonCtx, {
      assignments: [
        { role: 'pitcher-a', playerId: 'P0' },
        { role: 'pitcher-b', playerId: 'P1' },
      ],
    });
    const poison = directive(poisonDeal);
    const poisonLive = {
      ...(poison.pending as Record<string, unknown>),
      sub: 'PITCH',
      card: poison.dirs.$deal?.primary,
      pitchDeadline: poisonCtx.now + 1_000,
    };
    const poisonSkipped = poisonModule.control(ctxFor('poison', { gameState: poisonLive }), 'P0', 'SKIPEM');
    const poisonRaw = poisonSkipped.gameState as Record<string, unknown> & ModuleDirectives;
    expect(poisonRaw).toMatchObject({
      sub: 'DEAL',
      pitcherA: 'P0',
      pitcherB: 'P1',
      pitchersUsed: ['P0', 'P1'],
      skipUsed: true,
    });
    expect(poisonRaw.$spotlight).toBeUndefined();
  });

  it('Poison and Title Fight request semantic A/B roles and commit two distinct final performers', () => {
    const poisonCtx = ctxFor('poison');
    expect(directive(poisonModule.start(poisonCtx)).dirs.$spotlight?.roles).toEqual(['pitcher-a', 'pitcher-b']);
    const poison = finishAssignment(poisonModule, poisonCtx, {
      assignments: [
        { role: 'pitcher-a', playerId: 'P0' },
        { role: 'pitcher-b', playerId: 'P1' },
      ],
    }).gameState as Record<string, unknown>;
    expect(poison).toMatchObject({
      sub: 'DEAL',
      pitcherA: 'P0',
      pitcherB: 'P1',
      pitchersUsed: ['P0', 'P1'],
    });

    const titleCtx = ctxFor('titlefight');
    expect(directive(titlefightModule.start(titleCtx)).dirs.$spotlight?.roles).toEqual(['fighter-a', 'fighter-b']);
    const title = finishAssignment(titlefightModule, titleCtx, {
      assignments: [
        { role: 'fighter-a', playerId: 'P2' },
        { role: 'fighter-b', playerId: 'P3' },
      ],
    }).gameState as Record<string, unknown>;
    expect(title).toMatchObject({
      sub: 'DEAL',
      fighterA: 'P2',
      fighterB: 'P3',
      fightersUsed: ['P2', 'P3'],
    });
  });

  it.each([
    {
      module: poisonModule,
      game: 'poison' as const,
      assignments: [
        { role: 'pitcher-a' as const, playerId: 'P0' },
        { role: 'pitcher-b' as const, playerId: null },
      ],
      usedKey: 'pitchersUsed',
    },
    {
      module: titlefightModule,
      game: 'titlefight' as const,
      assignments: [
        { role: 'fighter-a' as const, playerId: 'P0' },
        { role: 'fighter-b' as const, playerId: null },
      ],
      usedKey: 'fightersUsed',
    },
  ])('$game skips an incomplete duel without fabricating a second performer', ({ module, game, assignments, usedKey }) => {
    const ctx = ctxFor(game);
    const started = module.start(ctx);
    const { pending } = directive(started);
    const callback = ctxFor(game, {
      players: ctx.players,
      gameState: pending,
      spotlight: { assignments },
    });
    const next = module.timer(callback, CORE_SPOTLIGHT_DONE);
    const raw = next.gameState as Record<string, unknown> & ModuleDirectives;
    expect(raw).toMatchObject({ sub: 'ASSIGN', loop: 1, [usedKey]: ['P0'] });
    expect(raw.$spotlight?.eligibleIds).not.toContain('P0');
    expect(raw.$deal).toBeUndefined();
    expect(next.done).not.toBe(true);
  });
});
