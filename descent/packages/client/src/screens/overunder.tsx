// OVER/UNDER screens — spec 5.3, client half of task D-122. Aligned to the LANDED
// engine module games/overunder.ts view() shapes (DEBATE/BET/TRUTH/REVEAL).
// DEBATE: the argument is out loud; only the scribe's phone grows the line dial.
// BET: two slabs, subject goes receipt-hunting. TRUTH: blocking (4.7) — subject's
// number pad + THE WITNESS TAKES THE FIFTH; the room gets the shame screen and,
// 30s in, the pit vote. REVEAL: number vs line slam. The truth number is rendered
// ONLY from the REVEAL view — this file never has it earlier, by construction.
import { useState } from 'preact/hooks';
import { betResult, ouBanner, padPress, padValue, stepLine, DIAL_STEPS } from '../games/logic';
import {
  asDeckView,
  overunderPayload,
  type OverUnderBetView,
  type OverUnderDebateView,
  type OverUnderRevealView,
  type OverUnderTruthView,
} from '../games/wire';
import type { Net } from '../net/ws';
import type { PlayerView, RoomView } from '../view';
import { Ring } from './bits';
import { HoldShell } from './hold';
import { InputFallback } from './roast';
import '../style/games.css';

const nameOf = (view: RoomView, id: string | null): string =>
  (id !== null ? view.players.find((p) => p.id === id)?.name : undefined) ?? '???';

export function OverUnderScreen({
  view,
  me,
  net,
  deadline,
}: {
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadline: number | null;
}) {
  const gv = view.gameView;
  const d = asDeckView<OverUnderDebateView>(gv, 'overunder', 'DEBATE');
  if (d) return <Debate key={`d${d.loop}`} v={d} view={view} net={net} deadline={deadline ?? d.deadline} />;
  const b = asDeckView<OverUnderBetView>(gv, 'overunder', 'BET');
  if (b) return <Bet key={`b${b.loop}`} v={b} view={view} net={net} deadline={deadline ?? b.deadline} />;
  const t = asDeckView<OverUnderTruthView>(gv, 'overunder', 'TRUTH');
  if (t) return <Truth key={`t${t.loop}`} v={t} view={view} me={me} net={net} />;
  return <InputFallback sub="OVER/UNDER" deadline={deadline} net={net} />;
}

/** The stat face every phone shows from CORE_DEALT on. */
function StatCard({ v, view }: { v: { card: { text: string; receiptSurface: string }; subjectId: string }; view: RoomView }) {
  return (
    <>
      <div class="stat-about">THE SUBJECT: {nameOf(view, v.subjectId)}</div>
      <h1 class="prompt">{v.card.text}</h1>
      <div class="receipt-hint">RECEIPTS LIVE AT — {v.card.receiptSurface}</div>
    </>
  );
}

/** LINE DEBATE (25s, verbal): scribe rides the dial; everyone else argues at the number. */
function Debate({
  v,
  view,
  net,
  deadline,
}: {
  v: OverUnderDebateView;
  view: RoomView;
  net: Net;
  deadline: number;
}) {
  const [dial, setDial] = useState<number>(v.line ?? 0);
  const shown = v.youAreScribe ? dial : (v.line ?? null);

  const move = (delta: number): void => {
    const n = stepLine(dial, delta);
    setDial(n);
    net.send({ t: 'INPUT', p: overunderPayload.dial(n) });
  };

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          SUBJECT {v.loop + 1} OF {v.loops} · ARGUE. OUT LOUD.
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <StatCard v={v} view={view} />
      <div class="dial-face">{shown ?? '—'}</div>
      {v.youAreScribe ? (
        <>
          <div class="dial-steps">
            {DIAL_STEPS.map((s) => (
              <button key={s} class="dial-step" onClick={() => move(s)}>
                {s > 0 ? `+${s}` : s}
              </button>
            ))}
          </div>
          <button class="btn-blood big" onClick={() => net.send({ t: 'INPUT', p: overunderPayload.lock(dial) })}>
            LOCK THE LINE
          </button>
          <p class="gate-note">You hold the pen. The room holds the argument. Lock what the table lands on.</p>
        </>
      ) : (
        <p class="game-blurb breathe">{nameOf(view, v.scribeId)} holds the pen. Shout the line into their skull.</p>
      )}
    </main>
  );
}

