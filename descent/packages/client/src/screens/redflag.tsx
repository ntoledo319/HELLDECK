// RED FLAG RALLY screens — spec 5.7, client half of task D-133. Aligned to the LANDED
// engine module games/redflag.ts view() shapes (DEFENSE/VOTE/REVEAL; DEAL serializes
// nothing — the ceremony owns the announce).
// DEFENSE: a dating FIGHT CARD. One PERK worth wanting, one RED FLAG worth running from,
//   stacked with a "…BUT" stamp welded across the seam — the GAP is the whole joke. The
//   defender gets 45s to talk the room past the flag ([I REST MY CASE] ends it early,
//   [SKIP 'EM] burns the CARD not the human for a fresh disaster). Everyone else watches
//   and fires.
// VOTE: everyone but the defender swipes SMASH or PASS (two slabs; INPUT {vote}); the
//   defender just sweats a count. 12s. Ballots are secret — you see the tally + your own.
// REVEAL: a slammed SMASH/PASS stamp (the <2s glance) + the fight-card recap + the
//   weighted spread. voided => the date ghosted, no stamp, nobody scores.
// LAW (5.7): who-voted-what NEVER leaves the server — the reveal spread is weighted
//   counts only, and no per-loop score delta rides the view. This file has no other source.
//
// NEW CSS (integrator: append to games.css) — the fight card IS this screen's signature.
// Two facts about ONE fictional date, not two options: the perk pulls toward the fire
// (ember), the flag is the wound (blood), and the "…BUT" is stamped across the seam
// because that hinge is exactly where the bait becomes the trap. Over/Under's stat-card
// or Confession's sin-cards would read as a menu; this reads as a tale of the tape.
//   .fight-card { display: flex; flex-direction: column; gap: 2px; }
//   .fc-side { display: flex; flex-direction: column; justify-content: center; gap: 8px; padding: 18px 16px; background: var(--char); min-height: 88px; }
//   .fc-perk { border-left: 3px solid var(--ember); }
//   .fc-flag { border-left: 3px solid var(--blood); }
//   .fc-tag { font-family: var(--display); font-weight: 800; font-size: 12px; letter-spacing: 0.24em; }
//   .fc-perk .fc-tag { color: var(--ember); }
//   .fc-flag .fc-tag { color: var(--blood); }
//   .fc-line { font-family: var(--display); font-weight: 900; font-size: clamp(19px, 5.4vw, 28px); line-height: 1.12; color: var(--bone); text-transform: none; overflow-wrap: anywhere; }
//   .fc-pivot { align-self: center; position: relative; z-index: 1; margin: -6px 0; padding: 4px 14px 4px 18px; background: var(--blood); color: var(--bone); font-family: var(--display); font-weight: 900; font-size: 14px; letter-spacing: 0.3em; transform: rotate(-2deg); }
//   .fight-card.compact { gap: 2px; }
//   .fight-card.compact .fc-side { min-height: 0; padding: 11px 14px; }
//   .fight-card.compact .fc-line { font-size: 15px; line-height: 1.2; }
//   .stamp-smash { color: var(--ember); border-color: var(--ember); } /* SMASH survives in ember; PASS reuses .stamp-false (blood) */
import { useConnectionOptimistic } from '../connection';
import { asDeckView } from '../games/wire';
import type { Net } from '../net/ws';
import type { PlayerView, RoomView } from '../view';
import { FireButton, Ring } from './bits';
import { HoldShell } from './hold';
import { InputFallback } from './roast';
import '../style/games.css';

