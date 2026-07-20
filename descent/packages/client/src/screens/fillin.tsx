// FILL-IN FINISHER screens — spec 5.2, client half of task D-121.
// WRITE (writers type filth / Reader picks a tone) -> PERFORM (teleprompter on the
// performing phone ONLY; everyone else gets "LISTEN UP") -> VOTE (memory-aid ballot,
// randomized per phone server-side, own line dead) -> REVEAL (the line + author in lights).
// LAW (5.2 acceptance): answer text renders ONLY where view() put it — this file never
// invents a place to show teleprompter/assignment/ballot texts the server didn't hand this socket.
import { useState } from 'preact/hooks';
import { answerReady, charsLeft, clampAnswer, performProgress } from '../games/logic';
import {
  asDeckView,
  FILLIN_READ_TONES,
  fillinPayload,
  type FillinPerformView,
  type FillinRevealView,
  type FillinVoteView,
  type FillinWriteView,
} from '../games/wire';
import type { Net } from '../net/ws';
import type { PlayerView, RoomView } from '../view';
import { FireButton, Ring } from './bits';
import { HoldShell } from './hold';
import { InputFallback } from './roast';
import '../style/games.css';

const nameOf = (view: RoomView, id: string | null): string =>
  (id !== null ? view.players.find((p) => p.id === id)?.name : undefined) ?? '???';

const toneLabel = (tone: number | null): string | null => (tone !== null ? (FILLIN_READ_TONES[tone] ?? null) : null);

/** Reader-paced runs report a 1-based line cursor; derange/face-off report a reads-done count. */
const progressLabel = (p: FillinPerformView): string => {
  const readerPaced = p.stage !== 'faceoff' && p.mode !== 'derange';
  return readerPaced
    ? performProgress(p.progress.pos - 1, p.progress.total)
    : performProgress(p.progress.pos, p.progress.total);
};

export function FillinScreen({
  view,
  net,
  deadline,
  heat,
}: {
  view: RoomView;
  net: Net;
  deadline: number | null;
  heat: number;
}) {
  const gv = view.gameView;
  const w = asDeckView<FillinWriteView>(gv, 'fillin', 'WRITE');
  if (w) {
    if (w.you.isReader) return <TonePick key="tone" v={w} me={view.you} net={net} deadline={deadline} />;
    if (w.you.setup !== null) return <Write key={`w${w.you.setup}`} v={w} net={net} deadline={deadline} />;
    const submitted = w.setups.reduce((n, s) => n + s.submitted, 0);
    const writers = w.setups.reduce((n, s) => n + s.writers, 0);
    return (
      <main class="screen waiting">
        <div class="waiting-label breathe">PENS OUT</div>
        <h1 class="lights">
          {submitted}/{writers}
        </h1>
        <p class="game-blurb">The writers are sharpening. You just have to live with what comes out.</p>
      </main>
    );
  }
  const p = asDeckView<FillinPerformView>(gv, 'fillin', 'PERFORM');
  if (p) {
    if (p.teleprompter !== null) return <Teleprompter key={`p${p.stage}${p.progress.pos}`} v={p} view={view} net={net} />;
    if (p.assignment !== null) return <DerangePerform key="derange" v={p} net={net} />;
    return <ListenUp v={p} view={view} net={net} heat={heat} />;
  }
  const b = asDeckView<FillinVoteView>(gv, 'fillin', 'VOTE');
  if (b) return <Ballot key={`v${b.stage}`} v={b} net={net} deadline={deadline} />;
  return <InputFallback sub="FILL-IN" deadline={deadline} net={net} />;
}