/** BET (12s skippable): OVER/UNDER slabs; the subject gets marching orders instead. */
function Bet({ v, view, net, deadline }: { v: OverUnderBetView; view: RoomView; net: Net; deadline: number }) {
  const [picked, setPicked] = useState<'over' | 'under' | null>(null);
  const chosen = picked ?? v.youBet;
  const locked = chosen !== null;
  const bet = (side: 'over' | 'under'): void => {
    if (locked) return;
    setPicked(side);
    net.send({ t: 'INPUT', p: overunderPayload.bet(side) });
  };

  if (v.youAreSubject) {
    return (
      <main class="screen waiting">
        <div class="waiting-label breathe">GO DIG</div>
        <h1 class="lights">{v.card.receiptSurface}</h1>
        <p class="game-blurb">
          The line sits at {v.line}. They're betting on you right now. Come back with the number — the real one.
        </p>
      </main>
    );
  }

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {v.betCount}/{v.eligible} BETS IN
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <StatCard v={v} view={view} />
      <div class="line-strip">
        THE LINE: <b>{v.line}</b>
      </div>
      <div class={locked ? 'bet-slabs locked' : 'bet-slabs'}>
        <button class={'bet-slab' + (chosen === 'over' ? ' picked' : '')} disabled={locked} onClick={() => bet('over')}>
          OVER
        </button>
        <button class={'bet-slab' + (chosen === 'under' ? ' picked' : '')} disabled={locked} onClick={() => bet('under')}>
          UNDER
        </button>
      </div>
      {locked && <div class="locked-banner flash-in">LOCKED — {chosen?.toUpperCase()} {v.line}</div>}
    </main>
  );
}

/** Shared number pad — subject's testimony and the host's verbal relay use the same keys. */
function NumberPad({ value, onKey }: { value: string; onKey: (k: string) => void }) {
  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'clear', '0', 'back'];
  return (
    <>
      <div class="pad-read">{value === '' ? '···' : value}</div>
      <div class="pad">
        {keys.map((k) => (
          <button key={k} class={/^[0-9]$/.test(k) ? 'pad-key' : 'pad-key pad-fn'} onClick={() => onKey(k)}>
            {k === 'back' ? '⌫' : k === 'clear' ? 'CLR' : k}
          </button>
        ))}
      </div>
    </>
  );
}

/**
 * TRUTH — blocking (4.7). Timer paused (the core sent deadline:null / WAITING_ON);
 * this screen serves both the INPUT and WAITING_ON phases so the subject's half-typed
 * number survives the shame transition.
 */