// ===== view types (mirror engine games/redflag.ts view() verbatim) =====
interface RedflagViewBase {
  deck: 'redflag';
  sub: string;
  loop: number; // 0-based defender index
  loops: number; // 2 defenses per circle
}
type RedflagDefenseView = RedflagViewBase & {
  sub: 'DEFENSE';
  perk: string;
  flag: string;
  defenderId: string;
  youAreDefender: boolean;
  deadline: number; // 45s verbal sell
};
type RedflagVoteView = RedflagViewBase & {
  sub: 'VOTE';
  perk: string;
  flag: string;
  defenderId: string;
  eligible: number; // everyone but the defender (imps included)
  votedCount: number; // counts only — never who
  youVoted: 'smash' | 'pass' | null; // your own ballot, nobody else's
  deadline: number; // 12s skippable -> auto-abstain
  youAreDefender: boolean;
};
type RedflagRevealView = RedflagViewBase & {
  sub: 'REVEAL';
  verdict: 'SMASH' | 'PASS';
  defenderId: string;
  perk: string;
  flag: string;
  spread: { smash: number; pass: number }; // weighted counts (imps 0.5)
  youVoted: 'smash' | 'pass' | null;
  voided: boolean; // seat-lapse / host kill: ignore the stamp, show the void banner
};

const nameOf = (view: RoomView, id: string | null): string =>
  (id !== null ? view.players.find((p) => p.id === id)?.name : undefined) ?? '???';

/** The tale of the tape — the perk pulls toward the fire, the flag is the wound. */
function FightCard({ perk, flag, compact = false }: { perk: string; flag: string; compact?: boolean }) {
  return (
    <div class={compact ? 'fight-card compact' : 'fight-card'}>
      <div class="fc-side fc-perk">
        <span class="fc-tag">THE PERK</span>
        <p class="fc-line">{perk}</p>
      </div>
      {!compact && <div class="fc-pivot">…BUT</div>}
      <div class="fc-side fc-flag">
        <span class="fc-tag">THE RED FLAG</span>
        <p class="fc-line">{flag}</p>
      </div>
    </div>
  );
}

export function RedflagScreen({
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
  const d = asDeckView<RedflagDefenseView>(gv, 'redflag', 'DEFENSE');
  if (d) return <Defense key={`d${d.loop}`} v={d} view={view} me={me} net={net} deadline={deadline ?? d.deadline} heat={heat} />;
  const vt = asDeckView<RedflagVoteView>(gv, 'redflag', 'VOTE');
  if (vt) return <Vote key={`v${vt.loop}`} v={vt} view={view} me={me} net={net} deadline={deadline ?? vt.deadline} />;
  return <InputFallback sub="RED FLAG RALLY" deadline={deadline} net={net} />;
}

/**
 * DEFENSE (45s, verbal) — the fight card goes up on every phone. The defender sells the
 * date out loud; [I REST MY CASE] ends the clock, [SKIP 'EM] re-deals the card (same
 * human, fresh disaster, once per circle — the engine no-ops a second try). Everyone
 * else heckles and fires; the host can mercy-kill a dying pitch.
 */
function Defense({
  v,
  view,
  me,
  net,
  deadline,
  heat,
}: {
  v: RedflagDefenseView;
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadline: number;
  heat: number;
}) {
  if (v.youAreDefender) {
    return (
      <main class="screen">
        <header class="vote-head">
          <span class="vote-tally">
            DEFENDER {v.loop + 1} OF {v.loops} · SELL THE DATE
          </span>
          <Ring deadline={deadline} now={() => net.serverNow()} />
        </header>
        <div class="stat-about">ONE PERK. ONE FELONY. TALK THE ROOM INTO IT.</div>
        <FightCard perk={v.perk} flag={v.flag} />
        <button class="btn-blood big rest-btn" onClick={() => net.send({ t: 'REST' })}>
          I REST MY CASE
        </button>
        <button class="btn-ghost" onClick={() => net.send({ t: 'SKIPEM' })}>
          SKIP 'EM — DEAL A FRESH DISASTER
        </button>
      </main>
    );
  }
  return (
    <main class="screen">
      <header class="vote-head">
        <span class="vote-tally">{nameOf(view, v.defenderId).toUpperCase()} IS SELLING IT</span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <div class="stat-about">YOU SAW THE FLAG. SMASH ANYWAY?</div>
      <FightCard perk={v.perk} flag={v.flag} />
      <div class="watch-tag breathe">WATCH THEM DEFEND THE INDEFENSIBLE.</div>
      {me?.role === 'host' && (
        <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
          KILL THE DATE (VOID)
        </button>
      )}
      <div class="reveal-actions">
        <FireButton onFire={() => net.fire()} heat={heat} />
      </div>
    </main>
  );
}

/** VOTE (12s skippable): the room swipes SMASH / PASS; the defender sweats a count. */
function Vote({
  v,
  view,
  me,
  net,
  deadline,
}: {
  v: RedflagVoteView;
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadline: number;
}) {
  const [picked, setPicked] = useConnectionOptimistic<'smash' | 'pass' | null>(null);
  const chosen = picked ?? v.youVoted;
  const locked = chosen !== null;
  const vote = (side: 'smash' | 'pass'): void => {
    if (locked) return;
    if (net.send({ t: 'INPUT', p: { vote: side } })) setPicked(side); // engine parseVote reads exactly `vote`
  };

  if (v.youAreDefender) {
    return (
      <main class="screen waiting">
        <header class="vote-head">
          <span class="vote-tally">VERDICT INCOMING</span>
          <Ring deadline={deadline} now={() => net.serverNow()} />
        </header>
        <h1 class="lights">
          {v.votedCount}/{v.eligible}
        </h1>
        <p class="game-blurb">The room is deciding if you're dateable. Nothing you say now counts. Sweat.</p>
      </main>
    );
  }

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {v.votedCount}/{v.eligible} VOTES IN
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <FightCard perk={v.perk} flag={v.flag} compact />
      <div class={locked ? 'bet-slabs locked' : 'bet-slabs'}>
        <button
          type="button"
          class={'bet-slab' + (chosen === 'smash' ? ' picked' : '')}
          disabled={locked}
          aria-pressed={chosen === 'smash'}
          onClick={() => vote('smash')}
        >
          SMASH
        </button>
        <button
          type="button"
          class={'bet-slab' + (chosen === 'pass' ? ' picked' : '')}
          disabled={locked}
          aria-pressed={chosen === 'pass'}
          onClick={() => vote('pass')}
        >
          PASS
        </button>
      </div>
      {locked && <div class="locked-banner flash-in" role="status" aria-live="polite">LOCKED — YOU CHOSE {chosen === 'smash' ? 'SMASH' : 'PASS'}</div>}
      {me?.role === 'host' && (
        <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
          KILL THE DATE (VOID)
        </button>
      )}
    </main>
  );
}

