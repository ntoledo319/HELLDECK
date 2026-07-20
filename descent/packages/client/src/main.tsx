// Routes: "/" landing · "/:code" join + room (spec 6.1). The Room is a thin renderer:
// STATE in, taps out. The server is the only authority; every gate here is a mirror.
import { render, type JSX } from 'preact';
import { useEffect, useRef, useState } from 'preact/hooks';
import { playSting, unlockAudio } from './audio';
import { serverErrorMessage, validCode } from './logic';
import { Net, type ConnStatus } from './net/ws';
import { Overlay, Toast } from './screens/bits';
import { DealScreen, PreviewOverlay } from './screens/deal';
import { CircleIntro } from './screens/intro';
import { JoinScreen } from './screens/join';
import { Judgment } from './screens/judgment';
import { Ladder } from './screens/ladder';
import { Lobby } from './screens/lobby';
import { RevealScreen } from './screens/reveal';
import { StageShell } from './screens/stage';
import { SpotlightOverlay } from './screens/spotlight';
import {
  dismissSpotlight,
  expireSpotlight,
  parseSpotlightMessage,
  receiveSpotlightMessage,
  requestSpotlightBurn,
  type SpotlightClientState,
} from './screens/spotlight.logic';
import { InputFallback, RoastVote, WaitingOn } from './screens/roast';
import { ConfessionReveal, ConfessionScreen } from './screens/confession';
import { FillinReveal, FillinScreen } from './screens/fillin';
import { OverUnderReveal, OverUnderScreen } from './screens/overunder';
import { ScatterReveal, ScatterScreen } from './screens/scatter';
import { PoisonReveal, PoisonScreen } from './screens/poison';
import { RedflagReveal, RedflagScreen } from './screens/redflag';
import { AlibiReveal, AlibiScreen } from './screens/alibi';
import { TitleFightReveal, TitleFightScreen } from './screens/titlefight';
import { deckOf } from './games/wire';
import { asView, type RoastVoteView, type RoomView } from './view';
import { keepAwake } from './wake';
import './style/style.css';

function App() {
  const path = location.pathname.slice(1).toUpperCase();
  return validCode(path) ? <Room code={path} /> : <Landing />;
}

function Landing() {
  const [code, setCode] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const go = (): void => {
    const c = code.toUpperCase();
    if (validCode(c)) location.href = `/${c}`;
    else setErr('NO SUCH PIT — codes are 4 letters, never vowels.');
  };
  return (
    <main class="screen" style="justify-content:center;gap:18px">
      <h1 class="wordmark landing-title">
        HELL<em>DECK</em>
      </h1>
      {err && <div class="err-banner">{err}</div>}
      <button
        class="btn-blood big"
        onClick={() => {
          void (async () => {
            try {
              const r = await fetch('/api/room', { method: 'POST' });
              const j = (await r.json()) as { code: string };
              location.href = `/${j.code}`;
            } catch {
              setErr('THE PIT IS UNREACHABLE — check your signal.');
            }
          })();
        }}
      >
        START A NIGHT
      </button>
      <div class="code-row">
        <input
          class="code-input"
          maxLength={4}
          placeholder="CODE"
          autocomplete="off"
          value={code}
          onInput={(e) => {
            setErr(null);
            setCode((e.target as HTMLInputElement).value.toUpperCase());
          }}
          onKeyDown={(e) => {
            if ((e as KeyboardEvent).key === 'Enter') go();
          }}
        />
        <button class="btn-ghost" onClick={go}>
          JOIN
        </button>
      </div>
      <p class="landing-tag">18+. Play with people who can take it.</p>
    </main>
  );
}

// ===== room container =====
interface Profile {
  name: string;
  avatar: number;
}
interface RoomError {
  id: number;
  message: string;
}
const profileKey = (code: string): string => `hd:${code}:profile`;

function RoomErrorNotice({ error, onDismiss }: { error: RoomError; onDismiss: () => void }) {
  return (
    <div class="room-error flash-in">
      <span class="room-error-message" role="alert" aria-live="assertive" aria-atomic="true">
        {error.message}
      </span>
      <button type="button" class="room-error-dismiss" onClick={onDismiss} aria-label="Dismiss error message">
        DISMISS
      </button>
    </div>
  );
}

