// POISON PITCH screens — spec 5.6, client half of task D-132. Aligned to the LANDED
// engine module games/poison.ts view() shapes (PITCH/VOTE/REVEAL; DEAL serializes
// nothing — the 4.5 ceremony owns the "A vs B" fight-card announce, sides stay secret
// until PITCH so nobody can pre-argue).
// PITCH: two players get handed OPPOSITE sides of a cursed dilemma and have to SELL it.
//   The pitcher's phone shows THEIR option huge + corner-man venom + [I REST MY CASE]
//   ({t:'REST'}) + [SKIP-'EM] ({t:'SKIPEM'}, once/circle, same fighters/fresh poison).
//   Everyone else watches the fight card and heckles with fire. Timer rides the Ring.
// VOTE: non-pitchers call which PITCH won — A or B ({vote}). At N=3 the lone survivor
//   is a single judge who scorches each pitch 1..5 on a damage meter ({rate:{A,B}}).
//   Pitchers can't vote their own duel — they sweat it out on a waiting screen.
// REVEAL: the meaner pitch in lights + what they championed + the A|B damage spread.
//   A tie splits the pot (both bleed +2); a 0-0 tie is a no-contest (nobody moved).
// LAW (5.6): sides never leak before PITCH; VOTE shows counts + YOUR OWN ballot only;
//   the reveal spread is aggregate weighted counts/ratings — never who cast what. This
//   file renders only what view() handed THIS socket.
//
// NEW CSS (integrator: append to games.css) — the fight-card signature. Over/under's
// slabs sit side by side; a poison duel is two people shoved into OPPOSITE corners, so
// it stacks vertically with a blood "VS" wedged between them — the forced confrontation
// is the whole game, and blood (not the ember action-accent) says these two will bleed.
//   .duel { display: flex; flex-direction: column; gap: 8px; }
//   .duel-vs {
//     align-self: center;
//     font-family: var(--display);
//     font-weight: 900;
//     font-size: 20px;
//     letter-spacing: 0.3em;
//     color: var(--blood);
//   }
import { useState } from 'preact/hooks';
import { asDeckView } from '../games/wire';
import type { Net } from '../net/ws';
import type { PlayerView, RoomView } from '../view';
import { FireButton, Flame, Ring } from './bits';
import { HoldShell } from './hold';
import { InputFallback } from './roast';
import '../style/games.css';

// ===== view types (mirror engine games/poison.ts view() verbatim) =====
type Side = 'A' | 'B';
type DuelWinner = Side | 'tie';

interface PoisonViewBase {
  deck: 'poison';
  loop: number; // 0-based duel index
  loops: number; // 2 duels per circle
}
type PoisonPitchView = PoisonViewBase & {
  sub: 'PITCH';
  optionA: string;
  optionB: string;
  pitcherA: string;
  pitcherB: string;
  youArePitcher: Side | null; // your phone: sell your side, or watch + fire
  deadline: number; // 60s verbal window
};
type PoisonVoteView = PoisonViewBase & {
  sub: 'VOTE';
  optionA: string;
  optionB: string;
  pitcherA: string;
  pitcherB: string;
  ratingMode: boolean; // N=3: one judge scorches each pitch 1..5 instead of A/B
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: Side | null; // your own call (ratingMode: the side you scored higher)
  youRated?: { A: number; B: number }; // the judge's own ballot, echoed back only to them
  deadline: number; // 12s skippable
};
type PoisonRevealView = PoisonViewBase & {
  sub: 'REVEAL';
  winner: DuelWinner;
  pitcherA: string;
  pitcherB: string;
  optionA: string;
  optionB: string;
  spread: { A: number; B: number }; // weighted vote counts, or summed ratings (N=3)
};

const nameOf = (view: RoomView, id: string | null): string =>
  (id !== null ? view.players.find((p) => p.id === id)?.name : undefined) ?? '???';