/** Writer: setup + 140-char field + PANIC shelf (two curated fallbacks, half points). */
function Write({ v, net, deadline }: { v: FillinWriteView; net: Net; deadline: number | null }) {
  const [text, setText] = useState('');
  const [sent, setSent] = useState(false);
  const [shelfTaken, setShelfTaken] = useState(false);
  const setup = v.you.setup !== null ? v.setups[v.you.setup] : undefined;
  if (!setup || v.you.setup === null) return null; // half-built loop: never invent a prompt
  const po = v.you.panicOptions;
  const submitted = sent || v.you.yourAnswer !== null;
  const onShelf = shelfTaken || v.you.yourAnswerPanic;
  const left = charsLeft(text);

  const submit = (): void => {
    const line = clampAnswer(text).trim();
    if (line.length === 0) return;
    net.send({ t: 'INPUT', p: fillinPayload.answer(line) });
    setSent(true);
    setShelfTaken(false); // a real line beats the shelf
  };
  const grabShelf = (opt: 'A' | 'B'): void => {
    net.send({ t: 'INPUT', p: fillinPayload.panicTake(opt) });
    setShelfTaken(true);
    setSent(true);
  };

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {setup.submitted}/{setup.writers} IN
          {v.setups.length > 1 ? ` · SETUP ${v.you.setup + 1}/${v.setups.length}` : ''}
        </span>
        {deadline !== null && <Ring deadline={deadline} now={() => net.serverNow()} />}
      </header>
      <h1 class="prompt">{setup.card.text}</h1>
      <textarea
        class="writebox"
        rows={3}
        maxLength={200 /* soft; clampAnswer is the law */}
        placeholder="finish it. make it hurt."
        value={text}
        onInput={(e) => setText(clampAnswer((e.target as HTMLTextAreaElement).value))}
      />
      <div class={left <= 20 ? 'chars-left low' : 'chars-left'}>{left}</div>
      <button class="btn-blood big" disabled={!answerReady(text)} onClick={submit}>
        {submitted ? 'OVERWRITE IT' : 'COMMIT IT'}
      </button>
      {submitted && (
        <div class="locked-banner flash-in">
          {onShelf ? 'SHELF LINE TAKEN — HALF CREDIT. COWARDICE HAS A PRICE.' : "IT'S IN. THE READER OWNS IT NOW."}
        </div>
      )}
      {po === null && !onShelf && (
        <button class="panic-btn" onClick={() => net.send({ t: 'INPUT', p: fillinPayload.panicOpen() })}>
          PANIC
        </button>
      )}
      {po !== null && !shelfTaken && (
        <div class="panic-shelf flash-in">
          <div class="panic-label">THE SHELF — half points, zero dignity</div>
          <button class="panic-option" onClick={() => grabShelf('A')}>
            {po.a}
          </button>
          <button class="panic-option" onClick={() => grabShelf('B')}>
            {po.b}
          </button>
        </div>
      )}
    </main>
  );
}

/** Reader (during WRITE): pick the read tone while the writers sweat. */
function TonePick({
  v,
  me,
  net,
  deadline,
}: {
  v: FillinWriteView;
  me: string;
  net: Net;
  deadline: number | null;
}) {
  const [picked, setPicked] = useState<number | null>(v.you.yourTone);
  const idx = v.setups.findIndex((s) => s.readerId === me);
  const setup = idx >= 0 ? v.setups[idx] : v.setups[0];
  const pick = (i: number): void => {
    setPicked(i);
    net.send({ t: 'INPUT', p: fillinPayload.tone(i) });
  };
  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          YOU READ. {setup?.submitted ?? 0}/{setup?.writers ?? 0} WRITING
        </span>
        {deadline !== null && <Ring deadline={deadline} now={() => net.serverNow()} />}
      </header>
      {setup && <h1 class="prompt">{setup.card.text}</h1>}
      <div class="section-label">READ THEIR FILTH AS…</div>
      <div class="pick-row">
        {v.you.toneOptions.map((t, i) => (
          <button key={t} class={picked === i ? 'pick sel' : 'pick'} onClick={() => pick(i)}>
            {t.toUpperCase()}
          </button>
        ))}
      </div>
      {picked !== null && <div class="locked-banner flash-in">TONE SET — CLEAR YOUR THROAT</div>}
    </main>
  );
}

