// Shared atoms. Design law 6.2: reveals SNAP, holds breathe, nothing eases politely.
import type { ComponentChildren } from 'preact';
import { useEffect, useRef, useState } from 'preact/hooks';

/** Numbered devil glyph — 16 avatar slots, each cocks its horns a little differently. */
export function Devil({ n, size = 48 }: { n: number; size?: number }) {
  const tilt = (n % 4) * 7 - 10;
  const wide = 4 + (Math.floor(n / 4) % 2) * 2;
  return (
    <svg viewBox="0 0 32 32" width={size} height={size} class="devil" aria-hidden="true">
      <g transform={`rotate(${tilt} 16 16)`}>
        <path d={`M${8 - wide / 2} 14 C6 10 4 7 3 2 C9 5 12 8 13 13 Z`} fill="currentColor" />
        <path d={`M${24 + wide / 2} 14 C26 10 28 7 29 2 C23 5 20 8 19 13 Z`} fill="currentColor" />
      </g>
      <circle cx="16" cy="20" r="10.5" fill="currentColor" />
      <text x="16" y="24.5" class="devil-num" fill="var(--pit)">
        {n + 1}
      </text>
    </svg>
  );
}

export function Flame({ lit = true, size = 26 }: { lit?: boolean; size?: number }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} class={lit ? 'flame lit' : 'flame'} aria-hidden="true">
      <path
        d="M12 2c1 4 5 5.5 5 10a5 5 0 0 1-10 0c0-2.5 1.6-3.8 2.4-6 .7 1.5 1.9 2.3 2.6-4z"
        fill="currentColor"
      />
    </svg>
  );
}

export function Crown({ size = 56 }: { size?: number }) {
  return (
    <svg viewBox="0 0 32 20" width={size} height={(size * 20) / 32} class="crown" aria-hidden="true">
      <path d="M2 18 L4 5 L10 11 L16 2 L22 11 L28 5 L30 18 Z" fill="currentColor" />
    </svg>
  );
}

/**
 * Countdown ring driven by an AT deadline mapped through serverNow() — never a local
 * interval accumulating drift (spec 3.3). Redraws from the wall each frame.
 */
