// REVEAL (spec 5.1 phase 3 + 4.2): 3-2-1 full-screen flash synced to a server AT deadline,
// then the victim's name in lights + vote spread. Fire button live; DESCEND per rules
// (host always, everyone past the 45s softcap — server enforces, we show).
import { useEffect, useMemo, useState } from 'preact/hooks';
import { canDescend, flipAt } from '../logic';
import type { Net } from '../net/ws';
import { asView, type PlayerView, type RoastRevealView, type RoomView } from '../view';
import { Countdown321, FireButton } from './bits';

export function RevealScreen({
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
  // Flip moment computed once per phase entry; a late rejoiner gets null => render now (3.5).
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const flip = useMemo(() => flipAt(holdSince, deadlines, net.serverNow()), [epoch]);
  const [flipped, setFlipped] = useState(flip === null);
  const isHost = me?.role === 'host';
  const [descendOk, setDescendOk] = useState(() => canDescend(isHost, holdSince, net.serverNow()));

  useEffect(() => {
    let raf = 0;
    const tick = (): void => {
      const now = net.serverNow();
      if (flip !== null && now >= flip) setFlipped(true);
      setDescendOk(canDescend(isHost, holdSince, now));
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [flip, isHost, holdSince]);

  if (!flipped && flip !== null) {
    return (
      <main class="screen reveal-count">
        <Countdown321 at={flip} now={() => net.serverNow()} />
      </main>
    );
  }

  const rv = asView<RoastRevealView>(view.gameView, 'REVEAL');
  const name = (id: string): string => view.players.find((p) => p.id === id)?.name ?? '???';
  const victims = rv ? rv.victims.map(name) : [];
  const spread = rv?.spread ?? [];
  const maxWeight = Math.max(1, ...spread.map((s) => s.weight));
  const fmt = (w: number): string => (Number.isInteger(w) ? String(w) : w.toFixed(1));

  return (
    <main class="screen reveal flash-in">
      {rv?.doubleRoast && <div class="double-tag">DOUBLE ROAST</div>}
      <h1 class="lights">
        {rv?.voided ? 'VOIDED' : victims.length > 0 ? victims.join(' & ') : 'NOBODY BURNED'}
      </h1>
      {rv?.voided && <p class="reveal-prompt">The host killed this one. Nobody scores; nobody talks.</p>}
      {rv && !rv.voided && <p class="reveal-prompt">{rv.prompt.text}</p>}
      {spread.length > 0 && (
        <div class="spread">
          {spread.map((s) => (
            <div class="spread-row" key={s.target}>
              <span class="spread-name">{name(s.target)}</span>
              <span class="spread-bar" style={`width:${(s.weight / maxWeight) * 100}%`} />
              <span class="spread-n">{fmt(s.weight)}</span>
            </div>
          ))}
        </div>
      )}
      {rv?.edges && rv.edges.length > 0 && (
        <div class="spread">
          <div class="accusers-tag">FACE YOUR ACCUSERS</div>
          {rv.edges.map((e, i) => (
            <div class="edge-row" key={`${e.from}-${i}`}>
              <span class="spread-name">{name(e.from)}</span>
              <span class="edge-arrow">→</span>
              <span class="spread-name">{e.to === '▮▮▮' ? '▮▮▮' : name(e.to)}</span>
            </div>
          ))}
        </div>
      )}
      {rv?.roomHeat && <div class="heat-banner">ROOM HEAT — THE TABLE AGREED. PLURALITY PAID.</div>}
      <div class="reveal-actions">
        <FireButton onFire={() => net.fire()} heat={heat} />
        {descendOk && (
          <button class="btn-descend" onClick={() => net.send({ t: 'DESCEND' })}>
            DESCEND ▼
          </button>
        )}
      </div>
    </main>
  );
}
