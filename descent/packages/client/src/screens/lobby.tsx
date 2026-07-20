// Lobby (spec 6.1): HUGE code + client-side QR, roster with connection dots, host depth/vibe
// pickers gated by pool arithmetic, every player's private heat dial (value never rendered
// after pick — SEALED), BEGIN THE DESCENT. The server is the enforcer; this is the mirror.
import { useMemo, useState } from 'preact/hooks';
import { depthGate } from '../logic';
import type { Net } from '../net/ws';
import { qrEncode } from '../vendor/qr';
import type { PlayerView, RoomView } from '../view';
import { Devil, Flame } from './bits';

const DEPTHS = [
  { d: 5 as const, name: 'QUICK DIP', sub: '5 CIRCLES · ~20 MIN' },
  { d: 7 as const, name: 'STANDARD DESCENT', sub: '7 CIRCLES · ~27 MIN' },
  { d: 9 as const, name: 'FULL DAMNATION', sub: '9 CIRCLES · ~32 MIN' },
];
const VIBES = [
  { v: 'sober', name: 'SOBER' },
  { v: 'warm', name: 'WARMED UP' },
  { v: 'feral', name: 'FERAL' },
] as const;

function QR({ text }: { text: string }) {
  const path = useMemo(() => {
    const m = qrEncode(text);
    let d = '';
    m.forEach((row, y) => {
      row.forEach((dark, x) => {
        if (dark) d += `M${x + 4} ${y + 4}h1v1h-1z`; // +4 = quiet zone (spec 6.3)
      });
    });
    return { d, size: m.length + 8 };
  }, [text]);
  return (
    <svg class="qr" viewBox={`0 0 ${path.size} ${path.size}`} shape-rendering="crispEdges" aria-hidden="true">
      <rect width={path.size} height={path.size} fill="var(--bone)" />
      <path d={path.d} fill="var(--pit)" />
    </svg>
  );
}

/** Private heat-ceiling flame dial. The picked value is discarded the moment it ships. */
function HeatDial({ sealed, onSeal }: { sealed: boolean; onSeal: (v: 1 | 2 | 3 | 4 | 5) => void }) {
  const [dial, setDial] = useState(0);
  if (sealed) {
    return (
      <div class="dial">
        <div class="dial-q">YOUR HEAT CEILING</div>
        <div class="sealed-stamp">SEALED</div>
        <div class="dial-note">Nobody sees your number. Not even a hint. Cards about you never run hotter.</div>
      </div>
    );
  }
  return (
    <div class="dial">
      <div class="dial-q">HOW HOT CAN CARDS ABOUT YOU RUN?</div>
      <div class="dial-flames">
        {[1, 2, 3, 4, 5].map((v) => (
          <button key={v} class={dial >= v ? 'dial-flame lit' : 'dial-flame'} onClick={() => setDial(v)}>
            <Flame lit={dial >= v} size={22} />
          </button>
        ))}
      </div>
      <button class="btn-ghost" disabled={dial === 0} onClick={() => onSeal(dial as 1 | 2 | 3 | 4 | 5)}>
        SEAL IT
      </button>
      <div class="dial-note">Private. Silent. The deck obeys; the room never knows.</div>
    </div>
  );
}

export function Lobby({ view, me, net }: { view: RoomView; me: PlayerView | null; net: Net }) {
  const isHost = me?.role === 'host';
  const sinners = view.players.filter((p) => p.role !== 'imp');
  const gate = depthGate(sinners.length);
  const cfg = view.config;

  const sealedKey = `hd:${view.code}:sealed`;
  const [sealed, setSealed] = useState(
    () => me?.sealed === true || localStorage.getItem(sealedKey) === '1',
  );
  const seal = (v: 1 | 2 | 3 | 4 | 5): void => {
    net.send({ t: 'CEILING', v });
    localStorage.setItem(sealedKey, '1'); // the FACT is stored; the value evaporates here
    setSealed(true);
  };

  const setConfig = (depth: number, vibe: string): void => {
    net.send({ t: 'CONFIG', depth, vibe, stage: sinners.length >= 5 });
  };

  const beginHint = !isHost
    ? 'THE HOST LIGHTS THE FUSE'
    : sinners.length < 3
      ? `NEED ${3 - sinners.length} MORE ${sinners.length === 2 ? 'BODY' : 'BODIES'}`
      : !cfg
        ? 'PICK A DEPTH'
        : !sealed
          ? 'SEAL YOUR HEAT FIRST'
          : view.players.some((p) => p.sealed === false)
            ? 'WAITING ON SEALS'
            : null;

  return (
    <main class="screen lobby">
      <div class="section-label">THE PIT IS OPEN — GET THEM IN</div>
      <div class="room-code">{view.code}</div>
      <div class="qr-wrap">
        <QR text={`${location.origin}/${view.code}`} />
      </div>

      <div class="section-label">
        {sinners.length}/12 SINNERS{view.players.length > sinners.length ? ` · ${view.players.length - sinners.length} IMP` : ''}
      </div>
      <div class="roster">
        {view.players.map((p) => (
          <div key={p.id} class={p.role === 'host' ? 'roster-row host-row' : 'roster-row'}>
            <span class={p.connected ? 'conn-dot on' : 'conn-dot'} />
            <span style="color:var(--ember)">
              <Devil n={p.avatar} size={26} />
            </span>
            <span class="roster-name">
              {p.name}
              {p.id === view.you ? ' — YOU' : ''}
            </span>
            {p.role === 'imp' && <span class="roster-tag">IMP</span>}
            {p.role === 'host' && <span class="roster-tag">HOST</span>}
            {p.sealed === true && <span class="roster-tag sealed-tag">SEALED</span>}
          </div>
        ))}
      </div>

      <HeatDial sealed={sealed} onSeal={seal} />

      {isHost && (
        <>
          <div class="section-label">HOW DEEP DOES TONIGHT GO?</div>
          <div class="pick-row">
            {DEPTHS.map((o) => (
              <button
                key={o.d}
                class={cfg?.depth === o.d ? 'pick sel' : 'pick'}
                disabled={o.d > gate.max}
                onClick={() => setConfig(o.d, cfg?.vibe ?? 'warm')}
              >
                <span>{o.name}</span>
                <span class="pick-sub">{o.sub}</span>
              </button>
            ))}
          </div>
          {gate.warning && <div class="gate-note">{gate.warning}</div>}

          <div class="section-label">VIBE CHECK</div>
          <div class="pick-row">
            {VIBES.map((o) => (
              <button
                key={o.v}
                class={cfg?.vibe === o.v ? 'pick sel' : 'pick'}
                onClick={() => setConfig(cfg?.depth ?? 7, o.v)}
              >
                <span>{o.name}</span>
              </button>
            ))}
          </div>
        </>
      )}

      <div class="begin-block">
        {beginHint && <div class="begin-hint">{beginHint}</div>}
        {isHost && (
          <button class="btn-blood big" disabled={beginHint !== null} onClick={() => net.send({ t: 'BEGIN' })}>
            BEGIN THE DESCENT
          </button>
        )}
      </div>
    </main>
  );
}
