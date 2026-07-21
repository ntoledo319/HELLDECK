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
      <div class="ladder-rows" role="list" aria-label="Current standings">
        {ranked.map((p) => {
          const place = ranked.findIndex((candidate) => candidate.score === p.score) + 1;
          const delta = lv?.deltas?.[p.id] ?? 0;
          return (
            <div
              key={p.id}
              role="listitem"
              class={p.id === mover ? 'ladder-row mover flash-in' : 'ladder-row'}
            >
              <span class="ladder-rank" aria-label={`Rank ${place}`}>
                {place}
              </span>
              <span style="color:var(--ember)">
                <Devil n={p.avatar} size={26} />
              </span>
              <span class="ladder-name">
                {p.name}
                {p.id === view.you && <span class="ladder-you"> YOU</span>}
              </span>
              <span
                class={delta > 0 ? 'ladder-delta gained' : 'ladder-delta'}
                aria-label={`${delta} points this circle`}
              >
                {delta > 0 ? `+${delta}` : '—'}
              </span>
              <span class="ladder-score">{p.score}</span>
            </div>
          );
        })}
      </div>
    </main>
  );
}
