// THE STAGE + LIFT-TO-SIN web fallback — spec Part 7 / HDRealRules2 §The Stage, task D-135.
// At N>=5 the lobby flips stageMode on: the HOST's phone lies face-up mid-table as the shared
// screen, so it must never show a private ballot. This is the WEB fallback — a MANUAL flip
// button, not the Android accelerometer (that sensor is M4 garnish; the button ships first).
//
// Flat (default): during a private-input phase or while a private overlay is pending, the stage
//   shows only PUBLIC faces (game title, live timer, safe status) + a big PICK UP TO SIN. Public
//   phases otherwise render straight through — the payoff belongs on the table.
// Lifted: the host tapped PICK UP TO SIN → their real per-viewer screen appears (cast the vote),
//   with a LAY IT BACK bar. Lift permission is keyed to the exact circle/phase/sub/private
//   payload and own-ballot acknowledgement, so any change is flat synchronously and cannot
//   leak one frame of the next secret (or leave a completed choice face-up).
import type { ComponentChildren } from 'preact';
import { useContext, useEffect, useRef, useState } from 'preact/hooks';
import { ConnectionGeneration } from '../connection';
import { deckOf, subOf } from '../games/wire';
import type { Net } from '../net/ws';
import type { PlayerView, RoomView } from '../view';
import { Ring } from './bits';
import { GAME_META } from './intro';
import { stageDecisionSignature, stageGate, stageLiftForContext, stagePrivacyKey } from './stage.logic';

interface StagePrivateOverlay {
  id: number;
  content: ComponentChildren;
  expiresAt?: number;
}

export function StageShell({
  view,
  me,
  net,
  children,
  privateOverlay = null,
}: {
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  children: ComponentChildren;
  privateOverlay?: StagePrivateOverlay | null;
}) {
  const on = (view.config?.stageMode ?? false) && me?.role === 'host';
  const connectionGeneration = useContext(ConnectionGeneration);
  const [liftedFor, setLiftedFor] = useState<string | null>(null);
  const sub = subOf(view.gameView);
  const privateOverlayId = privateOverlay?.id ?? null;
  const decisionSignature = stageDecisionSignature(view.gameView);
  const privacyKey = stagePrivacyKey(
    view.phase.k,
    view.circleIdx,
    sub,
    privateOverlayId,
    on,
    decisionSignature,
    connectionGeneration,
  );
  const previousPrivacyKeyRef = useRef(privacyKey);
  const activeLift = stageLiftForContext(liftedFor, previousPrivacyKeyRef.current, privacyKey);
  previousPrivacyKeyRef.current = privacyKey;
  useEffect(() => {
    setLiftedFor(null);
  }, [privacyKey]);
  const gate = stageGate(on, view.phase.k, privateOverlayId, activeLift, privacyKey);

  if (gate === 'passthrough') {
    return (
      <>
        {children}
        {privateOverlay?.content}
      </>
    );
  }

  if (gate === 'lifted') {
    return (
      <div class="stage-lifted">
        <button class="lay-flat" onClick={() => setLiftedFor(null)}>
          ▲ LAY IT BACK ON THE TABLE ▲
        </button>
        {children}
        {privateOverlay?.content}
      </div>
    );
  }
  return (
    <StageFace
      view={view}
      net={net}
      privatePending={privateOverlay !== null}
      privateDeadline={privateOverlay?.expiresAt ?? null}
      onLift={() => setLiftedFor(privacyKey)}
    />
  );
}

/** The face-up stage during a private-input phase: public info only, plus the lift button. */
function StageFace({
  view,
  net,
  privatePending,
  privateDeadline,
  onLift,
}: {
  view: RoomView;
  net: Net;
  privatePending: boolean;
  privateDeadline: number | null;
  onLift: () => void;
}) {
  const deck = deckOf(view.gameView) ?? '';
  const title = GAME_META[deck]?.title ?? 'THE PIT';
  const deadline = privateDeadline ?? (view.phase.k === 'INPUT' ? view.phase.deadline : null);
  return (
    <main class="screen stage-face">
      <div class="stage-tag breathe">THE STAGE · FACE-UP ON THE TABLE</div>
      <h1 class="game-title">{title}</h1>
      {deadline !== null && (
        <div class="stage-ring">
          <Ring deadline={deadline} now={() => net.serverNow()} />
        </div>
      )}
      <div class="subject-banner" role="status" aria-live="polite">
        {privatePending ? 'SOMETHING PRIVATE IS WAITING.' : 'THE TABLE IS DECIDING.'}
      </div>
      <button class="btn-blood big" onClick={onLift}>
        PICK UP TO SIN
      </button>
      <p class="game-blurb">
        {privatePending
          ? 'Lift the phone to your face, handle it in private, then lay it flat. No peeking.'
          : "Lift the phone to your face, cast in private, lay it flat when you're done. No peeking."}
      </p>
    </main>
  );
}
