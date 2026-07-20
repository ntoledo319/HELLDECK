// TITLE FIGHT screens — spec 5.9, client half of task D-133 (the arc's second
// PHYSICAL spike). Aligned field-for-field to the landed engine module
// games/titlefight.ts view() shapes (BOUT / VOTE / REVEAL).
//
// The law this file honors: color is FIGHTER IDENTITY, not decoration. HELLDECK
// has no red-corner/blue-corner — it has two fires. fighterA is the BLOOD corner,
// fighterB is the EMBER corner, and that reading holds from the bell (BOUT) through
// the count (VOTE) to the belt (REVEAL). BOUT is a bell-clanging brawl: fighters
// perform IRL and can throw the towel ([REST]); everyone else is a crowd meter
// screaming through the fire button. VOTE: the crowd POINTS at a corner
// ({t:'INPUT', p:{vote: fighterId}}) — fighters can't vote themselves out, they
// just stand there and take the verdict. The host can wave off a duel gone wrong
// ({t:'VOID'} — engine kill-switch 4.7). REVEAL: the belt, in lights, or a SPLIT
// DECISION where nobody loses. The vote map is redacted server-side — this file
// only ever sees counts, its own ballot, and the anonymous spread.
//
// NEW CSS (integrator: append to games.css):
//   /* TITLE FIGHT — the tale of the tape (spec 5.9). Two fires, differently hot. */
//   .corners { display: grid; grid-template-columns: 1fr auto 1fr; align-items: stretch; gap: 8px; }
//   .corner {
//     display: flex; flex-direction: column; align-items: center; justify-content: center;
//     gap: 6px; padding: 18px 10px; min-height: 128px; border: 1px solid var(--ash); text-align: center;
//   }
//   .corner-a { border-top: 4px solid var(--blood); }
//   .corner-b { border-top: 4px solid var(--ember); }
//   .corner-kicker { font-family: var(--display); font-weight: 700; font-size: 11px; letter-spacing: 0.2em; color: var(--ash); }
//   .corner-a .corner-kicker { color: var(--blood); }
//   .corner-b .corner-kicker { color: var(--ember); }
//   .corner-name { font-family: var(--display); font-weight: 900; font-size: clamp(22px, 7vw, 38px); line-height: 0.95; color: var(--bone); text-transform: uppercase; overflow-wrap: anywhere; }
//   .corner.you { background: var(--char); }
//   .corner-a.picked { border-color: var(--blood); background: rgba(142, 27, 27, 0.16); }
//   .corner-b.picked { border-color: var(--ember); background: rgba(226, 87, 27, 0.14); }
//   .corners.locked .corner:not(.picked) { opacity: 0.4; }
//   .vs-bolt { align-self: center; font-family: var(--display); font-weight: 900; font-size: clamp(20px, 6vw, 30px); letter-spacing: 0.04em; color: var(--ash); }
import { useState } from 'preact/hooks';
import { asDeckView } from '../games/wire';
import type { Net } from '../net/ws';
import type { PlayerView, RoomView } from '../view';
import { FireButton, Ring } from './bits';
import { HoldShell } from './hold';
import { InputFallback } from './roast';
import '../style/games.css';

// ===== view types (mirror games/titlefight.ts view() exactly) =====
type Duel = { id: string; text: string };

interface TitleFightBoutView {
  deck: 'titlefight';
  sub: 'BOUT';
  duel: Duel;
  fighterA: string;
  fighterB: string;
  youAreFighter: boolean;
  deadline: number;
  loop: number;
  loops: number;
}

interface TitleFightVoteView {
  deck: 'titlefight';
  sub: 'VOTE';
  duel: Duel;
  fighterA: string;
  fighterB: string;
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: string | null; // your own ballot, nobody else's
  youAreFighter: boolean;
  deadline: number;
}

interface TitleFightRevealView {
  deck: 'titlefight';
  sub: 'REVEAL';
  duel: Duel | null;
  winners: string[]; // 0 = no contest, 1 = clean belt, 2 = SPLIT DECISION
  fighterA: string;
  fighterB: string;
  splitDecision: boolean;
  spread: { fighterId: string; weight: number }[]; // both corners, desc, anonymous
  youVoted: string | null;
}

