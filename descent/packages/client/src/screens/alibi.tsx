// ALIBI DROP screens — spec 5.8, client half of task D-133. Aligned to the LANDED
// engine module games/alibi.ts view() shapes (ALIBI/HUNT/REVEAL; DEAL serializes
// nothing — the 4.5 ceremony owns the private preview + the public announce).
// ALIBI: the accusation goes huge on every phone. The ACCUSED's phone — and ONLY
//   theirs — grows a contraband strip of THREE words they're forced to smuggle into
//   a 30s spoken lie; [I REST MY CASE] ends it early. Everyone else gets a Ring, the
//   accusation, and a FireButton — they can't see the words, they have to HEAR them.
// HUNT: the jury (everyone but the accused) gets an 8-word lineup (three plants mixed
//   with five decoys, order shuffled PER VIEWER by law) and names EXACTLY THREE they
//   think were forced (INPUT {picks:[w,w,w]}). Select/deselect until three; NAME THEM
//   locks. The accused just sweats and watches the ballot count climb.
// REVEAL: a server-timed drumroll. results is the 8 words in a shared stage order with
//   PLANTED/DECOY truth (which exists on the wire HERE and nowhere earlier); beat is how
//   many have flipped. We render 8 face-down slots and turn over exactly `beat` of them,
//   one blood stamp per plant, one struck-out dud per decoy — the view re-broadcasts as
//   each AT beat lands, so the phone just draws up to beat. Your own three (youPicked)
//   glow ember so you see, in real time, whether you called it.
// LAW (5.8): which words are plants is server-only until the flip; HUNT ballots are
//   counts + your OWN three only; the fuse... there is none, but the plant/decoy truth
//   never precedes REVEAL. This file has no other source for any of it.
//
// NEW CSS (integrator: append to games.css) — two bespoke blocks. The contraband strip
// is the signature of the game (three words you cannot refuse to say); a borrowed fill-in
// panic-shelf would read as cowardice, not smuggling. The lineup flip is the trickiest
// reveal in the deck; a reused vote-grid can't carry face-down -> blood-stamp choreography.
//   .contraband { display: flex; flex-direction: column; gap: 8px; border: 1px solid var(--ember); border-left-width: 4px; background: rgba(226, 87, 27, 0.06); padding: 12px 14px; }
//   .contraband-label { font-family: var(--display); font-weight: 800; font-size: 12px; letter-spacing: 0.18em; color: var(--ember); }
//   .contraband-words { display: flex; flex-wrap: wrap; gap: 6px 16px; }
//   .contraband-word { font-family: var(--display); font-weight: 900; font-size: clamp(22px, 6.5vw, 34px); line-height: 1; color: var(--bone); overflow-wrap: anywhere; }
//   .lineup-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
//   .lineup-cell { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 5px; min-height: 74px; padding: 10px 8px; border: 1px solid var(--ash); text-align: center; }
//   .lineup-cell.down { color: var(--ash); }
//   .lineup-cell.plant { border-color: var(--blood); background: rgba(142, 27, 27, 0.32); }
//   .lineup-cell.decoy { opacity: 0.4; }
//   .lineup-cell.mine { box-shadow: inset 0 0 0 2px var(--ember); }
//   .lineup-word { font-family: var(--display); font-weight: 900; font-size: clamp(16px, 4.5vw, 22px); line-height: 1; color: var(--bone); overflow-wrap: anywhere; }
//   .lineup-word.decoy { text-decoration: line-through; color: var(--ash); }
//   .lineup-tag { font-family: var(--display); font-weight: 800; font-size: 10px; letter-spacing: 0.16em; }
//   .lineup-tag.plant { color: var(--blood); }
//   .lineup-tag.decoy { color: var(--ash); }
import { useState } from 'preact/hooks';
import { asDeckView } from '../games/wire';
import type { Net } from '../net/ws';
import type { PlayerView, RoomView } from '../view';
import { FireButton, Ring } from './bits';
import { HoldShell } from './hold';
import { InputFallback } from './roast';
import '../style/games.css';