function loadProfile(code: string): Profile | null {
  try {
    const raw = localStorage.getItem(profileKey(code));
    if (!raw) return null;
    const p = JSON.parse(raw) as Profile;
    return typeof p.name === 'string' && typeof p.avatar === 'number' ? p : null;
  } catch {
    return null;
  }
}

function Room({ code }: { code: string }) {
  // Auto-rejoin silently when a profile is already on file (spec 3.1 / task D-105).
  const [profile, setProfile] = useState<Profile | null>(() => loadProfile(code));
  const [view, setView] = useState<RoomView | null>(null);
  const [epoch, setEpoch] = useState(0);
  const [conn, setConn] = useState<ConnStatus>('connecting');
  const [err, setErr] = useState<RoomError | null>(null);
  const [priv, setPriv] = useState<{ id: number; k: string; p: unknown } | null>(null);
  const [spotlight, setSpotlight] = useState<SpotlightClientState | null>(null);
  const [heat, setHeat] = useState(0);
  const [toast, setToast] = useState<string | null>(null);
  const [impAck, setImpAck] = useState(false);

  const netRef = useRef<Net | null>(null);
  const viewRef = useRef<RoomView | null>(null);
  const phaseKRef = useRef<string>('');
  const prevRoleRef = useRef<string | null>(null);
  const deadlinesRef = useRef(new Map<string, number>());
  const firesRef = useRef(0);
  const closeFailsRef = useRef(0);
  const errorIdRef = useRef(0);
  const privateIdRef = useRef(0);

  useEffect(() => {
    if (!profile) return;
    keepAwake(); // spec 6.3: wake lock on join, re-acquired on visibilitychange
    const net = new Net(code, {
      onState: (v, ep) => {
        const rv = v as RoomView;
        if (rv.phase.k !== phaseKRef.current) {
          phaseKRef.current = rv.phase.k;
          setPriv(null); // private payloads are phase-scoped
          setSpotlight(null);
          setHeat(0); // heat meter is per-reveal
        }
        viewRef.current = rv;
        setView(rv);
        setEpoch(ep);
      },
      onPrivate: (k, p) => {
        const id = ++privateIdRef.current;
        if (k === 'spotlight') {
          const message = parseSpotlightMessage(p);
          if (message) {
            const now = netRef.current?.serverNow() ?? Date.now();
            setPriv(null); // a new assignment supersedes any phase-stale card secret
            setSpotlight((current) => receiveSpotlightMessage(current, message, id, now));
          }
          return;
        }
        // Content-private payloads begin only after assignment settles. Clear the
        // role curtain immediately so it cannot mask the next full safety window.
        setSpotlight(null);
        setPriv({ id, k, p });
      },
      onAudio: (sting) => {
        // Host phone honors AUDIO; everyone else ignores (spec 3.2).
        const me = viewRef.current?.players.find((p) => p.id === viewRef.current?.you);
        if (me?.role === 'host') playSting(sting);
      },
      onHeat: (n) => {
        firesRef.current += n;
        setHeat((h) => h + n);
      },
      onDeadline: (id, at) => {
        deadlinesRef.current.set(id, at);
      },
      onStatus: (s) => {
        setConn(s);
        if (s === 'open') {
          closeFailsRef.current = 0;
          setErr(null); // a fresh connection must not revive a stale rejected action
          // (Re)seat: JOIN is idempotent server-side; token identifies us.
          net.send({ t: 'JOIN', name: profile.name, avatar: profile.avatar });
          net.send({ t: 'ATTEST18' });
        }
        if (s === 'closed') closeFailsRef.current++;
      },
      onWelcome: () => {},
      onError: (c, m) => {
        setErr({ id: ++errorIdRef.current, message: serverErrorMessage(c, m) });
      },
    });
    netRef.current = net;
    net.connect();
    const unlock = (): void => unlockAudio(); // audio unlock on first tap (spec 6.3)
    document.addEventListener('pointerdown', unlock, { once: true });
    return () => {
      net.close();
      document.removeEventListener('pointerdown', unlock);
    };
  }, [profile, code]);

  // Rejected taps are transient feedback, not room state. Keep them long enough to
  // read, allow immediate dismissal, and reset the timer for repeated failures.
  useEffect(() => {
    if (!profile || !err) return undefined;
    const id = err.id;
    const timer = setTimeout(() => {
      setErr((current) => (current?.id === id ? null : current));
    }, 8000);
    return () => clearTimeout(timer);
  }, [profile, err]);

  const spotlightCeremonyId = spotlight?.assignment.ceremonyId ?? null;
  const spotlightBurnDeadline = spotlight?.assignment.burnDeadline ?? null;
  useEffect(() => {
    if (spotlightCeremonyId === null || spotlightBurnDeadline === null) return undefined;
    const expire = (): void => {
      setSpotlight((current) => expireSpotlight(current, spotlightCeremonyId));
    };
    const remaining = spotlightBurnDeadline - (netRef.current?.serverNow() ?? Date.now());
    if (remaining <= 0) {
      expire();
      return undefined;
    }
    const timer = setTimeout(expire, remaining);
    return () => clearTimeout(timer);
  }, [spotlightCeremonyId, spotlightBurnDeadline]);

  const me = view ? (view.players.find((p) => p.id === view.you) ?? null) : null;

  // Imp conversion toast (spec 4.8): "clawed their way up".
  useEffect(() => {
    const role = me?.role ?? null;
    const converted = prevRoleRef.current === 'imp' && role === 'player';
    prevRoleRef.current = role;
    if (converted) {
      setToast("YOU'VE CLAWED YOUR WAY UP — FULL SINNER NOW");
      const t = setTimeout(() => setToast(null), 4000);
      return () => clearTimeout(t);
    }
    return undefined;
  }, [me?.role]);

  if (!profile) {
    return (
      <JoinScreen
        code={code}
        error={err?.message ?? (closeFailsRef.current >= 3 ? 'NO SUCH PIT — check the code.' : null)}
        onJoin={(name, avatar) => {
          const p = { name, avatar };
          localStorage.setItem(profileKey(code), JSON.stringify(p));
          setProfile(p);
        }}
      />
    );
  }

  const net = netRef.current;
  if (!view || !net) {
    return (
      <Overlay
        title={closeFailsRef.current >= 3 ? 'NO SUCH PIT?' : 'OPENING THE PIT…'}
        sub={closeFailsRef.current >= 3 ? 'The room may have burned down. Check the code.' : undefined}
      />
    );
  }

  let screen: JSX.Element;
  switch (view.phase.k) {
    case 'LOBBY':
    case 'CONSENT':
      screen = <Lobby view={view} me={me} net={net} />;
      break;
    case 'CIRCLE_INTRO':
      screen = <CircleIntro circleIdx={view.circleIdx} arcLength={view.arcLength} gameView={view.gameView} net={net} />;
      break;
    case 'DEAL':
      screen = <DealScreen players={view.players} />;
      break;
    case 'INPUT': {
      // Dispatch on the module view's deck discriminant (games/wire.ts).
      const deck = deckOf(view.gameView);
      screen =
        deck === 'fillin' ? (
          <FillinScreen view={view} net={net} deadline={view.phase.deadline} heat={heat} />
        ) : deck === 'overunder' ? (
          <OverUnderScreen view={view} me={me} net={net} deadline={view.phase.deadline} />
        ) : deck === 'confession' ? (
          <ConfessionScreen view={view} me={me} net={net} deadline={view.phase.deadline} heat={heat} />
        ) : deck === 'scatter' ? (
          <ScatterScreen view={view} me={me} net={net} deadline={view.phase.deadline} heat={heat} />
        ) : deck === 'poison' ? (
          <PoisonScreen view={view} me={me} net={net} deadline={view.phase.deadline} heat={heat} />
        ) : deck === 'redflag' ? (
          <RedflagScreen view={view} me={me} net={net} deadline={view.phase.deadline} heat={heat} />
        ) : deck === 'alibi' ? (
          <AlibiScreen view={view} me={me} net={net} deadline={view.phase.deadline} heat={heat} />
        ) : deck === 'titlefight' ? (
          <TitleFightScreen view={view} me={me} net={net} deadline={view.phase.deadline} heat={heat} />
        ) : asView<RoastVoteView>(view.gameView, 'VOTE') ? (
          <RoastVote key={epoch} view={view} net={net} deadline={view.phase.deadline} />
        ) : (
          <InputFallback sub={view.phase.sub} deadline={view.phase.deadline} net={net} />
        );
      break;
    }
    case 'WAITING_ON': {
      // Blocking games keep their own screens through the shame state (owner keeps the
      // input UI, the room gets the pit vote — spec 4.7); everything else shames plainly.
      const deck = deckOf(view.gameView);
      screen =
        deck === 'overunder' ? (
          <OverUnderScreen view={view} me={me} net={net} deadline={null} />
        ) : deck === 'confession' ? (
          <ConfessionScreen view={view} me={me} net={net} deadline={null} heat={heat} />
        ) : (
          <WaitingOn view={view} me={me} net={net} who={view.phase.who} />
        );
      break;
    }
    case 'REVEAL': {
      const deck = deckOf(view.gameView);
      const hold = {
        view,
        me,
        net,
        deadlines: deadlinesRef.current,
        heat,
        epoch,
        holdSince: view.phase.holdSince,
      };
      screen =
        deck === 'fillin' ? (
          <FillinReveal key={epoch} {...hold} />
        ) : deck === 'overunder' ? (
          <OverUnderReveal key={epoch} {...hold} />
        ) : deck === 'confession' ? (
          <ConfessionReveal key={epoch} {...hold} />
        ) : deck === 'scatter' ? (
          <ScatterReveal key={epoch} {...hold} />
        ) : deck === 'poison' ? (
          <PoisonReveal key={epoch} {...hold} />
        ) : deck === 'redflag' ? (
          <RedflagReveal key={epoch} {...hold} />
        ) : deck === 'alibi' ? (
          <AlibiReveal key={epoch} {...hold} />
        ) : deck === 'titlefight' ? (
          <TitleFightReveal key={epoch} {...hold} />
        ) : (
          <RevealScreen key={epoch} {...hold} />
        );
      break;
    }
    case 'LADDER':
      screen = <Ladder view={view} />;
      break;
    case 'JUDGMENT':
      screen = <Judgment view={view} me={me} net={net} fires={firesRef.current} />;
      break;
  }

  const privatePreview =
    priv?.k === 'preview' && view.phase.k === 'DEAL'
      ? {
          id: priv.id,
          content: <PreviewOverlay payload={priv.p} net={net} onClose={() => setPriv(null)} />,
        }
      : null;
  const privateSpotlight =
    spotlight && !spotlight.dismissed
      ? {
          id: spotlight.id,
          content: (
            <SpotlightOverlay
              key={spotlight.assignment.ceremonyId}
              assignment={spotlight.assignment}
              burnPending={spotlight.burnPending}
              net={net}
              onBurn={(at) => {
                const ceremonyId = spotlight.assignment.ceremonyId;
                setSpotlight((current) => requestSpotlightBurn(current, ceremonyId, at));
                net.send({ t: 'BURN', kind: 'spotlight' });
              }}
              onDismiss={() => {
                const ceremonyId = spotlight.assignment.ceremonyId;
                setSpotlight((current) => dismissSpotlight(current, ceremonyId));
              }}
            />
          ),
        }
      : null;
  const privateOverlay = privateSpotlight ?? privatePreview;

  return (
    <>
      {/* At N>=5 the host phone is the face-up STAGE; StageShell shows public faces + a manual
          lift-to-sin flip for private ballots (D-135). A no-op wrapper otherwise. */}
      <StageShell view={view} me={me} net={net} privateOverlay={privateOverlay}>
        {screen}
      </StageShell>
      {me?.role === 'imp' && !impAck && (
        <Overlay title="YOU'RE AN IMP" sub="Feed on their sins. Your name is in the pool, your votes weigh half, and a seat opens at the next circle.">
          <button class="btn-ghost" onClick={() => setImpAck(true)}>
            UNDERSTOOD
          </button>
        </Overlay>
      )}
      {conn === 'open' && err && <RoomErrorNotice key={err.id} error={err} onDismiss={() => setErr(null)} />}
      {conn !== 'open' && <Overlay title="CRAWLING BACK…" sub="The pit remembers you. Hold on." />}
      {toast && <Toast text={toast} />}
    </>
  );
}

render(<App />, document.getElementById('app')!);
