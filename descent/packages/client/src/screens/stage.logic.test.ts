import { describe, expect, it } from 'vitest';
import { stageDecisionSignature, stageGate, stageLiftForContext, stagePrivacyKey } from './stage.logic';

describe('stage privacy gate', () => {
  it('passes ordinary screens and every non-stage phone through', () => {
    const key = stagePrivacyKey('DEAL', 2, null, null, true);
    expect(stageGate(true, 'DEAL', null, null, key)).toBe('passthrough');
    expect(stageGate(false, 'DEAL', 7, null, key)).toBe('passthrough');
  });

  it('keeps a pending DEAL preview flat until lifted for that exact payload', () => {
    const key = stagePrivacyKey('DEAL', 2, null, 7, true);
    expect(stageGate(true, 'DEAL', 7, null, key)).toBe('flat');
    expect(stageGate(true, 'DEAL', 7, key, key)).toBe('lifted');
  });

  it('drops synchronously when the private payload, phase, sub, or circle changes', () => {
    const liftedFor = stagePrivacyKey('DEAL', 2, null, 7, true);
    const changed = [
      stagePrivacyKey('DEAL', 2, null, 8, true),
      stagePrivacyKey('INPUT', 2, 'VOTE', null, true),
      stagePrivacyKey('DEAL', 2, 'LOCK', 7, true),
      stagePrivacyKey('DEAL', 3, null, 7, true),
      stagePrivacyKey('DEAL', 2, null, 7, false),
    ];
    for (const key of changed) expect(stageGate(true, 'DEAL', 7, liftedFor, key)).toBe('flat');
  });

  it('invalidates a lift synchronously when a fresh socket reconnects', () => {
    const firstSocket = stagePrivacyKey('INPUT', 2, 'VOTE', null, true, '{}', 1);
    const nextSocket = stagePrivacyKey('INPUT', 2, 'VOTE', null, true, '{}', 2);
    expect(stageLiftForContext(firstSocket, firstSocket, nextSocket)).toBeNull();
    expect(stageGate(true, 'INPUT', null, firstSocket, nextSocket)).toBe('flat');
  });

  it('does not resurrect a lift when a later transition returns to an old key', () => {
    const oldKey = stagePrivacyKey('INPUT', 2, 'VOTE', null, true);
    const interveningKey = stagePrivacyKey('WAITING_ON', 2, 'VOTE', null, true);
    expect(stageLiftForContext(oldKey, interveningKey, oldKey)).toBeNull();
  });

  it('protects INPUT and WAITING_ON even without a separate overlay', () => {
    for (const phase of ['INPUT', 'WAITING_ON']) {
      const key = stagePrivacyKey(phase, 1, 'BALLOT', null, true);
      expect(stageGate(true, phase, null, null, key)).toBe('flat');
      expect(stageGate(true, phase, null, key, key)).toBe('lifted');
    }
  });

  it('changes privacy context when this viewer receives a decision acknowledgement', () => {
    expect(stageDecisionSignature({ deck: 'roast', sub: 'VOTE', youVoted: null })).not.toBe(
      stageDecisionSignature({ deck: 'roast', sub: 'VOTE', youVoted: 'P2' }),
    );
    expect(stageDecisionSignature({ deck: 'fillin', sub: 'WRITE', you: { yourAnswer: null, yourTone: null } })).not.toBe(
      stageDecisionSignature({ deck: 'fillin', sub: 'WRITE', you: { yourAnswer: 'hell yes', yourTone: null } }),
    );
    expect(stageDecisionSignature({ deck: 'poison', sub: 'VOTE', votedCount: 1 })).toBe('{}');
    expect(stageDecisionSignature({ deck: 'confession', sub: 'LOCK', youPitVoted: null })).not.toBe(
      stageDecisionSignature({ deck: 'confession', sub: 'LOCK', youPitVoted: 'drag' }),
    );
  });
});
