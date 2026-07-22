// Routes: "/" landing · "/:code" join + room (spec 6.1). The Room is a thin renderer:
// STATE in, taps out. The server is the only authority; every gate here is a mirror.
import { render, type JSX } from 'preact';
import { useEffect, useRef, useState } from 'preact/hooks';
import { playSting, unlockAudio } from './audio';
import { ConnectionGeneration } from './connection';
import { deviceToken, parseUnlockReturn, verifyAndStore } from './entitle';
import { cleanCodeInput, serverErrorMessage, validCode } from './logic';
import { Net, type ConnStatus } from './net/ws';
import { Overlay, Toast } from './screens/bits';
import { DealScreen, PreviewOverlay } from './screens/deal';
import {
  dismissPreview,
  expirePreview,
  parsePreviewMessage,
  preparePreviewReconnect,
  receivePreviewMessage,
  rejectPreviewBurn,
  requestPreviewBurn,
  type PreviewClientState,
} from './screens/preview.logic';
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
  prepareSpotlightReconnect,
  receiveSpotlightMessage,
  rejectSpotlightBurn,
  requestSpotlightBurn,
  type SpotlightClientState,
} from './screens/spotlight.logic';
import { Paywall } from './screens/paywall';
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
  const [creating, setCreating] = useState(false);
  const go = (): void => {
    const c = cleanCodeInput(code);
    if (validCode(c)) location.href = `/${c}`;
    else setErr('NO SUCH PIT — codes are 4 letters, never vowels.');
  };
  const create = async (): Promise<void> => {
    if (creating) return;
    setCreating(true);
    setErr(null);
    try {
      const response = await fetch('/api/room', { method: 'POST' });
      if (!response.ok) throw new Error(`room create failed: ${response.status}`);
      const payload = (await response.json()) as { code?: unknown };
      if (typeof payload.code !== 'string' || !validCode(payload.code)) throw new Error('invalid room code');
      location.href = `/${payload.code}`;
    } catch {
      setCreating(false);
      setErr('THE PIT IS UNREACHABLE — check your signal.');
    }
  };
  return (
    <main class="screen landing">
      <h1 class="wordmark landing-title">
        HELL<em>DECK</em>
      </h1>
      {err && (
        <div id="landing-error" class="err-banner" role="alert">
          {err}
        </div>
      )}
      <button
        type="button"
        class="btn-blood big"
        disabled={creating}
        aria-busy={creating}
        onClick={() => void create()}
      >
        {creating ? 'OPENING THE PIT…' : 'START A NIGHT'}
      </button>
      <label class="field-label landing-join-label" for="room-code">
        JOIN A NIGHT
      </label>
      <form
        class="code-row"
        onSubmit={(event) => {
          event.preventDefault();
          go();
        }}
      >
        <input
          id="room-code"
          class="code-input"
          maxLength={4}
          placeholder="CODE"
          autocomplete="off"
          autocapitalize="characters"
          spellcheck={false}
          inputMode="text"
          enterKeyHint="go"
          aria-invalid={err?.startsWith('NO SUCH PIT') === true}
          aria-describedby={err ? 'landing-error landing-age' : 'landing-age'}
          value={code}
          onInput={(e) => {
            setErr(null);
            setCode(cleanCodeInput((e.target as HTMLInputElement).value));
          }}
        />
        <button type="submit" class="btn-ghost" disabled={code.length !== 4}>
          JOIN
        </button>
      </form>
      <p id="landing-age" class="landing-tag">
        18+. Bring people you trust.
      </p>
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

function RoomErrorNotice({
  error,
  dismissible,
  onDismiss,
}: {
  error: RoomError;
  dismissible: boolean;
  onDismiss: () => void;
}) {
  return (
    <div class="room-error flash-in">
      <span class="room-error-message" role="alert" aria-live="assertive" aria-atomic="true">
        {error.message}
      </span>
      {dismissible && (
        <button type="button" class="room-error-dismiss" onClick={onDismiss} aria-label="Dismiss error message">
          DISMISS
        </button>
      )}
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
  const [preview, setPreview] = useState<PreviewClientState | null>(null);
  const [spotlight, setSpotlight] = useState<SpotlightClientState | null>(null);
  const [heat, setHeat] = useState(0);
  const [toast, setToast] = useState<string | null>(null);
  const [impAck, setImpAck] = useState(false);
  const [connectionGeneration, setConnectionGeneration] = useState(0);
  const [paywall, setPaywall] = useState(false);
  // The host returns here from Stripe with ?session_id=… — hold the room until the pact is sealed.
  const [unlockReturn, setUnlockReturn] = useState<'verifying' | null>(() =>
    parseUnlockReturn(location.search).kind === 'verify' ? 'verifying' : null,
  );

  const netRef = useRef<Net | null>(null);
  const viewRef = useRef<RoomView | null>(null);
  const phaseKRef = useRef<string>('');
  const prevRoleRef = useRef<string | null>(null);
  const deadlinesRef = useRef(new Map<string, number>());
  const firesRef = useRef(0);
  const closeFailsRef = useRef(0);
  const errorIdRef = useRef(0);
  const privateIdRef = useRef(0);

  // Finish the Stripe round-trip. On success we reload clean so the socket reconnects carrying
  // the freshly-stored unlock (the host is entitled now); on cancel/failure we just clean the URL.
  useEffect(() => {
    const ret = parseUnlockReturn(location.search);
    if (ret.kind === 'verify') {
      void verifyAndStore(ret.sessionId, ret.dev).then((ok) => {
        if (ok) {
          location.replace(`/${code}`);
        } else {
          history.replaceState(null, '', `/${code}`);
          setUnlockReturn(null);
          setToast('THE PACT DIDN’T TAKE — the pit saw no coin. Try the toll again.');
        }
      });
    } else if (ret.kind === 'cancel') {
      history.replaceState(null, '', `/${code}`);
      setToast('YOU BACKED OFF THE LEDGE. The toll’s still there when you are.');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!profile) return;
    keepAwake(); // spec 6.3: wake lock on join, re-acquired on visibilitychange
    const net = new Net(code, {
      onState: (v, ep) => {
        const rv = v as RoomView;
        if (rv.phase.k !== phaseKRef.current) {
          phaseKRef.current = rv.phase.k;
          setPreview(null);
          setSpotlight(null);
          setHeat(0); // heat meter is per-reveal
        }
        viewRef.current = rv;
        setView(rv);
        setEpoch(ep);
      },
      onPrivate: (k, p) => {
        const id = ++privateIdRef.current;
        if (k === 'preview') {
          const message = parsePreviewMessage(p);
          if (message) {
            const now = netRef.current?.serverNow() ?? Date.now();
            setSpotlight(null);
            setPreview((current) => receivePreviewMessage(current, message, id, now));
          }
          return;
        }
        if (k === 'spotlight') {
          const message = parseSpotlightMessage(p);
          if (message) {
            const now = netRef.current?.serverNow() ?? Date.now();
            setPreview(null);
            setSpotlight((current) => receiveSpotlightMessage(current, message, id, now));
          }
          return;
        }
        // Content-private payloads begin only after assignment settles. Clear the
        // role curtain immediately so it cannot mask the next full safety window.
        setSpotlight(null);
        setPreview(null);
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
          // A new socket makes replay authoritative. Connection-aware action locks
          // reset, while ordinary draft state (answers/testimony) stays mounted.
          setConnectionGeneration((generation) => generation + 1);
          setPreview((current) => preparePreviewReconnect(current));
          setSpotlight((current) => prepareSpotlightReconnect(current));
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
        setPreview((current) => rejectPreviewBurn(current));
        setSpotlight((current) => rejectSpotlightBurn(current));
        // A locked second night is a decision, not a transient tap failure: open the toll
        // instead of flashing a banner. Only the host can trigger BEGIN, so only they see it.
        if (c === 'NO_ENTITLEMENT') {
          setPaywall(true);
          return;
        }
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

  const previewId = preview?.assignment.previewId ?? null;
  const previewBurnDeadline = preview?.assignment.burnDeadline ?? null;
  useEffect(() => {
    if (previewId === null || previewBurnDeadline === null) return undefined;
    const expire = (): void => {
      setPreview((current) => expirePreview(current, previewId));
    };
    const remaining = previewBurnDeadline - (netRef.current?.serverNow() ?? Date.now());
    if (remaining <= 0) {
      expire();
      return undefined;
    }
    const timer = setTimeout(expire, remaining);
    return () => clearTimeout(timer);
  }, [previewId, previewBurnDeadline]);

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

  if (unlockReturn === 'verifying') {
    return <Overlay title="SEALING THE PACT…" sub="The pit is counting your coin. Don’t close this." />;
  }

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

  const changeRoom = (): void => {
    netRef.current?.close();
    localStorage.removeItem(profileKey(code));
    localStorage.removeItem(`hd:${code}:token`);
    localStorage.removeItem(`hd:${code}:sealed`);
    location.href = '/';
  };

  const net = netRef.current;
  if (!view || !net) {
    const lost = closeFailsRef.current >= 3;
    return (
      <Overlay
        title={lost ? 'NO SUCH PIT?' : 'OPENING THE PIT…'}
        sub={lost ? 'The room may have burned down. Check the code or try another.' : undefined}
      >
        {lost && (
          <button class="btn-ghost" onClick={changeRoom}>
            TRY ANOTHER CODE
          </button>
        )}
      </Overlay>
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
    preview && !preview.dismissed && view.phase.k === 'DEAL'
      ? {
          id: preview.id,
          expiresAt: preview.assignment.burnDeadline,
          content: (
            <PreviewOverlay
              key={preview.assignment.previewId}
              assignment={preview.assignment}
              burnPending={preview.burnPending}
              net={net}
              onBurn={(at) => {
                const id = preview.assignment.previewId;
                if (
                  !preview.assignment.canBurn ||
                  preview.burnPending ||
                  preview.assignment.burnDeadline <= at ||
                  !net.send({ t: 'BURN', kind: 'card' })
                ) {
                  return false;
                }
                setPreview((current) => requestPreviewBurn(current, id, at));
                return true;
              }}
              onDismiss={() => {
                const id = preview.assignment.previewId;
                setPreview((current) => dismissPreview(current, id));
              }}
            />
          ),
        }
      : null;
  const privateSpotlight =
    spotlight && !spotlight.dismissed
      ? {
          id: spotlight.id,
          expiresAt: spotlight.assignment.burnDeadline,
          content: (
            <SpotlightOverlay
              key={spotlight.assignment.ceremonyId}
              assignment={spotlight.assignment}
              burnPending={spotlight.burnPending}
              net={net}
              onBurn={(at) => {
                const ceremonyId = spotlight.assignment.ceremonyId;
                if (
                  !spotlight.assignment.canBurn ||
                  spotlight.burnPending ||
                  spotlight.assignment.burnDeadline <= at ||
                  !net.send({ t: 'BURN', kind: 'spotlight' })
                ) {
                  return false;
                }
                setSpotlight((current) => requestSpotlightBurn(current, ceremonyId, at));
                return true;
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
  const activePrivateOverlay = conn === 'open' ? privateOverlay : null;
  const impModalOpen = conn === 'open' && me?.role === 'imp' && !impAck;

  return (
    <>
      {/* At N>=5 the host phone is the face-up STAGE; StageShell shows public faces + a manual
          lift-to-sin flip for private ballots (D-135). A no-op wrapper otherwise. */}
      <ConnectionGeneration.Provider value={connectionGeneration}>
        <StageShell view={view} me={me} net={net} privateOverlay={activePrivateOverlay}>
          {screen}
        </StageShell>
      </ConnectionGeneration.Provider>
      {impModalOpen && (
        <Overlay title="YOU'RE AN IMP" sub="Feed on their sins. Your name is in the pool, your votes weigh half, and a seat opens at the next circle.">
          <button class="btn-ghost" onClick={() => setImpAck(true)}>
            UNDERSTOOD
          </button>
        </Overlay>
      )}
      {paywall && (
        <Paywall device={deviceToken()} returnPath={`/${code}`} onDismiss={() => setPaywall(false)} />
      )}
      {conn === 'open' && err && (
        <RoomErrorNotice
          key={err.id}
          error={err}
          dismissible={activePrivateOverlay === null && !impModalOpen}
          onDismiss={() => setErr(null)}
        />
      )}
      {conn !== 'open' && (
        <Overlay
          title={closeFailsRef.current >= 3 ? 'STILL CRAWLING…' : 'CRAWLING BACK…'}
          sub={
            closeFailsRef.current >= 3
              ? 'The signal is not coming back. You can keep waiting or change rooms.'
              : 'The pit remembers you. Hold on.'
          }
        >
          {closeFailsRef.current >= 3 && (
            <button class="btn-ghost" onClick={changeRoom}>
              CHANGE ROOM
            </button>
          )}
        </Overlay>
      )}
      {toast && <Toast text={toast} />}
    </>
  );
}

render(<App />, document.getElementById('app')!);