export function PoisonScreen({
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
  const p = asDeckView<PoisonPitchView>(gv, 'poison', 'PITCH');
  if (p) return <Pitch key={`p${p.loop}`} v={p} view={view} net={net} deadline={deadline ?? p.deadline} heat={heat} />;
  const vt = asDeckView<PoisonVoteView>(gv, 'poison', 'VOTE');
  if (vt) return <Vote key={`v${vt.loop}`} v={vt} view={view} me={me} net={net} deadline={deadline ?? vt.deadline} />;
  return <InputFallback sub="POISON PITCH" deadline={deadline} net={net} />;
}

/**
 * PITCH (60s, verbal). The pitcher sees the side they were DEALT (they didn't pick it)
 * and has to argue it to the death; everyone else gets the fight card and a fire button.
 * [I REST MY CASE] cuts the clock; [SKIP-'EM] swaps the dilemma once per circle.
 */
function Pitch({
  v,
  view,
  net,
  deadline,
  heat,
}: {
  v: PoisonPitchView;
  view: RoomView;
  net: Net;
  deadline: number;
  heat: number;
}) {
  const pitcherAName = nameOf(view, v.pitcherA);
  const pitcherBName = nameOf(view, v.pitcherB);

  if (v.youArePitcher !== null) {
    const mine = v.youArePitcher === 'A' ? v.optionA : v.optionB;
    const theirs = v.youArePitcher === 'A' ? v.optionB : v.optionA;
    const oppName = v.youArePitcher === 'A' ? pitcherBName : pitcherAName;
    return (
      <main class="screen vote">
        <header class="vote-head">
          <span class="vote-tally">
            YOUR CORNER · DUEL {v.loop + 1}/{v.loops}
          </span>
          <Ring deadline={deadline} now={() => net.serverNow()} />
        </header>
        <div class="stat-about">YOU DIDN'T PICK THIS SIDE. DIE ON IT.</div>
        <h1 class="prompt">“{mine}”</h1>
        <div class="you-chip">VS {oppName} — “{theirs}”</div>
        <div class="section-label">CORNER ADVICE</div>
        <p class="gate-note">
          Argue it like your rent's due on it. Nobody cares that you're right — they pay out for LOUD.
          <br />
          Make it personal. Point at someone. The meanest true thing in the room wins the pot.
          <br />
          Hate the hand you drew? SKIP-'EM once and pull a fresh poison — same fight, new venom.
        </p>
        <div class="begin-block">
          <button class="btn-blood big" onClick={() => net.send({ t: 'REST' })}>
            I REST MY CASE
          </button>
          <button class="btn-ghost" onClick={() => net.send({ t: 'SKIPEM' })}>
            SKIP-'EM — DEAL A FRESH POISON
          </button>
        </div>
      </main>
    );
  }

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          DUEL {v.loop + 1}/{v.loops} · THE MEANER ONE WINS
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <div class="duel">
        <div class="faceoff-slab">
          <span class="faceoff-label">{pitcherAName}</span>
          <span class="faceoff-text">“{v.optionA}”</span>
        </div>
        <div class="duel-vs">VS</div>
        <div class="faceoff-slab">
          <span class="faceoff-label">{pitcherBName}</span>
          <span class="faceoff-text">“{v.optionB}”</span>
        </div>
      </div>
      <div class="watch-tag breathe">NO RIGHT ANSWER. ONLY A LOUDER ONE.</div>
      <div class="reveal-actions">
        <FireButton onFire={() => net.fire()} heat={heat} />
      </div>
    </main>
  );
}

/**
 * VOTE (12s skippable). Non-pitchers call which PITCH won. At N=3 the lone non-pitcher
 * is a single judge who scorches each pitch on a 1..5 damage meter. Pitchers can't vote
 * their own duel — they wait. Imps are barred from the N=3 meter (5.6) — they wait too.
 */