/** The performing phone (single-mode Reader): huge type, one line at a time, NEXT + BURN LINE (UGC strike).
 * Face-off Reader gets the same stage, but one finalist line and a single [I SOLD IT] read tap. */
function Teleprompter({ v, view, net }: { v: FillinPerformView; view: RoomView; net: Net }) {
  const tp = v.teleprompter;
  if (!tp) return null;
  const tone = toneLabel(v.tone);

  if (v.stage === 'faceoff') {
    return (
      <main class="screen tele">
        <header class="vote-head">
          <span class="vote-tally">{progressLabel(v)}</span>
        </header>
        <div class="double-tag">FACE-OFF</div>
        <div class="tele-line flash-in">{tp.text}</div>
        <div class="tele-actions">
          <button class="btn-blood big tele-next" onClick={() => net.send({ t: 'INPUT', p: fillinPayload.read() })}>
            I SOLD IT ▸
          </button>
        </div>
        <p class="tele-note">You are {nameOf(view, view.you)}. Land your finalist, then tap. The other Reader's up too.</p>
      </main>
    );
  }

  return (
    <main class="screen tele">
      <header class="vote-head">
        <span class="vote-tally">{progressLabel(v)}</span>
        {tone && <span class="tone-chip">AS {tone.toUpperCase()}</span>}
      </header>
      {v.card && <div class="tele-setup">{v.card.text}</div>}
      <div class="tele-line flash-in">{tp.text}</div>
      <div class="tele-actions">
        {tp.canBurn && (
          <button class="burn-line" onClick={() => net.send({ t: 'INPUT', p: fillinPayload.burn() })}>
            BURN LINE
          </button>
        )}
        <button class="btn-blood big tele-next" onClick={() => net.send({ t: 'INPUT', p: fillinPayload.next() })}>
          NEXT ▸
        </button>
      </div>
      <p class="tele-note">You are {nameOf(view, view.you)}. They are listening. Ruin them.</p>
    </main>
  );
}

/** Derange (N=3): every player performs SOMEONE ELSE's line out loud, then taps [I READ IT]. */
function DerangePerform({ v, net }: { v: FillinPerformView; net: Net }) {
  const a = v.assignment;
  if (!a) return null;
  return (
    <main class="screen tele">
      <header class="vote-head">
        <span class="vote-tally">{progressLabel(v)}</span>
      </header>
      <div class="derange-tag breathe">YOU'RE PERFORMING {a.authorName.toUpperCase()}'S FILTH — SELL IT</div>
      {v.card && <div class="tele-setup">{v.card.text}</div>}
      <div class="tele-line flash-in">{a.text}</div>
      <div class="tele-actions">
        <button class="btn-blood big tele-next" onClick={() => net.send({ t: 'INPUT', p: fillinPayload.read() })}>
          I READ IT ▸
        </button>
      </div>
      <p class="tele-note">First time these words hit the room is your mouth. No take-backs.</p>
    </main>
  );
}

/** Everyone who is NOT performing: no text, just the command to listen. */
function ListenUp({ v, view, net, heat }: { v: FillinPerformView; view: RoomView; net: Net; heat: number }) {
  const tone = toneLabel(v.tone);
  return (
    <main class="screen listen">
      {v.stage === 'faceoff' && <div class="double-tag">FACE-OFF</div>}
      <div class="waiting-label breathe">LISTEN UP</div>
      <h1 class="listen-count">{progressLabel(v)}</h1>
      {tone && <div class="tone-chip">READ AS {tone.toUpperCase()}</div>}
      <p class="game-blurb">
        {v.stage === 'faceoff'
          ? 'Both finalists, head to head. Eyes up — your phone knows nothing.'
          : `${nameOf(view, v.readerId)} has the floor. Eyes up — the phones know nothing.`}
      </p>
      <div class="reveal-actions">
        <FireButton onFire={() => net.fire()} heat={heat} />
      </div>
    </main>
  );
}