const nameOf = (view: RoomView, id: string | null): string =>
  (id !== null ? view.players.find((p) => p.id === id)?.name : undefined) ?? '???';
const fmt = (w: number): string => (Number.isInteger(w) ? String(w) : w.toFixed(1));

/**
 * The tale of the tape — reused across BOUT (inert, self-highlighted) and VOTE
 * (tap targets). fighterA is always the blood corner, fighterB the ember corner.
 */
function Corners({
  view,
  you,
  a,
  b,
  chosen,
  onPick,
}: {
  view: RoomView;
  you: string;
  a: string;
  b: string;
  chosen?: string | null;
  onPick?: (id: string) => void;
}) {
  const locked = chosen != null;
  const cell = (id: string, side: 'a' | 'b') => {
    const isYou = id === you;
    const kicker = isYou ? "THAT'S YOU" : side === 'a' ? 'THIS CORNER' : 'THAT CORNER';
    const cls =
      `corner corner-${side}` + (isYou ? ' you' : '') + (chosen === id ? ' picked' : '');
    const inner = (
      <>
        <span class="corner-kicker">{kicker}</span>
        <span class="corner-name">{nameOf(view, id)}</span>
      </>
    );
    return onPick ? (
      <button key={id} class={cls} disabled={locked} onClick={() => onPick(id)}>
        {inner}
      </button>
    ) : (
      <div key={id} class={cls}>
        {inner}
      </div>
    );
  };
  return (
    <div class={locked ? 'corners locked' : 'corners'}>
      {cell(a, 'a')}
      <span class="vs-bolt">VS</span>
      {cell(b, 'b')}
    </div>
  );
}

export function TitleFightScreen({
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
  const you = me?.id ?? view.you;
  const b = asDeckView<TitleFightBoutView>(gv, 'titlefight', 'BOUT');
  if (b) return <Bout key={`b${b.loop}`} v={b} view={view} me={me} you={you} net={net} deadline={deadline ?? b.deadline} heat={heat} />;
  const vo = asDeckView<TitleFightVoteView>(gv, 'titlefight', 'VOTE');
  if (vo) return <Vote key={`v${vo.duel.id}`} v={vo} view={view} me={me} you={you} net={net} deadline={deadline ?? vo.deadline} />;
  return <InputFallback sub="TITLE FIGHT" deadline={deadline} net={net} />;
}

/** BOUT (30s): the bell's rung. Fighters brawl IRL; the room screams through the fire. */
function Bout({
  v,
  view,
  me,
  you,
  net,
  deadline,
  heat,
}: {
  v: TitleFightBoutView;
  view: RoomView;
  me: PlayerView | null;
  you: string;
  net: Net;
  deadline: number;
  heat: number;
}) {
  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {v.loops > 1 ? `BOUT ${v.loop + 1}/${v.loops} · ` : ''}BELL'S RUNG · 30 SECONDS
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <div class="stat-about">THE DUEL</div>
      <h1 class="prompt">{v.duel.text}</h1>
      <Corners view={view} you={you} a={v.fighterA} b={v.fighterB} />
      {v.youAreFighter ? (
        <>
          <p class="gate-note">Sell it harder than the situation deserves. The room's already deciding.</p>
          <button class="btn-blood big rest-btn" onClick={() => net.send({ t: 'REST' })}>
            I REST MY CASE
          </button>
        </>
      ) : (
        <>
          <div class="watch-tag breathe">SCREAM FOR A CORNER.</div>
          <div class="reveal-actions">
            <FireButton onFire={() => net.fire()} heat={heat} />
          </div>
          {me?.role === 'host' && (
            <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
              STOP THE FIGHT
            </button>
          )}
        </>
      )}
    </main>
  );
}

