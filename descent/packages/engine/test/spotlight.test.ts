// D-134 private spotlight ceremony: two fixed 10s windows, volunteer-first
// assignment, silent dodge/replacement, reconnect replay, and null exhaustion.
import { describe, expect, it } from 'vitest';
import {
  SPOTLIGHT_TOTAL_MS,
  SPOTLIGHT_WINDOW_MS,
  beginSpotlight,
  burnSpotlight,
  completeSpotlight,
  handoffSpotlight,
  orderSpotlightCandidates,
  spotlightPrivateFor,
  spotlightResolution,
} from '../src/spotlight.js';
import { rng } from '../src/rng.js';
import { mkPlayer } from './helpers.js';

const NOW = 1_000_000;
const players = [
  mkPlayer('P0', 'host', 0, { spotlightCount: 4 }),
  mkPlayer('P1', 'player', 1, { spotlightCount: 0 }),
  mkPlayer('P2', 'player', 2, { spotlightCount: 2 }),
  mkPlayer('P3', 'player', 3, { spotlightCount: 1 }),
];

describe('spotlight candidate order', () => {
  it('puts all eligible volunteers before non-volunteers and remains deterministic', () => {
    const one = orderSpotlightCandidates(players, [players[2]!, players[3]!], rng('blood'));
    const two = orderSpotlightCandidates(players, [players[2]!, players[3]!], rng('blood'));
    expect(new Set(one.slice(0, 2).map((p) => p.id))).toEqual(new Set(['P2', 'P3']));
    expect(one.map((p) => p.id)).toEqual(two.map((p) => p.id));
  });
});

