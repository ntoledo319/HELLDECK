// bots.ts — N headless WS bots play a FULL mixed night against a live server.
// Spec Part 11 ("bot nights"). Each bot is a thin renderer: it reads its own
// per-socket redacted gameView and taps what a real phone would, via the shared
// botlogic.botMoves decision function — the SAME code the protocol.test.ts unit
// night drives. So this script is the live twin of that test: it exercises every
// registered game end-to-end (all nine shipping decks at depth 9, including both
// BLOCKING truth inputs and the core-owned private spotlight ceremonies).
//
// RUN (needs `.js`->`.ts` import resolution, so use tsx, not bare node):
//   terminal 1:  cd packages/server && pnpm dev            # wrangler dev on http://127.0.0.1:8787
//   terminal 2:  npx tsx packages/server/test/bots.ts      # (or: node --import tsx packages/server/test/bots.ts)
//                env: HELLDECK_URL=... HELLDECK_N=6 HELLDECK_DEPTH=9
//
// PASSES (exit 0) when the room reaches JUDGMENT; exits 1 with a phase dump on timeout.
// Depth 9 exercises all nine games; depths 5/7 are faster smoke runs.
import { botMoves, deckOf, subOf, type RosterMember, type WireMsg } from './botlogic.js';

declare const process: {
  env: Record<string, string | undefined>;
  exit(code: number): never;
};

const BASE = process.env['HELLDECK_URL'] ?? 'http://127.0.0.1:8787';
const WS_BASE = BASE.replace(/^http/, 'ws');
const N = Math.max(3, Math.min(12, Number(process.env['HELLDECK_N']) || 6));
const DEPTH = ([5, 7, 9].includes(Number(process.env['HELLDECK_DEPTH'])) ? Number(process.env['HELLDECK_DEPTH']) : 9) as 5 | 7 | 9;
// A depth-9 night is long by design (ceremonies, 20s+ reveal holds the host cuts short).
const TIMEOUT_MS = DEPTH === 9 ? 900_000 : 480_000;
const POOL = ['HOST', 'ASH', 'BLAZE', 'CINDER', 'EMBER', 'SOOT', 'GRIME', 'RASH', 'MOLD', 'BILE', 'SCAB', 'CRUD'];
const NAMES = POOL.slice(0, N);

type Frame = { t: string; [k: string]: unknown };
type StateView = {
  you: string;
  config: unknown;
  players: RosterMember[];
  gameView: unknown;
  phase: { k: string; sub?: string; circle?: number; deadline?: number | null };
};

class Bot {
  ws!: WebSocket;
  view: StateView | null = null;
  epoch = -1;
  joined = false;
  consented = false;
  lastBeginAt = 0;
  readonly name: string;
  readonly isHost: boolean;

  // no TS parameter properties: keep the file friendly to strip-only TS runners
  constructor(name: string, isHost: boolean) {
    this.name = name;
    this.isHost = isHost;
  }

  connect(code: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(`${WS_BASE}/ws/${code}?v=1`);
      this.ws.addEventListener('open', () => resolve());
      this.ws.addEventListener('error', () => reject(new Error(`${this.name}: ws error`)));
      this.ws.addEventListener('message', (ev) => this.onFrame(JSON.parse(String((ev as MessageEvent).data)) as Frame));
    });
  }

  send(msg: Record<string, unknown>): void {
    if (this.ws.readyState === WebSocket.OPEN) this.ws.send(JSON.stringify(msg));
  }

  private onFrame(f: Frame): void {
    switch (f.t) {
      case 'PING':
        this.send({ t: 'PONG', id: f['id'], cl: Date.now() }); // answer every PING immediately (3.1)
        break;
      case 'STATE': {
        const epoch = Number(f['epoch']);
        if (epoch < this.epoch) return; // stale
        this.epoch = epoch;
        this.view = f['s'] as StateView;
        break;
      }
      case 'ERR':
        console.log(`[${this.name}] ERR ${String(f['code'])}: ${String(f['msg'])}`);
        break;
      default:
        break; // WELCOME/AT/AUDIO/HEAT/PRIVATE: fine, ignored by these bots
    }
  }

  /** One decision tick. Returns true once this bot has seen JUDGMENT. */
  tick(): boolean {
    const v = this.view;
    if (!v) return false;
    const phase = v.phase?.k ?? 'LOBBY';
    const meIn = v.players.some((p) => p.id === v.you);

    if (!this.joined) {
      this.send({ t: 'JOIN', name: this.name, avatar: Math.floor(Math.random() * 16) });
      this.joined = true;
      return false;
    }
    if (!this.consented && meIn) {
      this.send({ t: 'CEILING', v: 5 }); // ceiling high so the deck has room to climb (invisible either way)
      this.send({ t: 'ATTEST18' });
      this.consented = true;
    }

    if (phase === 'LOBBY' || phase === 'CONSENT') {
      if (this.isHost) {
        const everyoneIn = v.players.length >= NAMES.length;
        if (everyoneIn && !v.config) this.send({ t: 'CONFIG', depth: DEPTH, vibe: 'feral', stage: N >= 5 });
        if (everyoneIn && v.config && Date.now() - this.lastBeginAt > 2000) {
          this.send({ t: 'BEGIN' }); // retried until the phase moves
          this.lastBeginAt = Date.now();
        }
      }
      return false;
    }

    // In a circle: every action is a decision off THIS bot's own redacted gameView —
    // exactly what a real phone sees. botMoves covers every registered game and both
    // blocking inputs; PRIVATE notices need no bot action unless a burn is being tested.
    for (const m of botMoves(v.gameView, v.you, v.players, this.isHost) as WireMsg[]) this.send(m);

    return phase === 'JUDGMENT';
  }
}

const sleep = (ms: number): Promise<void> => new Promise((r) => setTimeout(r, ms));

async function main(): Promise<void> {
  console.log(`creating room at ${BASE} — N=${N} depth=${DEPTH}`);
  const res = await fetch(`${BASE}/api/room`, { method: 'POST' });
  if (!res.ok) throw new Error(`POST /api/room -> ${res.status}`);
  const { code } = (await res.json()) as { code: string };
  console.log(`room ${code} — connecting ${NAMES.length} bots`);

  const bots = NAMES.map((n, i) => new Bot(n, i === 0));
  for (const b of bots) await b.connect(code); // sequential: host joins first -> seat 0 (host role)

  const startedAt = Date.now();
  let lastLogged = '';
  while (Date.now() - startedAt < TIMEOUT_MS) {
    const done = bots.map((b) => b.tick());
    const host = bots[0]?.view;
    const gv = host?.gameView;
    const tag = `${host?.phase?.k ?? '?'}${gv ? ` ${deckOf(gv) ?? ''}/${subOf(gv) ?? ''}` : ''} c${host?.phase?.circle ?? '?'}`;
    if (tag !== lastLogged) {
      console.log(`-> ${tag}`);
      lastLogged = tag;
    }
    if (done.every(Boolean)) {
      console.log(`JUDGMENT reached in ${((Date.now() - startedAt) / 1000).toFixed(1)}s — PASS`);
      process.exit(0);
    }
    await sleep(400);
  }

  console.error('TIMEOUT — room never reached JUDGMENT. Last known views:');
  for (const b of bots) {
    const gv = b.view?.gameView;
    console.error(`  ${b.name}: phase=${b.view?.phase?.k ?? 'none'} game=${gv ? `${deckOf(gv)}/${subOf(gv)}` : '-'} epoch=${b.epoch}`);
  }
  process.exit(1);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