function Vote({
  v,
  view,
  me,
  net,
  deadline,
}: {
  v: PoisonVoteView;
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadline: number;
}) {
  const pitcherAName = nameOf(view, v.pitcherA);
  const pitcherBName = nameOf(view, v.pitcherB);
  const youArePitcher: Side | null = view.you === v.pitcherA ? 'A' : view.you === v.pitcherB ? 'B' : null;

  if (youArePitcher !== null) {
    return (
      <main class="screen waiting">
        <div class="waiting-label breathe">THE JURY'S OUT</div>
        <h1 class="lights">
          {v.votedCount}/{v.eligible}
        </h1>
        <p class="game-blurb">
          They're deciding which of you sold it harder. Whatever your face is doing right now — stop it.
        </p>
      </main>
    );
  }

  if (v.ratingMode) {
    if (me?.role === 'imp') {
      return (
        <main class="screen waiting">
          <div class="waiting-label breathe">NOT YOUR CALL</div>
          <h1 class="lights">ONE JUDGE</h1>
          <p class="game-blurb">Three left standing. The lone survivor scores the damage. You just watch it land.</p>
        </main>
      );
    }
    return (
      <DamageJudge v={v} net={net} deadline={deadline} pitcherAName={pitcherAName} pitcherBName={pitcherBName} />
    );
  }

  return <Ballot v={v} net={net} deadline={deadline} pitcherAName={pitcherAName} pitcherBName={pitcherBName} />;
}

/** N=3 damage meter: the single judge scorches each pitch 1..5, then deals both at once. */
function DamageJudge({
  v,
  net,
  deadline,
  pitcherAName,
  pitcherBName,
}: {
  v: PoisonVoteView;
  net: Net;
  deadline: number;
  pitcherAName: string;
  pitcherBName: string;
}) {
  const [rA, setRA] = useState<number | null>(v.youRated?.A ?? null);
  const [rB, setRB] = useState<number | null>(v.youRated?.B ?? null);
  const [dealt, setDealt] = useState<boolean>(v.youRated != null);
  const commit = (): void => {
    if (rA === null || rB === null || dealt) return;
    setDealt(true);
    net.send({ t: 'INPUT', p: { rate: { A: rA, B: rB } } });
  };
  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">YOU ALONE JUDGE THIS · N=3</span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <Meter label={pitcherAName} option={v.optionA} value={rA} onSet={setRA} disabled={dealt} />
      <Meter label={pitcherBName} option={v.optionB} value={rB} onSet={setRB} disabled={dealt} />
      {dealt ? (
        <div class="locked-banner flash-in">
          DAMAGE DEALT — {rA} vs {rB}
        </div>
      ) : (
        <button class="btn-blood big" disabled={rA === null || rB === null} onClick={commit}>
          DEAL THE DAMAGE
        </button>
      )}
    </main>
  );
}

/** One pitch's 1..5 damage meter — flames light left-to-right as you score it. */
function Meter({
  label,
  option,
  value,
  onSet,
  disabled,
}: {
  label: string;
  option: string;
  value: number | null;
  onSet: (n: number) => void;
  disabled: boolean;
}) {
  const lit = value ?? 0;
  return (
    <div class="dial">
      <div class="dial-q">
        {label} — “{option}”
      </div>
      <div class="dial-flames">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            class={n <= lit ? 'dial-flame lit' : 'dial-flame'}
            disabled={disabled}
            onClick={() => onSet(n)}
          >
            <Flame lit={n <= lit} size={20} />
          </button>
        ))}
      </div>
      <div class="dial-note">{value ? `${value}/5 — SCORCHED` : 'TAP TO SCORCH'}</div>
    </div>
  );
}