describe('fixed two-window ceremony', () => {
  it('schedules T+10 and T+20 up front and sends only private primary assignments', () => {
    const { spotlight, effects } = beginSpotlight(
      { roles: ['pitcher-a', 'pitcher-b'], eligibleIds: players.map((p) => p.id) },
      players,
      [players[2]!],
      rng('two'),
      '7:1000',
      NOW,
    );
    expect(spotlight.handoffAt).toBe(NOW + SPOTLIGHT_WINDOW_MS);
    expect(spotlight.completesAt).toBe(NOW + SPOTLIGHT_TOTAL_MS);
    expect(effects.filter((e) => e.k === 'SCHEDULE')).toEqual([
      { k: 'SCHEDULE', timerId: 'spotlight:handoff:7:1000', atMs: NOW + SPOTLIGHT_WINDOW_MS },
      { k: 'SCHEDULE', timerId: 'spotlight:complete:7:1000', atMs: NOW + SPOTLIGHT_TOTAL_MS },
    ]);
    const sends = effects.filter((e) => e.k === 'SEND');
    expect(sends).toHaveLength(2);
    expect(new Set(spotlight.slots.map((slot) => slot.playerId)).size).toBe(2);
    expect(JSON.stringify(effects)).not.toContain('BROADCAST');
  });

  it('holds a burned primary vacant until fixed T+10, then gives replacement a full 10s', () => {
    const begun = beginSpotlight(
      { roles: ['subject'], eligibleIds: players.map((p) => p.id) },
      players,
      [],
      rng('single'),
      '8:2000',
      NOW,
    );
    const primary = begun.spotlight.slots[0]!.playerId!;
    const burned = burnSpotlight(begun.spotlight, primary, NOW + 9_500);
    expect(burned.burned).toBe(true);
    expect(burned.spotlight.slots[0]!.playerId).toBeNull();
    expect(burned.effects).toEqual([
      {
        k: 'SEND',
        to: primary,
        kind: 'spotlight',
        payload: { status: 'released', ceremonyId: 'spotlight:8:2000' },
      },
    ]);
    expect(burned.spotlight.handoffAt).toBe(begun.spotlight.handoffAt);
    expect(burned.spotlight.completesAt).toBe(begun.spotlight.completesAt);

    const handoff = handoffSpotlight(burned.spotlight, players);
    const replacement = handoff.spotlight.slots[0]!.playerId;
    expect(replacement).not.toBeNull();
    expect(replacement).not.toBe(primary);
    expect(handoff.effects).toEqual([
      expect.objectContaining({
        k: 'SEND',
        to: replacement,
        kind: 'spotlight',
        payload: expect.objectContaining({
          status: 'assigned',
          burnDeadline: NOW + SPOTLIGHT_TOTAL_MS,
          announceAt: NOW + SPOTLIGHT_TOTAL_MS,
        }),
      }),
    ]);
  });

  it('notifies a zero-Brimstone replacement with canBurn:false', () => {
    const cast = players.map((p) =>
      p.id === 'P3' ? { ...p, brimstones: 0 } : p,
    );
    // One primary + one reserve, forcing P3 to be the only replacement.
    const begun = beginSpotlight(
      { roles: ['defender'], eligibleIds: ['P0', 'P3'] },
      cast,
      [cast[0]!],
      rng('no-token'),
      '9:3000',
      NOW,
    );
    expect(begun.spotlight.slots[0]!.playerId).toBe('P0');
    const burned = burnSpotlight(begun.spotlight, 'P0', NOW + 1);
    const handoff = handoffSpotlight(burned.spotlight, cast);
    expect(handoff.spotlight.slots[0]).toMatchObject({ playerId: 'P3', burnable: false });
    expect(handoff.effects).toEqual([
      expect.objectContaining({
        k: 'SEND',
        to: 'P3',
        payload: expect.objectContaining({ status: 'assigned', canBurn: false }),
      }),
    ]);
  });

  it('never initially assigns offline players and drops offline, never-notified reserves', () => {
    const cast = players.map((p) =>
      p.id === 'P1' ? { ...p, connected: false } : p,
    );
    const onlyOffline = beginSpotlight(
      { roles: ['subject'], eligibleIds: ['P1'] },
      cast,
      [],
      rng('offline'),
      '10:4000',
      NOW,
    );
    expect(onlyOffline.spotlight.slots[0]!.playerId).toBeNull();
    expect(onlyOffline.effects.filter((e) => e.k === 'SEND')).toEqual([]);

    const begun = beginSpotlight(
      { roles: ['subject'], eligibleIds: ['P0', 'P2'] },
      cast,
      [cast[0]!],
      rng('reserve-drop'),
      '10:4001',
      NOW,
    );
    const burned = burnSpotlight(begun.spotlight, 'P0', NOW + 1);
    const dropped = cast.map((p) => (p.id === 'P2' ? { ...p, connected: false } : p));
    const handoff = handoffSpotlight(burned.spotlight, dropped);
    expect(handoff.spotlight.slots[0]!.playerId).toBeNull();
    expect(handoff.effects).toEqual([]);
  });

  it('allows a replacement to dodge without opening a third/timing-revealing window', () => {
    const begun = beginSpotlight(
      { roles: ['accused'], eligibleIds: players.map((p) => p.id) },
      players,
      [],
      rng('double-dodge'),
      '11:5000',
      NOW,
    );
    const primary = begun.spotlight.slots[0]!.playerId!;
    const first = burnSpotlight(begun.spotlight, primary, NOW + 1);
    const handoff = handoffSpotlight(first.spotlight, players);
    const replacement = handoff.spotlight.slots[0]!.playerId!;
    const second = burnSpotlight(handoff.spotlight, replacement, NOW + SPOTLIGHT_WINDOW_MS + 1);
    expect(second.burned).toBe(true);
    const complete = completeSpotlight(second.spotlight);
    expect(complete.completesAt).toBe(NOW + SPOTLIGHT_TOTAL_MS);
    expect(spotlightResolution(complete)).toEqual({
      assignments: [{ role: 'accused', playerId: null }],
    });
  });

  it('two-performer exhaustion produces null slots instead of duplicates or coercion', () => {
    const three = players.slice(0, 3);
    const begun = beginSpotlight(
      { roles: ['fighter-a', 'fighter-b'], eligibleIds: three.map((p) => p.id) },
      three,
      [],
      rng('n3'),
      '12:6000',
      NOW,
    );
    const a = begun.spotlight.slots[0]!.playerId!;
    const b = begun.spotlight.slots[1]!.playerId!;
    const one = burnSpotlight(begun.spotlight, a, NOW + 1);
    const two = burnSpotlight(one.spotlight, b, NOW + 2);
    const handoff = handoffSpotlight(two.spotlight, three);
    const ids = handoff.spotlight.slots.map((slot) => slot.playerId);
    expect(ids.filter((id) => id !== null)).toHaveLength(1);
    expect(new Set(ids.filter((id) => id !== null)).size).toBe(1);
    expect(ids).toContain(null);
  });

  it('rejects wrong, duplicate, and late burns without changing state or effects', () => {
    const begun = beginSpotlight(
      { roles: ['confessor'], eligibleIds: players.map((p) => p.id) },
      players,
      [],
      rng('reject'),
      '13:7000',
      NOW,
    );
    const primary = begun.spotlight.slots[0]!.playerId!;
    const wrong = burnSpotlight(begun.spotlight, 'UNKNOWN', NOW + 1);
    expect(wrong).toEqual({ spotlight: begun.spotlight, effects: [], burned: false });
    const once = burnSpotlight(begun.spotlight, primary, NOW + 1);
    const twice = burnSpotlight(once.spotlight, primary, NOW + 2);
    expect(twice).toEqual({ spotlight: once.spotlight, effects: [], burned: false });
    const late = burnSpotlight(begun.spotlight, primary, NOW + SPOTLIGHT_WINDOW_MS + 1);
    expect(late).toEqual({ spotlight: begun.spotlight, effects: [], burned: false });
  });
});

