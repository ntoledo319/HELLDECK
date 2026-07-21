// CONFESSION OR CAP screens — spec 5.4, client half of task D-123. Aligned to the
// LANDED engine module games/confession.ts view() shapes (PICK/LOCK/PERFORM/JURY/REVEAL).
// PICK: 3 sins on the confessor's phone only — the room doesn't even learn WHO is
// choosing (no confessorId in their frames). LOCK: blocking (4.7) — heavy hold-to-commit
// TRUE/FALSE that never rides the wire until REVEAL; the room gets the pit vote at 30s.
// PERFORM: sell it, I REST MY CASE. JURY: BELIEVE/CAP. REVEAL: stamp slam + HUNG JURY.
// LAW (5.4 acceptance): the truth renders ONLY from the REVEAL view; the hand renders
// ONLY from the confessor's PICK view. This file has no other source for either.
import { useEffect, useRef, useState } from 'preact/hooks';
import { useConnectionOptimistic } from '../connection';
import { confessionStamp, juryResult, LOCK_HOLD_MS } from '../games/logic';
import {
  asDeckView,
  confessionPayload,
  type ConfessionJuryView,
  type ConfessionLockView,
  type ConfessionPerformView,
  type ConfessionPickView,
  type ConfessionRevealView,
} from '../games/wire';
import type { Net } from '../net/ws';
import type { PlayerView, RoomView } from '../view';
import { FireButton, Ring } from './bits';
import { HoldShell } from './hold';
import { InputFallback } from './roast';
import '../style/games.css';

const nameOf = (view: RoomView, id: string | null): string =>
  (id !== null ? view.players.find((p) => p.id === id)?.name : undefined) ?? '???';

export function ConfessionScreen({
  view,
  me,
  net,
  deadline,
  heat,
}: {
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadline: number | null;
  heat: number;
}) {
  const gv = view.gameView;
  const p = asDeckView<ConfessionPickView>(gv, 'confession', 'PICK');
  if (p) return <Pick key={`p${p.loop}`} v={p} net={net} deadline={deadline ?? p.deadline} />;
  const l = asDeckView<ConfessionLockView>(gv, 'confession', 'LOCK');
  if (l) return <Lock key={`l${l.loop}`} v={l} view={view} me={me} net={net} />;
  const pf = asDeckView<ConfessionPerformView>(gv, 'confession', 'PERFORM');
  if (pf) return <Perform key={`f${pf.loop}`} v={pf} view={view} net={net} deadline={deadline ?? pf.deadline} heat={heat} />;
  const j = asDeckView<ConfessionJuryView>(gv, 'confession', 'JURY');
  if (j) return <Jury key={`j${j.loop}`} v={j} net={net} deadline={deadline ?? j.deadline} />;
  return <InputFallback sub="CONFESSION" deadline={deadline} net={net} />;
}

/** PICK: three sins on the confessor's phone; everyone else sees an anonymous choosing. */
function Pick({ v, net, deadline }: { v: ConfessionPickView; net: Net; deadline: number }) {
  const [picked, setPicked] = useConnectionOptimistic<number | null>(null);
  const pick = (i: number): void => {
    if (picked !== null) return;
    if (net.send({ t: 'INPUT', p: confessionPayload.pick(i) })) setPicked(i);
  };
  if (!v.youAreConfessor || !v.hand) {
    // The room does not learn who is choosing — no name here, by redaction law.
    return (
      <main class="screen waiting">
        <header class="vote-head">
          <span class="vote-tally">SIN {v.loop + 1} OF {v.loops} · CHOOSING IN PRIVATE</span>
          <Ring deadline={deadline} now={() => net.serverNow()} />
        </header>
        <h1 class="lights">THE ACCUSED</h1>
        <p class="game-blurb">…is choosing their sin. Three on the table. You'll hear exactly one.</p>
      </main>
    );
  }
  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">PICK YOUR SIN · THE ROOM SEES NOTHING</span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <div class={picked !== null ? 'sin-cards locked' : 'sin-cards'}>
        {v.hand.map((c, i) => (
          <button
            key={c.id}
            class={'sin-card' + (picked === i ? ' picked' : '')}
            disabled={picked !== null}
            aria-pressed={picked === i}
            onClick={() => pick(i)}
          >
            {c.text}
          </button>
        ))}
      </div>
      {picked !== null && <div class="locked-banner flash-in" role="status" aria-live="polite">CHOSEN. THE OTHER TWO NEVER EXISTED.</div>}
      <p class="gate-note">Timeout takes the first card. Choose like it matters.</p>
    </main>
  );
}