/** N>=4 ballot: two corners, tap the pitch that sold it. Your call locks on the tap. */
function Ballot({
  v,
  net,
  deadline,
  pitcherAName,
  pitcherBName,
}: {
  v: PoisonVoteView;
  net: Net;
  deadline: number;
  pitcherAName: string;
  pitcherBName: string;
}) {
  const [picked, setPicked] = useState<Side | null>(null);
  const chosen = picked ?? v.youVoted;
  const locked = chosen !== null;
  const vote = (side: Side): void => {
    if (locked) return;
    setPicked(side);
    net.send({ t: 'INPUT', p: { vote: side } });
  };
  const chosenName = chosen === 'A' ? pitcherAName : chosen === 'B' ? pitcherBName : null;
  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {v.votedCount}/{v.eligible} CALLED IT
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <div class="section-label">WHO SOLD IT HARDER?</div>
      <div class={locked ? 'faceoff locked' : 'faceoff'}>
        <button
          class={'faceoff-slab' + (chosen === 'A' ? ' picked' : '')}
          disabled={locked}
          onClick={() => vote('A')}
        >
          <span class="faceoff-label">{pitcherAName}</span>
          <span class="faceoff-text">“{v.optionA}”</span>
        </button>
        <button
          class={'faceoff-slab' + (chosen === 'B' ? ' picked' : '')}
          disabled={locked}
          onClick={() => vote('B')}
        >
          <span class="faceoff-label">{pitcherBName}</span>
          <span class="faceoff-text">“{v.optionB}”</span>
        </button>
      </div>
      {locked && <div class="locked-banner flash-in">CALLED IT — {chosenName} TOOK YOUR VOTE</div>}
    </main>
  );
}

/** REVEAL: 3-2-1 -> the meaner pitch in lights, what they championed, the damage spread. */
export function PoisonReveal({
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
  const rv = asDeckView<PoisonRevealView>(view.gameView, 'poison', 'REVEAL');
  const fmt = (w: number): string => (Number.isInteger(w) ? String(w) : w.toFixed(1));
  const pitcherAName = rv ? nameOf(view, rv.pitcherA) : '???';
  const pitcherBName = rv ? nameOf(view, rv.pitcherB) : '???';
  const noContest = rv !== null && rv.winner === 'tie' && rv.spread.A === 0 && rv.spread.B === 0;
  const split = rv !== null && rv.winner === 'tie' && !noContest;
  const winnerName = rv?.winner === 'A' ? pitcherAName : rv?.winner === 'B' ? pitcherBName : null;
  const winOption = rv?.winner === 'A' ? rv.optionA : rv?.winner === 'B' ? rv.optionB : null;
  const loseOption = rv?.winner === 'A' ? rv?.optionB : rv?.winner === 'B' ? rv?.optionA : null;
  const loserName = rv?.winner === 'A' ? pitcherBName : rv?.winner === 'B' ? pitcherAName : null;
  return (
    <HoldShell net={net} deadlines={deadlines} heat={heat} epoch={epoch} holdSince={holdSince} isHost={me?.role === 'host'}>
      {rv && (
        <>
          {winnerName !== null ? (
            <>
              <div class="section-label">THE MEANER PITCH</div>
              <h1 class="lights">{winnerName}</h1>
              <div class="subject-banner">SOLD “{winOption}”. +3.</div>
              <p class="reveal-prompt">
                BURIED “{loseOption}” — AND {loserName} WITH IT.
              </p>
            </>
          ) : split ? (
            <>
              <h1 class="lights">SPLIT DECISION</h1>
              <div class="subject-banner">
                {pitcherAName} & {pitcherBName} BOTH BLED FOR IT — +2 EACH.
              </div>
            </>
          ) : (
            <>
              <h1 class="lights">NO CONTEST</h1>
              <div class="subject-banner">THE ROOM SAT ON ITS HANDS. NOBODY GOT PAID.</div>
            </>
          )}
          {!noContest && (
            <div class="vs-slam">
              <div class="vs-cell">
                <span class="vs-label">{pitcherAName}</span>
                <span class={rv.winner === 'A' ? 'vs-num hot' : 'vs-num'}>{fmt(rv.spread.A)}</span>
              </div>
              <div class="vs-cell">
                <span class="vs-label">{pitcherBName}</span>
                <span class={rv.winner === 'B' ? 'vs-num hot' : 'vs-num'}>{fmt(rv.spread.B)}</span>
              </div>
            </div>
          )}
        </>
      )}
    </HoldShell>
  );
}
