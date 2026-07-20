import { describe, expect, it } from 'vitest';
import {
  dismissSpotlight,
  expireSpotlight,
  parseSpotlightMessage,
  receiveSpotlightMessage,
  requestSpotlightBurn,
  spotlightBurnCopy,
  spotlightRoleTitle,
  type SpotlightAssigned,
  type SpotlightClientState,
} from './spotlight.logic';

const assigned = (overrides: Partial<SpotlightAssigned> = {}): SpotlightAssigned => ({
  status: 'assigned',
  ceremonyId: 'spotlight:c4:l0',
  role: 'subject',
  burnDeadline: 20_000,
  announceAt: 30_000,
  canBurn: true,
  ...overrides,
});

const live = (overrides: Partial<SpotlightClientState> = {}): SpotlightClientState => ({
  id: 7,
  assignment: assigned(),
  burnPending: false,
  dismissed: false,
  ...overrides,
});

describe('spotlight PRIVATE contract', () => {
  it('accepts the exact assigned and released shapes', () => {
    expect(parseSpotlightMessage(assigned())).toEqual(assigned());
    expect(parseSpotlightMessage({ status: 'released', ceremonyId: 'spotlight:c4:l0' })).toEqual({
      status: 'released',
      ceremonyId: 'spotlight:c4:l0',
    });
  });

  it('rejects malformed, unknown-role, and impossible-timing payloads', () => {
    expect(parseSpotlightMessage(null)).toBeNull();
    expect(parseSpotlightMessage({ ...assigned(), role: 'victim' })).toBeNull();
    expect(parseSpotlightMessage({ ...assigned(), canBurn: 'yes' })).toBeNull();
    expect(parseSpotlightMessage({ ...assigned(), announceAt: 19_999 })).toBeNull();
  });
});

describe('spotlight acknowledgement state', () => {
  it('opens a future assignment but ignores one whose burn window is already dead', () => {
    expect(receiveSpotlightMessage(null, assigned(), 7, 10_000)).toEqual(live());
    expect(receiveSpotlightMessage(null, assigned(), 7, 20_000)).toBeNull();
  });

  it('marks a burn pending without optimistically closing the role', () => {
    const pending = requestSpotlightBurn(live(), 'spotlight:c4:l0', 10_000);
    expect(pending).toMatchObject({ burnPending: true, dismissed: false });
    expect(requestSpotlightBurn(pending, 'spotlight:c4:l0', 10_001)).toBe(pending);
  });

  it('will not request an invalid, unavailable, stale, or mismatched burn', () => {
    const state = live();
    expect(requestSpotlightBurn(state, 'other', 10_000)).toBe(state);
    expect(requestSpotlightBurn({ ...state, assignment: assigned({ canBurn: false }) }, 'spotlight:c4:l0', 10_000)).toMatchObject({
      burnPending: false,
    });
    expect(requestSpotlightBurn(state, 'spotlight:c4:l0', 20_000)).toBe(state);
  });

  it('closes only for a matching released acknowledgement', () => {
    const state = live({ burnPending: true });
    expect(receiveSpotlightMessage(state, { status: 'released', ceremonyId: 'other' }, 8, 10_000)).toBe(state);
    expect(receiveSpotlightMessage(state, { status: 'released', ceremonyId: 'spotlight:c4:l0' }, 9, 10_000)).toBeNull();
  });

  it('LET IT RIDE hides locally and duplicate assignment delivery cannot reopen it', () => {
    const dismissed = dismissSpotlight(live(), 'spotlight:c4:l0');
    expect(dismissed).toMatchObject({ dismissed: true });
    expect(receiveSpotlightMessage(dismissed, assigned({ canBurn: false }), 8, 11_000)).toMatchObject({
      id: 8,
      dismissed: true,
    });
  });

  it('does not allow dismissal while a burn acknowledgement is pending', () => {
    const pending = live({ burnPending: true });
    expect(dismissSpotlight(pending, 'spotlight:c4:l0')).toBe(pending);
  });

  it('expires only the matching ceremony', () => {
    const state = live();
    expect(expireSpotlight(state, 'other')).toBe(state);
    expect(expireSpotlight(state, 'spotlight:c4:l0')).toBeNull();
  });

  it('renders every private role as an explicit assignment', () => {
    expect(spotlightRoleTitle('subject')).toBe("YOU'RE THE SUBJECT");
    expect(spotlightRoleTitle('fighter-b')).toBe("YOU'RE FIGHTER B");
  });

  it('does not mistake a locked assignment for an empty Brimstone balance', () => {
    const copy = spotlightBurnCopy(false, false);
    expect(copy.button).toBe('BURN UNAVAILABLE');
    expect(copy.note).toMatch(/assignment is locked/i);
    expect(`${copy.button} ${copy.note}`).not.toMatch(/no brimstone|brimstone left/i);
  });
});