/** VOTE (15s): the crowd points at a corner. Fighters can't vote — they get judged. */
function Vote({
  v,
  view,
  me,
  you,
  net,
  deadline,
}: {
  v: TitleFightVoteView;
  view: RoomView;
  me: PlayerView | null;
  you: string;
  net: Net;
  deadline: number;
}) {
  const [picked, setPicked] = useState<string | null>(null);
  const chosen = picked ?? v.youVoted;

  if (v.youAreFighter) {
    return (
      <main class="screen waiting">
        <div class="waiting-label breathe">THE VERDICT</div>
        <h1 class="lights">
          {v.votedCount}/{v.eligible}
        </h1>
        <p class="game-blurb">Fingers are going up. You don't get a vote on your own beating — stand there and let them point.</p>
      </main>
    );
  }

  const pick = (id: string): void => {
    if (chosen != null) return;
    setPicked(id);
    net.send({ t: 'INPUT', p: { vote: id } });
  };

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {v.votedCount}/{v.eligible} POINTED
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <h1 class="prompt">{v.duel.text}</h1>
      <div class="section-label">POINT AT THE WINNER</div>
      <Corners view={view} you={you} a={v.fighterA} b={v.fighterB} chosen={chosen} onPick={pick} />
      {chosen == null && me?.role === 'host' && (
        <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
          WAVE THE BOUT OFF
        </button>
      )}
      {chosen != null && (
        <div class="locked-banner flash-in">POINTED — {nameOf(view, chosen)} TAKES YOUR VOTE</div>
      )}
    </main>
  );
}

/** REVEAL: the belt in lights (or SPLIT DECISION — nobody loses), then the spread. */
export function TitleFightReveal({
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
  const rv = asDeckView<TitleFightRevealView>(view.gameView, 'titlefight', 'REVEAL');
  const you = me?.id ?? view.you;

  // Verdict, glanceable in <2s: a belt-holder's name, or SPLIT, or NO CONTEST.
  const split = rv?.splitDecision ?? false;
  const winners = rv?.winners ?? [];
  const headline = !rv
    ? ''
    : split
      ? 'SPLIT DECISION'
      : winners.length === 1
        ? nameOf(view, winners[0] ?? null)
        : 'NO CONTEST';
  const banner = !rv
    ? ''
    : split
      ? `${nameOf(view, rv.fighterA)} & ${nameOf(view, rv.fighterB)} — NOBODY EATS DIRT`
      : winners.length === 1
        ? 'TAKES THE BELT'
        : 'THE BELT GOES BACK ON THE WALL';

  // Your personal beat: a fighter reads their fate, a voter learns if they called it.
  const chip = ((): string | null => {
    if (!rv) return null;
    const iFought = you === rv.fighterA || you === rv.fighterB;
    if (iFought) {
      if (winners.length === 0) return null;
      return winners.includes(you) ? "THE BELT'S YOURS." : 'COOKED. WALK IT OFF.';
    }
    if (rv.youVoted == null) return null;
    return winners.includes(rv.youVoted) ? 'YOU CALLED IT' : 'WRONG CORNER, TRAITOR';
  })();

  const maxW = rv ? Math.max(1, ...rv.spread.map((s) => s.weight)) : 1;

  return (
    <HoldShell net={net} deadlines={deadlines} heat={heat} epoch={epoch} holdSince={holdSince} isHost={me?.role === 'host'}>
      {rv && (
        <>
          {rv.duel && <p class="reveal-prompt">{rv.duel.text}</p>}
          <h1 class="lights">{headline}</h1>
          <div class="subject-banner">{banner}</div>
          {winners.length > 0 && (
            <div class="spread">
              {rv.spread.map((s) => (
                <div class="spread-row" key={s.fighterId}>
                  <span class="spread-name">{nameOf(view, s.fighterId)}</span>
                  <i class="spread-bar" style={`width:${Math.max(6, (s.weight / maxW) * 100)}%`} />
                  <span class="spread-n">{fmt(s.weight)}</span>
                </div>
              ))}
            </div>
          )}
          {chip !== null && <div class="you-chip">{chip}</div>}
        </>
      )}
    </HoldShell>
  );
}
