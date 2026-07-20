// SCATTERBLAST screens — spec 5.5, client half of task D-131. Aligned to the LANDED
// engine module games/scatter.ts view() shapes (BOMB/BOOM/REVEAL; DEAL serializes
// nothing — the ceremony owns the announce).
// BOMB: category + one LETTER go huge on every phone; the room shouts answers and
//   passes a bomb whose fuse is HIDDEN by law (no deadline ever hits the wire — that
//   silence IS the tension). No input; the turn passes by voice.
// BOOM: the fuse blew. Every phone flashes WHO DIED? and the room fingers whoever was
//   holding it — a tappable grid (INPUT {tap}), 5s, your own finger locks (youTapped).
//   Self-tap is LEGAL: "who was holding it" can honestly be you.
// REVEAL: the corpse's name in lights + THE TABLE HAS SPOKEN, or a dud-bomb banner
//   when nobody moved a finger (loser === null). The tap MAP never serializes — only
//   counts-only weighted bars (imps at 0.5) reach the wire.
// LAW (5.5): the fuse length appears NOWHERE on the wire; a viewer at BOOM sees the
//   running count + their OWN tap and nothing else. This file has no other source.
//
// NEW CSS (integrator: append to games.css) — one class, the screen's signature glyph.
// A single hellfire-glowing constraint letter reading as a lit bomb on the table; the
// over/under .dial-face (bone, no glow) would make BOMB generic within the app.
//   .bomb-letter {
//     font-family: var(--display);
//     font-weight: 900;
//     font-size: clamp(160px, 52vw, 360px);
//     line-height: 0.78;
//     text-align: center;
//     color: var(--bone);
//     margin: auto 0;
//     text-shadow:
//       0 0 20px rgba(226, 87, 27, 0.95),
//       0 0 70px rgba(226, 87, 27, 0.55),
//       0 0 130px rgba(142, 27, 27, 0.6);
//     animation: ember-breathe 2.6s ease-in-out infinite; /* keyframe lives in style.css */
//   }
import { useState } from 'preact/hooks';
import { asDeckView } from '../games/wire';
import type { Net } from '../net/ws';
import type { PlayerView, RoomView } from '../view';
import { Devil, Ring } from './bits';
import { HoldShell } from './hold';
import { InputFallback } from './roast';
import '../style/games.css';

// ===== view types (mirror engine games/scatter.ts view() verbatim) =====
interface ScatterViewBase {
  deck: 'scatter';
  sub: string;
  loop: number; // 0-based bomb index
  loops: number; // 3 per circle (best of three bombs)
  category: string; // huge on every phone
  letter: string; // the single-letter constraint
}
type ScatterBombView = ScatterViewBase & {
  sub: 'BOMB'; // deliberately deadline-free: the fuse is hidden by law
};
type ScatterBoomView = ScatterViewBase & {
  sub: 'BOOM';
  deadline: number; // 5s WHO DIED window
  eligible: number; // players + imps
  tappedCount: number; // counts only — never who
  youTapped: string | null; // your own finger, nobody else's
};
type ScatterRevealView = ScatterViewBase & {
  sub: 'REVEAL';
  loser: string | null; // null = dud bomb / host-voided: no corpse
  spread?: { target: string; weight: number }[]; // anonymous weighted bars; absent on a dud
};

const nameOf = (view: RoomView, id: string | null): string =>
  (id !== null ? view.players.find((p) => p.id === id)?.name : undefined) ?? '???';

export function ScatterScreen({
  view,
  me,
  net,
  deadline,
}: {
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadline: number | null;
  heat: number; // BOMB/BOOM take no fire — the reveal's HoldShell owns it
}) {
  const gv = view.gameView;
  const bomb = asDeckView<ScatterBombView>(gv, 'scatter', 'BOMB');
  if (bomb) return <Bomb key={`bomb${bomb.loop}`} v={bomb} me={me} net={net} />;
  const boom = asDeckView<ScatterBoomView>(gv, 'scatter', 'BOOM');
  if (boom) return <Boom key={`boom${boom.loop}`} v={boom} view={view} me={me} net={net} deadline={deadline ?? boom.deadline} />;
  return <InputFallback sub="SCATTERBLAST" deadline={deadline} net={net} />;
}