/** REVEAL: 3-2-1 -> SMASH/PASS stamp slam + fight-card recap + weighted spread. */
export function RedflagReveal({
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
  const rv = asDeckView<RedflagRevealView>(view.gameView, 'redflag', 'REVEAL');
  const defenderName = rv ? nameOf(view, rv.defenderId) : '???';
  const smash = rv?.verdict === 'SMASH';
  const fmt = (w: number): string => (Number.isInteger(w) ? String(w) : w.toFixed(1));
  return (
    <HoldShell net={net} deadlines={deadlines} heat={heat} epoch={epoch} holdSince={holdSince} isHost={me?.role === 'host'}>
      {rv &&
        (rv.voided ? (
          <>
            <div class="double-tag">VOID — THEY GHOSTED</div>
            <h1 class="lights">NO VERDICT</h1>
            <div class="subject-banner">THE DATE NEVER HAPPENED. NOBODY SCORES.</div>
          </>
        ) : (
          <>
            <div class={smash ? 'stamp stamp-smash flash-in' : 'stamp stamp-false flash-in'}>{rv.verdict}</div>
            <div class="subject-banner">
              {smash ? `${defenderName} SOLD THE UNSELLABLE.` : `${defenderName} COULDN'T CLOSE.`}
            </div>
            <FightCard perk={rv.perk} flag={rv.flag} compact />
            <div class="jury-spread">
              <div class="jury-cell">
                <span class="vs-label">SMASH</span>
                <span class="vs-num hot">{fmt(rv.spread.smash)}</span>
              </div>
              <div class="jury-cell">
                <span class="vs-label">PASS</span>
                <span class="vs-num">{fmt(rv.spread.pass)}</span>
              </div>
            </div>
            {rv.youVoted !== null && (
              <div class="you-chip">YOU CHOSE {rv.youVoted === 'smash' ? 'SMASH' : 'PASS'}</div>
            )}
          </>
        ))}
    </HoldShell>
  );
}