// ===== view types (mirror engine games/alibi.ts view() verbatim) =====
interface AlibiViewBase {
  deck: 'alibi';
  sub: string;
  loop: number; // 0-based accused index
  loops: number; // 2 accused per circle
}
type AlibiAlibiView = AlibiViewBase & {
  sub: 'ALIBI';
  accusation: string;
  accusedId: string;
  youAreAccused: boolean;
  words?: [string, string, string]; // the contraband — the accused's own frame ONLY
  deadline: number; // 30s
};
type AlibiHuntView = AlibiViewBase & {
  sub: 'HUNT';
  accusation: string;
  accusedId: string;
  lineup: string[]; // 8 words, plants mixed with decoys, order shuffled PER VIEWER
  youPicked: string[] | null; // your own three, nobody else's
  eligible: number;
  pickedCount: number; // counts only — never who, never what
  deadline: number; // 20s
};
type AlibiRevealView = AlibiViewBase & {
  sub: 'REVEAL';
  accusation: string;
  accusedId: string;
  results: { word: string; planted: boolean }[]; // stage order; PLANTED exists HERE and nowhere earlier
  youPicked: string[] | null;
  beat: number; // client flips word N once beat reaches N
  voided: boolean;
};

const nameOf = (view: RoomView, id: string | null): string =>
  (id !== null ? view.players.find((p) => p.id === id)?.name : undefined) ?? '???';

export function AlibiScreen({
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
  const a = asDeckView<AlibiAlibiView>(gv, 'alibi', 'ALIBI');
  if (a) return <Alibi key={`a${a.loop}`} v={a} view={view} me={me} net={net} deadline={deadline ?? a.deadline} heat={heat} />;
  const h = asDeckView<AlibiHuntView>(gv, 'alibi', 'HUNT');
  if (h) return <Hunt key={`h${h.loop}`} v={h} view={view} me={me} net={net} deadline={deadline ?? h.deadline} />;
  return <InputFallback sub="ALIBI DROP" deadline={deadline} net={net} />;
}

/**
 * ALIBI (30s, verbal). The accused's phone pins three contraband words they're forced
 * to work into a spoken lie; the jury can't see them — they have to catch them by ear.
 */
function Alibi({
  v,
  view,
  me,
  net,
  deadline,
  heat,
}: {
  v: AlibiAlibiView;
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadline: number;
  heat: number;
}) {
  if (v.youAreAccused && v.words) {
    return (
      <main class="screen vote">
        <header class="vote-head">
          <span class="vote-tally">
            ACCUSED {v.loop + 1} OF {v.loops} · LIE WITH YOUR WHOLE CHEST
          </span>
          <Ring deadline={deadline} now={() => net.serverNow()} />
        </header>
        <div class="contraband">
          <span class="contraband-label">SMUGGLE ALL THREE IN · NO ONE ELSE CAN SEE THEM</span>
          <div class="contraband-words">
            {v.words.map((w) => (
              <span key={w} class="contraband-word">
                {w}
              </span>
            ))}
          </div>
        </div>
        <h1 class="prompt">{v.accusation}</h1>
        <button class="btn-blood big rest-btn" onClick={() => net.send({ t: 'REST' })}>
          I REST MY CASE
        </button>
      </main>
    );
  }

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          ACCUSED {v.loop + 1} OF {v.loops} · THREE WORDS ARE HIDING IN THIS
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <div class="stat-about">{nameOf(view, v.accusedId)} IS LYING TO YOUR FACE</div>
      <h1 class="prompt">{v.accusation}</h1>
      <div class="watch-tag breathe">THEY CAN'T STOP THE WORDS. YOU CAN HEAR THEM.</div>
      <div class="reveal-actions">
        <FireButton onFire={() => net.fire()} heat={heat} />
        {me?.role === 'host' && (
          <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
            VOID ROUND
          </button>
        )}
      </div>
    </main>
  );
}

/**
 * HUNT (20s skippable). The jury names EXACTLY three plants out of an 8-word lineup;
 * select/deselect until three, NAME THEM locks. The accused watches the count climb.
 */