/**
 * BOMB — the constraint goes huge; the room shouts answers and passes a bomb by voice.
 * No countdown renders BY DESIGN: the fuse is a hidden seeded draw, and that silence is
 * the whole game. The app tracks nothing here; it only waits for the bang. No input.
 */
function Bomb({ v, me, net }: { v: ScatterBombView; me: PlayerView | null; net: Net }) {
  return (
    <main class="screen">
      <header class="vote-head">
        <span class="vote-tally">
          BOMB {v.loop + 1} OF {v.loops} · ANSWER · PASS · DON'T BE HOLDING IT
        </span>
      </header>
      <h1 class="prompt">{v.category}</h1>
      <div class="stat-about">— EVERY ANSWER STARTS WITH —</div>
      <div class="bomb-letter">{v.letter}</div>
      <div class="watch-tag breathe">THE FUSE IS LIT. IT WON'T TELL YOU WHEN.</div>
      {me?.role === 'host' && (
        <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
          DEFUSE IT (KILL THE BOMB)
        </button>
      )}
    </main>
  );
}

/**
 * BOOM — the fuse blew. WHO DIED flashes and the room fingers the holder. Self-tap is
 * legal (ratting yourself out is honest). Counts tick up; who-tapped-who never leaves
 * the server. First finger locks the phone; 5s deadline decides on whatever's in.
 */
function Boom({
  v,
  view,
  me,
  net,
  deadline,
}: {
  v: ScatterBoomView;
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadline: number;
}) {
  const [picked, setPicked] = useState<string | null>(null);
  const chosen = picked ?? v.youTapped;
  const locked = chosen !== null;
  const tap = (id: string): void => {
    if (locked) return;
    setPicked(id);
    net.send({ t: 'INPUT', p: { tap: id } }); // engine parseTap reads exactly `tap`
  };

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {v.tappedCount}/{v.eligible} FINGERS UP
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <h1 class="lights">WHO DIED?</h1>
      <div class={locked ? 'vote-grid locked' : 'vote-grid'}>
        {view.players.map((p) => (
          <button
            key={p.id}
            class={'vote-cell' + (chosen === p.id ? ' picked' : '')}
            disabled={locked}
            onClick={() => tap(p.id)}
          >
            <Devil n={p.avatar} size={36} />
            <span>{p.id === view.you ? `${p.name} (YOU)` : p.name}</span>
          </button>
        ))}
      </div>
      {locked && (
        <div class="locked-banner flash-in">
          {chosen === view.you ? 'YOU RATTED YOURSELF OUT' : `YOU FINGERED ${nameOf(view, chosen)}`}
        </div>
      )}
      {!locked && me?.role === 'host' && (
        <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
          VOID THIS BOMB
        </button>
      )}
    </main>
  );
}

/** REVEAL: 3-2-1 -> the corpse's name in lights + THE TABLE HAS SPOKEN, or a dud banner. */
export function ScatterReveal({
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
  const rv = asDeckView<ScatterRevealView>(view.gameView, 'scatter', 'REVEAL');
  const spread = rv?.spread ?? [];
  const maxWeight = Math.max(1, ...spread.map((s) => s.weight));
  const fmt = (w: number): string => (Number.isInteger(w) ? String(w) : w.toFixed(1));
  const dud = rv !== null && rv.loser === null;
  return (
    <HoldShell net={net} deadlines={deadlines} heat={heat} epoch={epoch} holdSince={holdSince} isHost={me?.role === 'host'}>
      {rv && (
        <>
          <p class="reveal-prompt">
            {rv.category} — on the letter “{rv.letter}”
          </p>
          {dud ? (
            <>
              <h1 class="lights">DUD BOMB</h1>
              <div class="subject-banner">NOT ONE FINGER MOVED. THE ROOM BLINKED.</div>
            </>
          ) : (
            <>
              <div class="section-label">THE TABLE HAS SPOKEN</div>
              <h1 class="lights">{nameOf(view, rv.loser)}</h1>
              <div class="subject-banner">WAS HOLDING IT WHEN IT WENT OFF.</div>
            </>
          )}
          {spread.length > 0 && (
            <div class="spread">
              {spread.map((s) => (
                <div class="spread-row" key={s.target}>
                  <span class="spread-name">{nameOf(view, s.target)}</span>
                  <span class="spread-bar" style={`width:${(s.weight / maxWeight) * 100}%`} />
                  <span class="spread-n">{fmt(s.weight)}</span>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </HoldShell>
  );
}