function Truth({
  v,
  view,
  me,
  net,
}: {
  v: OverUnderTruthView;
  view: RoomView;
  me: PlayerView | null;
  net: Net;
}) {
  const [pad, setPad] = useState('');
  const [relayOpen, setRelayOpen] = useState(false);
  const [pitPicked, setPitPicked] = useState<'drag' | 'pit' | null>(null);
  const isHost = me?.role === 'host';
  const subjectName = nameOf(view, v.subjectId);

  if (v.youAreSubject) {
    const n = padValue(pad);
    return (
      <main class="screen vote">
        <header class="vote-head">
          <span class="vote-tally">THE LINE: {v.line} · CLOCK STOPPED FOR YOU</span>
        </header>
        <div class="stat-about">PRODUCE THE NUMBER</div>
        <h1 class="prompt">{v.card.text}</h1>
        <div class="receipt-hint">{v.card.receiptSurface}</div>
        <NumberPad value={pad} onKey={(k) => setPad(padPress(pad, k))} />
        <button
          class="btn-blood big"
          disabled={n === null}
          onClick={() => {
            if (n !== null) net.send({ t: 'INPUT', p: overunderPayload.truth(n) });
          }}
        >
          TESTIFY
        </button>
        <button class="fifth-btn" onClick={() => net.send({ t: 'FIFTH' })}>
          THE WITNESS TAKES THE FIFTH
        </button>
      </main>
    );
  }

  const pitChoice = pitPicked ?? v.youPitVoted;
  const votePit = (choice: 'drag' | 'pit'): void => {
    if (pitChoice !== null) return;
    setPitPicked(choice);
    net.send({ t: 'INPUT', p: overunderPayload.pit(choice) });
  };

  return (
    <main class="screen waiting">
      <div class="waiting-label breathe">WAITING ON</div>
      <h1 class="lights">{subjectName}'S RECEIPTS</h1>
      <p class="game-blurb">
        The line sits at {v.line}. The clock is dead until the number shows. Stare accordingly.
      </p>
      {v.pitOpen && pitChoice === null && (
        <div class="pit-row flash-in">
          <button class="pit-drag" onClick={() => votePit('drag')}>
            DRAG THEM BACK
          </button>
          <button class="pit-feed" onClick={() => votePit('pit')}>
            FEED THEM TO THE PIT
          </button>
        </div>
      )}
      {pitChoice !== null && (
        <div class="locked-banner flash-in">
          {pitChoice === 'pit' ? 'YOU FED THEM TO THE PIT' : 'MERCY, NOTED. DRAG THEM BACK.'}
        </div>
      )}
      {isHost && (
        <div class="host-tools">
          {!relayOpen ? (
            <>
              <button class="btn-ghost" onClick={() => setRelayOpen(true)}>
                TAKE IT VERBAL
              </button>
              <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
                VOID ROUND
              </button>
            </>
          ) : (
            <div class="relay-box">
              <div class="section-label">THEY SAID IT OUT LOUD — TYPE IT. FLAGGED UNVERIFIED, NO RECEIPT BONUS.</div>
              <NumberPad value={pad} onKey={(k) => setPad(padPress(pad, k))} />
              <button
                class="btn-blood"
                disabled={padValue(pad) === null}
                onClick={() => {
                  const n = padValue(pad);
                  if (n !== null) net.send({ t: 'INPUT', p: overunderPayload.claim(n) });
                }}
              >
                RELAY THE CLAIM
              </button>
            </div>
          )}
        </div>
      )}
    </main>
  );
}

/** REVEAL: 3-2-1 -> number vs line slam, winners flash, vindicated/exposed banner. */
export function OverUnderReveal({
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
  const rv = asDeckView<OverUnderRevealView>(view.gameView, 'overunder', 'REVEAL');
  const verdict = rv ? ouBanner(rv, nameOf(view, rv.subjectId)) : null;
  const yours = rv ? betResult(rv.youBet, rv) : null;
  const winnerNames = rv?.winners.map((id) => nameOf(view, id)) ?? [];
  return (
    <HoldShell net={net} deadlines={deadlines} heat={heat} epoch={epoch} holdSince={holdSince} isHost={me?.role === 'host'}>
      {rv && (
        <>
          {rv.card && <p class="reveal-prompt">{rv.card.text}</p>}
          {rv.truth !== null && rv.line !== null ? (
            <div class="vs-slam">
              <div class="vs-cell">
                <span class="vs-label">THE LINE</span>
                <span class="vs-num">{rv.line}</span>
              </div>
              <div class="vs-cell">
                <span class="vs-label">THE TRUTH</span>
                <span class="vs-num hot">{rv.truth}</span>
              </div>
            </div>
          ) : null}
          <h1 class="lights">{verdict?.headline}</h1>
          <div class="subject-banner">{verdict?.subjectLine}</div>
          {rv.unverified && <div class="unverified-tag">UNVERIFIED — A SPOKEN NUMBER. NO RECEIPT, NO BONUS.</div>}
          {winnerNames.length > 0 && (
            <div class="heat-banner">CORRECT: {winnerNames.join(', ')} — PAID</div>
          )}
          {yours !== null && (
            <div class="you-chip">
              {yours === 'won' ? `YOU BET ${rv.youBet?.toUpperCase()} — RIGHT` : yours === 'push' ? 'PUSH — EVERYONE +1' : `YOU BET ${rv.youBet?.toUpperCase()} — WRONG`}
            </div>
          )}
        </>
      )}
    </HoldShell>
  );
}