/** Heavy hold-to-commit slab — a truth lock is not a tap (design law: deliberate weight). */
function HoldSlab({ label, sub, onCommit }: { label: string; sub: string; onCommit: () => void }) {
  const [holding, setHolding] = useState(false);
  const tRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const stop = (): void => {
    if (tRef.current) clearTimeout(tRef.current);
    tRef.current = null;
    setHolding(false);
  };
  const start = (e: Event): void => {
    e.preventDefault();
    if (tRef.current) return;
    setHolding(true);
    tRef.current = setTimeout(() => {
      tRef.current = null;
      onCommit();
    }, LOCK_HOLD_MS);
  };
  useEffect(() => stop, []);
  return (
    <button
      class={holding ? 'hold-slab holding' : 'hold-slab'}
      onPointerDown={start}
      onPointerUp={stop}
      onPointerLeave={stop}
      onPointerCancel={stop}
      onClick={(event) => {
        // Pointer clicks follow the hold gesture and must not double-commit.
        // Keyboard and assistive activations synthesize detail=0 clicks.
        if (event.detail === 0) onCommit();
      }}
      onBlur={stop}
      onContextMenu={(e) => e.preventDefault()}
      aria-label={`Hold to lock ${label}. ${sub}`}
    >
      <i class="hold-fill" />
      <span class="hold-label">{label}</span>
      <span class="hold-sub">{sub}</span>
    </button>
  );
}

/**
 * LOCK — blocking (4.7). Serves both INPUT and WAITING_ON phases: the confessor keeps
 * the lock UI through the shame transition; the room gets the pit vote at 30s.
 * The chosen sin is already public (the ceremony announced it) — the room reads along.
 */
function Lock({ v, view, me, net }: { v: ConfessionLockView; view: RoomView; me: PlayerView | null; net: Net }) {
  const [committed, setCommitted] = useConnectionOptimistic<boolean | null>(null);
  const [pitPicked, setPitPicked] = useConnectionOptimistic<'drag' | 'pit' | null>(null);

  if (v.youAreConfessor) {
    const commit = (truth: boolean): void => {
      if (committed !== null) return;
      if (net.send({ t: 'INPUT', p: confessionPayload.lock(truth) })) setCommitted(truth);
    };
    return (
      <main class="screen vote">
        <header class="vote-head">
          <span class="vote-tally">LOCK IT · THIS NEVER LEAVES THE VAULT</span>
        </header>
        <h1 class="prompt">{v.card.text}</h1>
        {committed === null ? (
          <>
            <div class="lock-slabs">
              <HoldSlab label="TRUE" sub="it happened" onCommit={() => commit(true)} />
              <HoldSlab label="FALSE" sub="a stone-cold cap" onCommit={() => commit(false)} />
            </div>
            <p class="gate-note">Hold to lock. Not even your own screen will show it again until the stamp.</p>
            <button class="fifth-btn" onClick={() => net.send({ t: 'FIFTH' })}>
              THE WITNESS TAKES THE FIFTH
            </button>
          </>
        ) : (
          <div class="locked-banner flash-in" role="status" aria-live="polite">LOCKED. SEALED. NOW GO SELL IT.</div>
        )}
      </main>
    );
  }

  const pitChoice = pitPicked ?? v.youPitVoted;
  const votePit = (choice: 'drag' | 'pit'): void => {
    if (pitChoice !== null) return;
    if (net.send({ t: 'INPUT', p: confessionPayload.pit(choice) })) setPitPicked(choice);
  };
  return (
    <main class="screen waiting">
      <div class="waiting-label breathe">UNDER OATH</div>
      <h1 class="lights">{nameOf(view, v.confessorId)}</h1>
      <p class="reveal-prompt">“{v.card.text}”</p>
      <p class="game-blurb">…is locking it in. True or cap — the phone knows before you do.</p>
      {v.pitOpen && pitChoice === null && (
        <div class="pit-row flash-in">
          <button type="button" class="pit-drag" onClick={() => votePit('drag')}>
            DRAG THEM BACK
          </button>
          <button type="button" class="pit-feed" onClick={() => votePit('pit')}>
            FEED THEM TO THE PIT
          </button>
        </div>
      )}
      {pitChoice !== null && (
        <div class="locked-banner flash-in" role="status" aria-live="polite">
          {pitChoice === 'pit' ? 'YOU FED THEM TO THE PIT' : 'MERCY, NOTED. DRAG THEM BACK.'}
        </div>
      )}
      {me?.role === 'host' && (
        <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
          VOID ROUND
        </button>
      )}
    </main>
  );
}

