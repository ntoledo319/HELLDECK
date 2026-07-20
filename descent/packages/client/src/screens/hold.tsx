// Shared REVEAL hold frame for the M2 game reveals (spec 4.2): synced 3-2-1 flash on
// the server AT beat, then the game's payload, fire button live, DESCEND per rules
// (host always, everyone past the 45s softcap — server enforces, we mirror).
// Same machinery as screens/reveal.tsx, extracted so fillin/overunder/confession
// reveals don't each re-implement the flip discipline. Roast's screen stays as-is.
import type { ComponentChildren } from 'preact';
import { useEffect, useMemo, useState } from 'preact/hooks';
import { canDescend, flipAt } from '../logic';
import type { Net } from '../net/ws';
import { Countdown321, FireButton } from './bits';

export function HoldShell({
  net,
  deadlines,
  heat,
  epoch,
  holdSince,
  isHost,
  children,
}: {
  net: Net;
  deadlines: ReadonlyMap<string, number>;
  heat: number;
  epoch: number;
  holdSince: number;
  isHost: boolean;
  children: ComponentChildren;
}) {
  // Flip moment computed once per phase entry; a late rejoiner gets null => render now (3.5).
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const flip = useMemo(() => flipAt(holdSince, deadlines, net.serverNow()), [epoch]);
  const [flipped, setFlipped] = useState(flip === null);
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

  return (
    <main class="screen reveal flash-in">
      {children}
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
