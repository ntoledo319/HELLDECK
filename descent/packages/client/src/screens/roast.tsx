// Roast Consensus input screens (spec 5.1 phase 2) + generic INPUT/WAITING_ON fallbacks.
import { useConnectionOptimistic } from '../connection';
import type { Net } from '../net/ws';
import { asView, type PlayerView, type RoastVoteView, type RoomView } from '../view';
import { Devil, Ring } from './bits';

/** Prompt + tappable avatar grid; self disabled; LOCKED after the tap lands. */
export function RoastVote({
  view,
  net,
  deadline,
}: {
  view: RoomView;
  net: Net;
  deadline: number | null;
}) {
  const gv = asView<RoastVoteView>(view.gameView, 'VOTE');
  const [picked, setPicked] = useConnectionOptimistic<string | null>(null);
  const locked = gv?.youVoted != null || picked !== null;
  const targets = view.players; // imps are citizens too: roastable, half-vote (4.8)

  const pick = (id: string): void => {
    if (locked || id === view.you) return;
    if (net.send({ t: 'INPUT', p: { vote: id } })) setPicked(id);
  };

  const selection = picked ?? gv?.youVoted ?? null;
  const pickedName = targets.find((t) => t.id === selection)?.name;

  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">{gv ? `${gv.votedCount}/${gv.eligible} VOTED` : 'VOTE'}</span>
        {deadline !== null && <Ring deadline={deadline} now={() => net.serverNow()} />}
      </header>
      <h1 class="prompt">{gv?.prompt.text ?? '…'}</h1>
      {gv && gv.loops > 1 && (
        <div class="prompt-idx">
          PROMPT {gv.loop + 1} OF {gv.loops}
        </div>
      )}
      <div class={locked ? 'vote-grid locked' : 'vote-grid'}>
        {targets.map((p) => (
          <button
            key={p.id}
            class={
              'vote-cell' + (p.id === view.you ? ' self' : '') + (selection === p.id ? ' picked' : '')
            }
            disabled={locked || p.id === view.you}
            aria-pressed={selection === p.id}
            onClick={() => pick(p.id)}
          >
            <Devil n={p.avatar} size={36} />
            <span>{p.id === view.you ? "THAT'S YOU" : p.name}</span>
          </button>
        ))}
      </div>
      {locked && (
        <div class="locked-banner flash-in" role="status" aria-live="polite">
          LOCKED IN{pickedName ? ` — ${pickedName} BURNS` : ''}
        </div>
      )}
    </main>
  );
}

/** INPUT phases this client doesn't have a bespoke screen for yet. */
export function InputFallback({ sub, deadline, net }: { sub: string; deadline: number | null; net: Net }) {
  return (
    <main class="screen vote">
      <header class="vote-head">
        <span class="vote-tally">{sub.toUpperCase()}</span>
        {deadline !== null && <Ring deadline={deadline} now={() => net.serverNow()} />}
      </header>
      <h1 class="prompt breathe">DO YOUR WORST.</h1>
    </main>
  );
}

/** Blocking shame state (spec 4.7): the room stares; the owner can plead; the host can void. */
export function WaitingOn({
  view,
  me,
  net,
  who,
}: {
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  who: string;
}) {
  const owner = view.players.find((p) => p.id === who);
  const isMe = who === view.you;
  return (
    <main class="screen waiting">
      <div class="waiting-label breathe">WAITING ON</div>
      <h1 class="lights">{owner?.name ?? '???'}</h1>
      {isMe && (
        <button class="btn-ghost" onClick={() => net.send({ t: 'FIFTH' })}>
          PLEAD THE FIFTH
        </button>
      )}
      {me?.role === 'host' && !isMe && (
        <button class="btn-ghost" onClick={() => net.send({ t: 'VOID' })}>
          VOID ROUND
        </button>
      )}
    </main>
  );
}