/** PERFORM (45s, verbal): the confessor sells it; the room reads along and watches their hands. */
function Perform({
  v,
  view,
  net,
  deadline,
  heat,
}: {
  v: ConfessionPerformView;
  view: RoomView;
  net: Net;
  deadline: number;
  heat: number;
}) {
  if (v.youAreConfessor) {
    return (
      <main class="screen vote">
        <header class="vote-head">
          <span class="vote-tally">SELL IT. EYE CONTACT HELPS.</span>
          <Ring deadline={deadline} now={() => net.serverNow()} />
        </header>
        <h1 class="prompt">{v.card.text}</h1>
        <button class="btn-blood big rest-btn" onClick={() => net.send({ t: 'REST' })}>
          I REST MY CASE
        </button>
      </main>
    );
  }
  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">{nameOf(view, v.confessorId)} CONFESSES</span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <h1 class="prompt">{v.card.text}</h1>
      <div class="watch-tag breathe">WATCH THEIR HANDS.</div>
      <div class="reveal-actions">
        <FireButton onFire={() => net.fire()} heat={heat} />
      </div>
    </main>
  );
}

/** JURY (12s skippable): BELIEVE / CAP. The confessor watches the room decide. */
function Jury({ v, net, deadline }: { v: ConfessionJuryView; net: Net; deadline: number }) {
  const [picked, setPicked] = useConnectionOptimistic<'believe' | 'cap' | null>(null);
  const chosen = picked ?? v.youVoted;
  const locked = chosen !== null;
  const vote = (verdict: 'believe' | 'cap'): void => {
    if (locked) return;
    if (net.send({ t: 'INPUT', p: confessionPayload.verdict(verdict) })) setPicked(verdict);
  };
  if (v.youAreConfessor) {
    return (
      <main class="screen waiting">
        <header class="vote-head">
          <span class="vote-tally">HOLD STILL · THEY'RE DECIDING</span>
          <Ring deadline={deadline} now={() => net.serverNow()} />
        </header>
        <h1 class="lights">
          {v.votedCount}/{v.eligible}
        </h1>
        <p class="game-blurb">They're deciding your fate. Whatever your face is doing — stop it.</p>
      </main>
    );
  }
  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {v.votedCount}/{v.eligible} VERDICTS IN
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <h1 class="prompt">{v.card.text}</h1>
      <div class={locked ? 'bet-slabs locked' : 'bet-slabs'}>
        <button
          type="button"
          class={'bet-slab' + (chosen === 'believe' ? ' picked' : '')}
          disabled={locked}
          aria-pressed={chosen === 'believe'}
          onClick={() => vote('believe')}
        >
          BELIEVE
        </button>
        <button
          type="button"
          class={'bet-slab' + (chosen === 'cap' ? ' picked' : '')}
          disabled={locked}
          aria-pressed={chosen === 'cap'}
          onClick={() => vote('cap')}
        >
          CAP
        </button>
      </div>
      {locked && <div class="locked-banner flash-in" role="status" aria-live="polite">VERDICT LOCKED — {chosen?.toUpperCase()}</div>}
    </main>
  );
}

/** REVEAL: 3-2-1 -> TRUE/FALSE stamp slam + jury spread + fooled/caught/HUNG JURY. */
export function ConfessionReveal({
  view,
  me,
  net,
  deadlines,
  heat,
  epoch,
  holdSince,
}: {
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadlines: ReadonlyMap<string, number>;
  heat: number;
  epoch: number;
  holdSince: number;
}) {
  const rv = asDeckView<ConfessionRevealView>(view.gameView, 'confession', 'REVEAL');
  const verdict = rv ? confessionStamp(rv, nameOf(view, rv.confessorId)) : null;
  const yours = rv ? juryResult(rv.youVoted, rv.truth) : null;
  const fmt = (w: number): string => (Number.isInteger(w) ? String(w) : w.toFixed(1));
  return (
    <HoldShell net={net} deadlines={deadlines} heat={heat} epoch={epoch} holdSince={holdSince} isHost={me?.role === 'host'}>
      {rv && verdict && (
        <>
          {rv.card && <p class="reveal-prompt">{rv.card.text}</p>}
          {verdict.stamp !== null ? (
            <div class={verdict.stamp === 'TRUE' ? 'stamp stamp-true flash-in' : 'stamp stamp-false flash-in'}>
              {verdict.stamp}
            </div>
          ) : (
            <h1 class="lights">SILENCE</h1>
          )}
          {rv.verdict === 'HUNG' && <div class="double-tag">HUNG JURY</div>}
          <div class="subject-banner">{verdict.banner}</div>
          {!rv.voided && (
            <div class="jury-spread">
              <div class="jury-cell">
                <span class="vs-label">BELIEVE</span>
                <span class="vs-num">{fmt(rv.spread.believe)}</span>
              </div>
              <div class="jury-cell">
                <span class="vs-label">CAP</span>
                <span class="vs-num">{fmt(rv.spread.cap)}</span>
              </div>
            </div>
          )}
          {yours !== null && (
            <div class="you-chip">{yours === 'right' ? 'YOU READ THEM RIGHT — +1' : 'THEY PLAYED YOU'}</div>
          )}
        </>
      )}
    </HoldShell>
  );
}