describe('spotlightPrivateFor reconnect replay', () => {
  it('replays only the current assignee and respects both fixed deadlines', () => {
    const begun = beginSpotlight(
      { roles: ['subject'], eligibleIds: players.map((p) => p.id) },
      players,
      [],
      rng('replay'),
      '14:8000',
      NOW,
    );
    const primary = begun.spotlight.slots[0]!.playerId!;
    expect(spotlightPrivateFor(begun.spotlight, primary, NOW + 1)).toMatchObject({
      status: 'assigned',
      burnDeadline: NOW + SPOTLIGHT_WINDOW_MS,
      canBurn: true,
    });
    expect(spotlightPrivateFor(begun.spotlight, 'UNKNOWN', NOW + 1)).toBeNull();
    expect(spotlightPrivateFor(begun.spotlight, primary, NOW + SPOTLIGHT_WINDOW_MS + 1)).toBeNull();

    const handoff = handoffSpotlight(begun.spotlight, players);
    expect(spotlightPrivateFor(handoff.spotlight, primary, NOW + SPOTLIGHT_WINDOW_MS + 1)).toMatchObject({
      burnDeadline: NOW + SPOTLIGHT_TOTAL_MS,
      canBurn: false, // kept primary is locked after its own full window
    });
    expect(spotlightPrivateFor(handoff.spotlight, primary, NOW + SPOTLIGHT_TOTAL_MS + 1)).toBeNull();
    expect(spotlightPrivateFor(completeSpotlight(handoff.spotlight), primary, NOW + SPOTLIGHT_TOTAL_MS)).toBeNull();
  });

  it('never replays a burned primary; it replays the replacement after handoff', () => {
    const begun = beginSpotlight(
      { roles: ['subject'], eligibleIds: players.map((p) => p.id) },
      players,
      [],
      rng('replay-burn'),
      '15:9000',
      NOW,
    );
    const primary = begun.spotlight.slots[0]!.playerId!;
    const burned = burnSpotlight(begun.spotlight, primary, NOW + 1);
    expect(spotlightPrivateFor(burned.spotlight, primary, NOW + 2)).toBeNull();
    const handoff = handoffSpotlight(burned.spotlight, players);
    const replacement = handoff.spotlight.slots[0]!.playerId!;
    expect(spotlightPrivateFor(handoff.spotlight, replacement, NOW + SPOTLIGHT_WINDOW_MS + 1)).toMatchObject({
      burnDeadline: NOW + SPOTLIGHT_TOTAL_MS,
      canBurn: true,
    });
  });
});
