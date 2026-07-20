// LADDER — 5s rank strip between circles (spec 6.1), biggest mover highlighted.
import { biggestMover, rankPlayers } from '../logic';
import { asView, type LadderGameView, type RoomView } from '../view';
import { Devil } from './bits';

export function Ladder({ view }: { view: RoomView }) {
  const lv = asView<LadderGameView>(view.gameView, 'LADDER');
  const ranked = rankPlayers(view.players);
  const mover = biggestMover(lv?.deltas);
  return (
    <main class="screen ladder">
      <div class="ladder-label">DESCENT RANK</div>
      <div class="ladder-rows">
        {ranked.map((p, i) => (
          <div key={p.id} class={p.id === mover ? 'ladder-row mover flash-in' : 'ladder-row'}>
            <span class="ladder-rank">{i + 1}</span>
            <span style="color:var(--ember)">
              <Devil n={p.avatar} size={26} />
            </span>
            <span class="ladder-name">{p.name}</span>
            {p.id === mover && lv?.deltas?.[p.id] !== undefined && (
              <span class="ladder-delta">+{lv.deltas[p.id]}</span>
            )}
            <span class="ladder-score">{p.score}</span>
          </div>
        ))}
      </div>
    </main>
  );
}