/** Memory-aid ballot: full texts, randomized per phone server-side, own line dead. */
function Ballot({ v, net, deadline }: { v: FillinVoteView; net: Net; deadline: number | null }) {
  const [picked, setPicked] = useState<number | null>(null);
  const locked = v.youVoted !== null || picked !== null;
  const cast = (id: number, yours: boolean): void => {
    if (locked || yours) return;
    setPicked(id);
    net.send({ t: 'INPUT', p: fillinPayload.vote(id) });
  };
  const chosen = picked ?? v.youVoted;

  if (v.stage === 'faceoff' && v.ballot.length === 2) {
    return (
      <main class="screen vote">
        <header class="vote-head">
          <span class="vote-tally">
            {v.votedCount}/{v.eligible} VOTED · FACE-OFF
          </span>
          {deadline !== null && <Ring deadline={deadline} now={() => net.serverNow()} />}
        </header>
        <h1 class="prompt">TWO LINES ENTER. ONE WALKS.</h1>
        <div class={locked ? 'faceoff locked' : 'faceoff'}>
          {v.ballot.map((e, i) => (
            <button
              key={e.id}
              class={'faceoff-slab' + (chosen === e.id ? ' picked' : '')}
              disabled={locked || e.yours}
              onClick={() => cast(e.id, e.yours)}
            >
              <span class="faceoff-label">VOTE {i === 0 ? 'A' : 'B'}</span>
              <span class="faceoff-text">{e.text}</span>
            </button>
          ))}
        </div>
        {locked && <div class="locked-banner flash-in">LOCKED IN</div>}
      </main>
    );
  }

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">
          {v.votedCount}/{v.eligible} VOTED
        </span>
        {deadline !== null && <Ring deadline={deadline} now={() => net.serverNow()} />}
      </header>
      <h1 class="prompt">CROWN THE ONE THAT KILLED.</h1>
      <div class={locked ? 'ballot locked' : 'ballot'}>
        {v.ballot.map((e) => (
          <button
            key={e.id}
            class={'ballot-row' + (e.yours ? ' own' : '') + (chosen === e.id ? ' picked' : '')}
            disabled={locked || e.yours}
            onClick={() => cast(e.id, e.yours)}
          >
            <span class="ballot-text">{e.text}</span>
            {e.yours && <span class="ballot-tag">YOURS</span>}
          </button>
        ))}
      </div>
      {locked && <div class="locked-banner flash-in">LOCKED IN</div>}
    </main>
  );
}

/** REVEAL: winning line + author in lights + runner-up (spec 5.2 phase 5). */
export function FillinReveal({
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
  const rv = asDeckView<FillinRevealView>(view.gameView, 'fillin', 'REVEAL');
  return (
    <HoldShell net={net} deadlines={deadlines} heat={heat} epoch={epoch} holdSince={holdSince} isHost={me?.role === 'host'}>
      {rv?.winner ? (
        <>
          <div class="win-line">“{rv.winner.text}”</div>
          <h1 class="lights">{nameOf(view, rv.winner.authorId)}</h1>
          {rv.winner.panic && <div class="panic-flag">A SHELF LINE. HALF CREDIT. FULL SHAME.</div>}
          {rv.runnerUp && (
            <div class="runner-up">
              runner-up: “{rv.runnerUp.text}” — {nameOf(view, rv.runnerUp.authorId)}
            </div>
          )}
        </>
      ) : (
        <>
          <h1 class="lights">{rv?.voided ? 'VOIDED' : 'NOBODY LAUGHED'}</h1>
          <p class="reveal-prompt">
            {rv?.voided ? 'The round died in the chair. Nobody scores.' : 'No votes, no winner, no mercy.'}
          </p>
        </>
      )}
    </HoldShell>
  );
}
