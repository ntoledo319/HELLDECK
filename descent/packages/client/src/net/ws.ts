// Socket wrapper: reconnect (0.5s→8s backoff), epoch discipline, clock offset, RESYNC on visible.
// Spec 3.1/3.3/3.5 + 6.3. Countdowns everywhere derive from serverNow() — never local intervals.
import { deviceToken, unlockToken } from '../entitle';
import { median } from '../logic';
import { FireCoalescer } from './coalesce';

export type ConnStatus = 'connecting' | 'open' | 'closed';
export const HEARTBEAT_MS = 15_000;

export interface NetHandlers {
  onState(view: unknown, epoch: number): void;
  onPrivate(kind: string, payload: unknown): void;
  onAudio(sting: string): void;
  onHeat(n: number): void;
  onDeadline(timerId: string, atServerMs: number): void;
  onStatus(s: ConnStatus): void;
  onWelcome(you: string): void;
  onError(code: string, msg: string): void;
}

export class Net {
  private ws: WebSocket | null = null;
  private epoch = -1;
  private offset = 0; // serverTime - clientTime, median of PING samples (spec 3.3)
  private offsetSamples: number[] = [];
  private backoff = 500;
  private closed = false;
  private fires: FireCoalescer;
  private fireTimer: ReturnType<typeof setTimeout> | null = null;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private readonly onVisible = (): void => {
    if (document.visibilityState === 'visible') this.send({ t: 'RESYNC' });
  };

  constructor(
    private code: string,
    private h: NetHandlers,
  ) {
    this.fires = new FireCoalescer((n) => this.send({ t: 'FIRE', n }));
    document.addEventListener('visibilitychange', this.onVisible);
  }

  get token(): string {
    const k = `hd:${this.code}:token`;
    let t = localStorage.getItem(k);
    if (!t) {
      t = crypto.randomUUID().replaceAll('-', '').slice(0, 12);
      localStorage.setItem(k, t);
    }
    return t;
  }

  connect(): void {
    this.closed = false;
    this.stopHeartbeat();
    this.h.onStatus('connecting');
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    // The host's device + unlock tokens ride this phone's OWN socket so the server can resolve
    // entitlement at BEGIN (spec Part 11). Read fresh each connect so a just-purchased unlock is
    // picked up on the next reconnect. Harmless for joiners — only the host's are ever consulted.
    const unlock = unlockToken();
    const params = `token=${this.token}&v=1&dev=${deviceToken()}${unlock ? `&unlock=${encodeURIComponent(unlock)}` : ''}`;
    this.ws = new WebSocket(`${proto}://${location.host}/ws/${this.code}?${params}`);
    this.ws.onopen = () => {
      this.backoff = 500;
      this.startHeartbeat();
      this.h.onStatus('open');
    };
    this.ws.onclose = () => {
      this.stopHeartbeat();
      this.h.onStatus('closed');
      if (!this.closed) {
        setTimeout(() => {
          if (!this.closed) this.connect(); // a close() during backoff must stick
        }, this.backoff);
        this.backoff = Math.min(this.backoff * 2, 8000);
      }
    };
    this.ws.onmessage = (ev) => this.handleMessage(JSON.parse(ev.data as string));
  }

  close(): void {
    this.closed = true;
    this.stopHeartbeat();
    document.removeEventListener('visibilitychange', this.onVisible);
    if (this.fireTimer) clearTimeout(this.fireTimer);
    this.ws?.close();
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.heartbeatTimer = setInterval(() => this.send({ t: 'HEARTBEAT' }), HEARTBEAT_MS);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer === null) return;
    clearInterval(this.heartbeatTimer);
    this.heartbeatTimer = null;
  }

  send(msg: Record<string, unknown>): boolean {
    if (this.ws?.readyState !== WebSocket.OPEN) return false;
    try {
      this.ws.send(JSON.stringify(msg));
      return true;
    } catch {
      return false;
    }
  }

  /** Fire-tap: coalesced to at most one wire message per 500ms (spec 3.2). */
  fire(n = 1): void {
    this.fires.tap(Date.now(), n);
    if (!this.fireTimer) {
      this.fireTimer = setTimeout(() => {
        this.fireTimer = null;
        this.fires.tick(Date.now());
      }, this.fires.intervalMs + 20);
    }
  }

  serverNow(): number {
    return Date.now() + this.offset;
  }

  /** Wire entry point (spec 3.2 server→client messages). Public so the routing rules are testable. */
  handleMessage(m: { t: string; [k: string]: unknown }): void {
    switch (m.t) {
      case 'WELCOME':
        // WELCOME is guaranteed to precede STATE/PRIVATE. Seed offset immediately
        // so a freshly loaded phone cannot discard a live safety window because its
        // wall clock is skewed; the 5-sample PING batch refines this estimate next.
        if (typeof m['sv'] === 'number' && Number.isFinite(m['sv'])) {
          const sample = m['sv'] - Date.now();
          this.offsetSamples = [sample];
          this.offset = sample;
        }
        this.h.onWelcome(String(m['you'] ?? ''));
        break;
      case 'STATE': {
        const epoch = Number(m['epoch']);
        if (epoch < this.epoch) return; // stale; equal = full resync, always accept
        this.epoch = epoch;
        this.h.onState(m['s'], epoch);
        break;
      }
      // PATCH (RFC6902-lite) lands with the server delta work; full STATE is authoritative until then.
      case 'PRIVATE':
        // Secrets ride per-socket only (spec 3.4): preview cards, roles, alibi/taboo words.
        this.h.onPrivate(String(m['k'] ?? ''), m['p']);
        break;
      case 'PING': {
        // One-way offset sample; latency bias is well under countdown tolerance (spec 3.3).
        const sv = Number(m['sv']);
        if (Number.isFinite(sv)) {
          this.offsetSamples = [...this.offsetSamples.slice(-4), sv - Date.now()];
          this.offset = median(this.offsetSamples);
        }
        this.send({ t: 'PONG', id: m['id'], cl: Date.now() });
        break;
      }
      case 'AT':
        this.h.onDeadline(String(m['timerId']), Number(m['at']));
        break;
      case 'AUDIO':
        this.h.onAudio(String(m['sting']));
        break;
      case 'HEAT':
        this.h.onHeat(Number(m['n']));
        break;
      case 'ERR':
        this.h.onError(String(m['code'] ?? ''), String(m['msg'] ?? ''));
        break;
    }
  }
}
