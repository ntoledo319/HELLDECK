// D-127: the real corpus is injected into the engine and actually dealt (not the stubs).
import { describe, expect, it } from 'vitest';
import { initialRoom, reduce, type GameEvent, type NightConfig, type RoomState } from '@helldeck/engine';
import { loadContent } from '../src/content.js';

const CFG: NightConfig = { depth: 5, vibe: 'feral', stageMode: false, crewId: 'content-test', irlFamiliar: true };

/** Drive real events + timers just far enough to observe the cards the engine deals. */
function dealtCardIds(): Set<string> {
  let s: RoomState = initialRoom('HELL', 0, true);
  const pending = new Map<string, number>();
  const dispatch = (e: GameEvent): void => {
    const r = reduce(s, e, 'HELL');
    s = r.state;
    for (const ef of r.effects) {
      if (ef.k === 'SCHEDULE') pending.set(ef.timerId, ef.atMs);
      else if (ef.k === 'CANCEL') pending.delete(ef.timerId);
    }
  };
  for (let i = 0; i < 5; i++) {
    dispatch({ t: 'JOIN', id: `P${i}`, name: `P${i}`, avatar: i, at: i });
    dispatch({ t: 'ATTEST18', id: `P${i}` });
    dispatch({ t: 'CEILING', id: `P${i}`, v: 5 });
  }
  dispatch({ t: 'CONFIG', id: 'P0', cfg: CFG });
  dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
  const ids = new Set<string>();
  for (let step = 0; step < 60; step++) {
    if (s.deal?.done === true && s.deal.card) ids.add(s.deal.card.id);
    let best: [string, number] | null = null;
    for (const [id, at] of pending) if (best === null || at < best[1]) best = [id, at];
    if (best === null) break;
    pending.delete(best[0]);
    dispatch({ t: 'TIMER', timerId: best[0], at: best[1] });
  }
  return ids;
}

describe('D-127 content wiring', () => {
  it('loadContent reports every wired deck and is idempotent', () => {
    const a = loadContent();
    const b = loadContent();
    expect(b).toEqual(a);
    const by = Object.fromEntries(a.map((r) => [r.deck, r.cards]));
    // the funnel-verified corpus sizes (content/decks/*.json)
    expect(by).toMatchObject({ roast: 157, fillin: 161, overunder: 144, confession: 150, scatter: 94, poison: 99, redflag: 96, alibi: 91, titlefight: 32 });
    for (const r of a) expect(r.cards).toBeGreaterThan(0);
  });

  it('a real night deals REAL v3 cards, never the module stubs', () => {
    loadContent();
    const ids = dealtCardIds();
    expect(ids.size).toBeGreaterThan(0);
    // every card the engine dealt is a funnel-verified v3 id from the JSON, not a *_stub_ card
    for (const id of ids) {
      expect(id, `dealt a stub instead of real content: ${id}`).not.toMatch(/_stub_/);
      expect(id, `dealt a non-v3 id: ${id}`).toMatch(/_v3_\d+$/);
    }
  });
});