function Hunt({
  v,
  view,
  me,
  net,
  deadline,
}: {
  v: AlibiHuntView;
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  deadline: number;
}) {
  const [sel, setSel] = useState<string[]>(v.youPicked ?? []);
  const [sent, setSent] = useState(v.youPicked !== null);
  const locked = sent || v.youPicked !== null;
  const chosen = v.youPicked ?? sel;

  // The accused doesn't hunt (engine drops their ballot) — they sweat the count.
  if (view.you === v.accusedId) {
    return (
      <main class="screen waiting">
        <div class="waiting-label breathe">THE JURY DELIBERATES</div>
        <h1 class="lights">
          {v.pickedCount}/{v.eligible}
        </h1>
        <p class="game-blurb">They're picking your story apart word by word. Whatever your face is doing — stop it.</p>
      </main>
    );
  }

  const toggle = (w: string): void => {
    if (locked) return;
    setSel((cur) => (cur.includes(w) ? cur.filter((x) => x !== w) : cur.length < 3 ? [...cur, w] : cur));
  };
  const commit = (): void => {
    if (locked || sel.length !== 3) return;
    setSent(true);
    net.send({ t: 'INPUT', p: { picks: sel } }); // engine parsePicks reads exactly `picks` — three distinct
  };

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {v.pickedCount}/{v.eligible} BALLOTS IN
        </span>
        <Ring deadline={deadline} now={() => net.serverNow()} />
      </header>
      <div class="stat-about">NAME THE THREE {nameOf(view, v.accusedId)} WAS FORCED TO SAY</div>
      <h1 class="prompt">{v.accusation}</h1>
      <div class={locked ? 'ballot locked' : 'ballot'}>
        {v.lineup.map((w) => (
          <button
            key={w}
            class={'ballot-row' + (chosen.includes(w) ? ' picked' : '')}
            disabled={locked || (!chosen.includes(w) && chosen.length >= 3)}
            onClick={() => toggle(w)}
          >
            <span>{w}</span>
            {chosen.includes(w) && <span class="ballot-tag">PLANT?</span>}
          </button>
        ))}
      </div>
      {!locked ? (
        <button class="btn-blood big" disabled={sel.length !== 3} onClick={commit}>
          {sel.length === 3 ? 'NAME THEM' : `PICK ${3 - sel.length} MORE`}
        </button>
      ) : (
        <div class="locked-banner flash-in">BALLOT CAST — THREE ACCUSATIONS, NO TAKEBACKS</div>
      )}
      {!locked && me?.role === 'host' && (
        <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
          VOID ROUND
        </button>
      )}
    </main>
  );
}

/** REVEAL: 8 face-down slots turn over one blood-stamp / struck-decoy per server beat. */
export function AlibiReveal({
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
  const rv = asDeckView<AlibiRevealView>(view.gameView, 'alibi', 'REVEAL');
  const results = rv?.results ?? [];
  const beat = rv?.beat ?? 0;
  const picked = rv?.youPicked ?? null;
  const allFlipped = rv !== null && beat >= results.length;
  const plants = results.filter((r) => r.planted).length;
  const caught = results.filter((r) => r.planted && picked?.includes(r.word)).length;
  const isAccused = rv !== null && view.you === rv.accusedId;

  return (
    <HoldShell net={net} deadlines={deadlines} heat={heat} epoch={epoch} holdSince={holdSince} isHost={me?.role === 'host'}>
      {rv && (
        <>
          <p class="reveal-prompt">{rv.accusation}</p>
          {rv.voided ? (
            <>
              <h1 class="lights">MISTRIAL</h1>
              <div class="subject-banner">THE CASE FELL APART. NOBODY SCORES, NOBODY WALKS CLEAN.</div>
            </>
          ) : (
            <>
              <div class="section-label">{nameOf(view, rv.accusedId).toUpperCase()}'S STORY — WORD BY WORD</div>
              <div class="lineup-grid">
                {results.map((r, i) => {
                  const shown = i < beat;
                  const mine = picked?.includes(r.word) ?? false;
                  const cls = shown ? (r.planted ? 'plant' : 'decoy') : 'down';
                  return (
                    <div key={r.word} class={`lineup-cell ${cls}${shown && mine ? ' mine' : ''}`}>
                      {shown ? (
                        <>
                          <span class={r.planted ? 'lineup-word' : 'lineup-word decoy'}>{r.word}</span>
                          <span class={r.planted ? 'lineup-tag plant' : 'lineup-tag decoy'}>
                            {r.planted ? 'PLANTED' : 'DECOY'}
                          </span>
                        </>
                      ) : (
                        <span class="lineup-word">▮▮▮</span>
                      )}
                    </div>
                  );
                })}
              </div>
              {allFlipped && (
                <div class="you-chip">
                  {isAccused
                    ? `THREE LIES SMUGGLED. HOW MANY DID THEY SWALLOW?`
                    : picked !== null
                      ? caught === plants
                        ? `YOU CAUGHT ALL ${plants} — NOTHING GOT PAST YOU`
                        : caught === 0
                          ? `YOU CAUGHT NOTHING. THEY LIED CLEAN.`
                          : `YOU CAUGHT ${caught} OF ${plants}`
                      : `YOU NAMED NOTHING. THE JURY NOTICED.`}
                </div>
              )}
            </>
          )}
        </>
      )}
    </HoldShell>
  );
}
