import { describe, expect, it } from 'vitest';
import {
  dismissPreview,
  expirePreview,
  parsePreviewMessage,
  preparePreviewReconnect,
  receivePreviewMessage,
  rejectPreviewBurn,
  requestPreviewBurn,
  type PreviewAssigned,
  type PreviewClientState,
} from './preview.logic';

const assigned = (overrides: Partial<PreviewAssigned> = {}): PreviewAssigned => ({
  status: 'assigned',
  previewId: 'deal:c2:l0',
  card: { id: 'redflag_v3_12', text: 'A private card' },
  burnDeadline: 20_000,
  revealAt: 20_000,
  canBurn: true,
  ...overrides,
});

const live = (overrides: Partial<PreviewClientState> = {}): PreviewClientState => ({
  id: 4,
  assignment: assigned(),
  burnPending: false,
  dismissed: false,
  ...overrides,
});

describe('card-preview PRIVATE contract', () => {
  it('accepts complete assigned and released messages', () => {
    expect(parsePreviewMessage(assigned())).toEqual(assigned());
    expect(parsePreviewMessage({ status: 'released', previewId: 'deal:c2:l0' })).toEqual({
      status: 'released',
      previewId: 'deal:c2:l0',
    });
  });

  it('rejects malformed cards and impossible timing', () => {
    expect(parsePreviewMessage(null)).toBeNull();
    expect(parsePreviewMessage({ ...assigned(), card: { text: 'missing id' } })).toBeNull();
    expect(parsePreviewMessage({ ...assigned(), canBurn: 'yes' })).toBeNull();
    expect(parsePreviewMessage({ ...assigned(), revealAt: 19_999 })).toBeNull();
  });
});

describe('card-preview acknowledgement state', () => {
  it('opens only a live assignment', () => {
    expect(receivePreviewMessage(null, assigned(), 4, 10_000)).toEqual(live());
    expect(receivePreviewMessage(null, assigned(), 4, 20_000)).toBeNull();
  });

  it('keeps the curtain open while a burn is pending', () => {
    const pending = requestPreviewBurn(live(), 'deal:c2:l0', 10_000);
    expect(pending).toMatchObject({ burnPending: true, dismissed: false });
    expect(dismissPreview(pending, 'deal:c2:l0')).toBe(pending);
  });

  it('recovers actions after a server rejection', () => {
    expect(rejectPreviewBurn(live({ burnPending: true }))).toMatchObject({ burnPending: false });
    expect(rejectPreviewBurn(live())).toEqual(live());
  });

  it('lets reconnect replay rebuild pending work without reopening a dismissed curtain', () => {
    expect(preparePreviewReconnect(live({ burnPending: true }))).toBeNull();
    expect(preparePreviewReconnect(live())).toBeNull();
    expect(preparePreviewReconnect(live({ dismissed: true }))).toMatchObject({ dismissed: true });
  });

  it('will not burn stale, unavailable, dismissed, or mismatched previews', () => {
    const state = live();
    const unavailable = { ...state, assignment: assigned({ canBurn: false }) };
    const dismissed = { ...state, dismissed: true };
    expect(requestPreviewBurn(state, 'other', 10_000)).toBe(state);
    expect(requestPreviewBurn(unavailable, 'deal:c2:l0', 10_000)).toBe(unavailable);
    expect(requestPreviewBurn(state, 'deal:c2:l0', 20_000)).toBe(state);
    expect(requestPreviewBurn(dismissed, 'deal:c2:l0', 10_000)).toBe(dismissed);
  });

  it('closes only for a matching release acknowledgement', () => {
    const pending = live({ burnPending: true });
    expect(receivePreviewMessage(pending, { status: 'released', previewId: 'other' }, 5, 10_000)).toBe(pending);
    expect(receivePreviewMessage(pending, { status: 'released', previewId: 'deal:c2:l0' }, 5, 10_000)).toBeNull();
  });

  it('does not reopen a dismissed preview on reconnect replay', () => {
    const dismissed = dismissPreview(live(), 'deal:c2:l0');
    expect(receivePreviewMessage(dismissed, assigned({ canBurn: false }), 5, 11_000)).toMatchObject({
      id: 5,
      dismissed: true,
    });
  });

  it('expires only the matching preview', () => {
    expect(expirePreview(live(), 'other')).toEqual(live());
    expect(expirePreview(live(), 'deal:c2:l0')).toBeNull();
  });
});
