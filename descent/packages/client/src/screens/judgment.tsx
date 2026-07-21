// THE JUDGMENT (spec 6.1): winner crowned, superlatives staggered 2s apart, the Devil's
// Bargain finally outed, share-card PNG via canvas + Web Share (download fallback),
// DESCEND AGAIN for the host (a DESCEND at Judgment resets the night to the lobby).
import { useState } from 'preact/hooks';
import { rankPlayers } from '../logic';
import type { Net } from '../net/ws';
import { shareCard } from '../share/card';
import type { PlayerView, RoomView } from '../view';
import { Crown } from './bits';

export function Judgment({
  view,
  me,
  net,
  fires,
}: {
  view: RoomView;
  me: PlayerView | null;
  net: Net;
  fires: number;
}) {
  const jv = view.judgment;
  const name = (id: string): string => view.players.find((p) => p.id === id)?.name ?? '???';
  const ranked = rankPlayers(view.players);
  const winnerNames =
    jv && jv.winners.length > 0 ? jv.winners.map(name) : ranked[0] ? [ranked[0].name] : [];
  const supers = (jv?.superlatives ?? []).map((s) => ({ name: name(s.playerId), title: s.title }));
  const bargain = jv?.bargain ?? null;
  const [shareNote, setShareNote] = useState<string | null>(null);
  const [sharing, setSharing] = useState(false);

  const doShare = async (): Promise<void> => {
    if (sharing) return;
    setSharing(true);
    setShareNote(null);
    try {
      const res = await shareCard({
        winnerName: winnerNames.join(' & ') || '???',
        superlatives: supers,
        stats: {
          players: view.players.length,
          circles: view.arcLength,
          fires,
        },
        url: `${location.origin}/`,
      });
      setShareNote(
        res === 'shared' ? 'DAMAGE SPREAD.' : res === 'downloaded' ? 'SAVED TO YOUR PHONE.' : 'SHARE CANCELED.',
      );
    } catch {
      setShareNote('THE CARD ESCAPED. TRY AGAIN.');
    } finally {
      setSharing(false);
    }
  };

  return (
    <main class="screen judgment">
      <div class="judgment-label">THE JUDGMENT</div>
      <Crown />
      <h1 class="lights">{winnerNames.length > 0 ? winnerNames.join(' & ') : '???'}</h1>
      <div class="winner-tag">{winnerNames.length > 1 ? "TONIGHT'S DEVILS" : "TONIGHT'S DEVIL"}</div>
      <div class="supers">
        {supers.map((s, i) => (
          <div key={`${s.title}-${i}`} class="super flash-in" style={`animation-delay:${i * 2}s`}>
            <span class="super-title">{s.title}</span>
            <span class="super-name">{s.name}</span>
          </div>
        ))}
        {bargain && (
          <div class="super flash-in" style={`animation-delay:${supers.length * 2}s`}>
            <span class="super-title">THE DEVIL'S BARGAIN</span>
            <span class="super-name">
              {name(bargain.holder)} ran circle {bargain.circle + 1} at double or nothing
            </span>
          </div>
        )}
      </div>
      <div class="judgment-actions">
        <button class="btn-blood big" disabled={sharing} aria-busy={sharing} onClick={() => void doShare()}>
          {sharing ? 'FORGING THE RECEIPT…' : 'SPREAD THE DAMAGE'}
        </button>
        {shareNote && (
          <div class="share-note" role="status" aria-live="polite">
            {shareNote}
          </div>
        )}
        {me?.role === 'host' && (
          <button class="btn-ghost" onClick={() => net.send({ t: 'DESCEND' })}>
            DESCEND AGAIN
          </button>
        )}
      </div>
    </main>
  );
}