export function Ring({ deadline, now }: { deadline: number; now: () => number }) {
  const [frac, setFrac] = useState(1);
  const [secs, setSecs] = useState(() => Math.ceil(Math.max(0, deadline - now()) / 1000));
  const totalRef = useRef(0);
  useEffect(() => {
    totalRef.current = Math.max(1000, deadline - now());
    let raf = 0;
    const tick = (): void => {
      const rem = Math.max(0, deadline - now());
      setFrac(Math.min(1, Math.round((rem / totalRef.current) * 100) / 100));
      setSecs(Math.ceil(rem / 1000));
      if (rem > 0) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deadline]);
  const C = 2 * Math.PI * 26;
  const timerLabel = `${secs} ${secs === 1 ? 'second' : 'seconds'} remaining`;
  return (
    <svg class="ring" viewBox="0 0 64 64" role="timer" aria-label={timerLabel}>
      <circle cx="32" cy="32" r="26" fill="none" stroke="var(--ash)" stroke-width="5" opacity="0.4" />
      <circle
        cx="32"
        cy="32"
        r="26"
        fill="none"
        stroke={secs <= 5 ? 'var(--blood)' : 'var(--ember)'}
        stroke-width="5"
        stroke-dasharray={`${C}`}
        stroke-dashoffset={`${C * (1 - frac)}`}
        transform="rotate(-90 32 32)"
      />
      <text x="32" y="41" class="ring-num" fill="var(--bone)">
        {secs}
      </text>
    </svg>
  );
}

/** Full-screen 3-2-1 before a reveal flip. Digits snap; no polite easing. */
export function Countdown321({ at, now }: { at: number; now: () => number }) {
  const [n, setN] = useState(() => Math.max(0, Math.ceil((at - now()) / 1000)));
  useEffect(() => {
    let raf = 0;
    const tick = (): void => {
      const rem = at - now();
      setN(Math.max(0, Math.ceil(rem / 1000)));
      if (rem > 0) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [at]);
  return (
    <div class="count321" role="timer" aria-live="assertive" aria-label={n > 0 ? `Reveal in ${n}` : 'Revealing now'}>
      <span key={n} class="count-num" aria-hidden="true">
        {n > 0 ? n : ''}
      </span>
    </div>
  );
}

/**
 * Fire button — hold-to-spam. Taps animate locally and instantly; the wire only sees
 * one coalesced FIRE per 500ms (onFire goes through Net.fire()).
 */
export function FireButton({ onFire, heat }: { onFire: () => void; heat: number }) {
  const [sparks, setSparks] = useState<number[]>([]);
  const idRef = useRef(0);
  const holdRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const burn = (): void => {
    onFire();
    idRef.current++;
    setSparks((s) => [...s.slice(-14), idRef.current]);
  };
  const stop = (): void => {
    if (holdRef.current) {
      clearInterval(holdRef.current);
      holdRef.current = null;
    }
  };
  const start = (e: Event): void => {
    e.preventDefault();
    if (holdRef.current) return;
    burn();
    holdRef.current = setInterval(burn, 140); // cosmetic spam cadence; wire stays coalesced
  };
  useEffect(() => stop, []);

  return (
    <button
      class="fire-btn"
      onPointerDown={start}
      onPointerUp={stop}
      onPointerLeave={stop}
      onPointerCancel={stop}
      onClick={(event) => {
        // Pointer users already fired on pointerdown; keyboard/AT activation emits
        // a click with detail=0 and needs an explicit single-fire path.
        if (event.detail === 0) burn();
      }}
      onBlur={stop}
      onContextMenu={(e) => e.preventDefault()}
      aria-label={`Send fire reaction${heat > 0 ? `; ${heat} room heat` : ''}`}
    >
      <Flame size={34} />
      <span class="fire-label">FIRE</span>
      {heat > 0 && <span class="fire-count">{heat}</span>}
      {sparks.map((id) => (
        <i key={id} class="spark" style={`left:${((id * 37) % 60) + 20}%;animation-delay:${(id % 3) * 40}ms`} />
      ))}
    </button>
  );
}

function focusableElements(root: HTMLElement): HTMLElement[] {
  return [...root.querySelectorAll<HTMLElement>('button:not([disabled]), a[href], input:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')]
    .filter((element) => !element.hasAttribute('inert'));
}

/** Full-screen private/action dialog with focus containment and inert background restoration. */
export function ModalOverlay({
  label,
  className = '',
  children,
}: {
  label: string;
  className?: string;
  children: ComponentChildren;
}) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const modal = ref.current;
    if (!modal) return undefined;
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const changed: Array<{ element: HTMLElement; inert: boolean; ariaHidden: string | null }> = [];
    let branch: HTMLElement = modal;
    while (branch.parentElement) {
      const parent = branch.parentElement;
      for (const sibling of parent.children) {
        if (!(sibling instanceof HTMLElement) || sibling === branch || sibling.classList.contains('room-error')) continue;
        changed.push({ element: sibling, inert: sibling.inert, ariaHidden: sibling.getAttribute('aria-hidden') });
        sibling.inert = true;
        sibling.setAttribute('aria-hidden', 'true');
      }
      if (parent.id === 'app') break;
      branch = parent;
    }
    const frame = requestAnimationFrame(() => (focusableElements(modal)[0] ?? modal).focus());
    return () => {
      cancelAnimationFrame(frame);
      for (const item of changed) {
        item.element.inert = item.inert;
        if (item.ariaHidden === null) item.element.removeAttribute('aria-hidden');
        else item.element.setAttribute('aria-hidden', item.ariaHidden);
      }
      if (previous?.isConnected) previous.focus();
    };
  }, []);

  return (
    <div
      ref={ref}
      class={`overlay${className ? ` ${className}` : ''}`}
      role="dialog"
      aria-modal="true"
      aria-label={label}
      tabIndex={-1}
      onKeyDown={(event) => {
        if (event.key !== 'Tab' || !ref.current) return;
        const focusable = focusableElements(ref.current);
        if (focusable.length === 0) {
          event.preventDefault();
          return;
        }
        const first = focusable[0]!;
        const last = focusable[focusable.length - 1]!;
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }}
    >
      {children}
    </div>
  );
}

export function Overlay({ title, sub, children }: { title: string; sub?: string; children?: ComponentChildren }) {
  const contents = (
    <>
      <div class="overlay-title breathe">{title}</div>
      {sub && <div class="overlay-sub">{sub}</div>}
      {children}
    </>
  );
  return <ModalOverlay label={title}>{contents}</ModalOverlay>;
}

export function Toast({ text }: { text: string }) {
  return (
    <div class="toast flash-in" role="status" aria-live="polite" aria-atomic="true">
      {text}
    </div>
  );
}
