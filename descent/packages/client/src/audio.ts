// Host-phone stings (spec 3.2 AUDIO — host honors, others ignore). Synthesized in WebAudio:
// zero asset bytes against the 200KB budget. Unlock on first tap (spec 6.3).
let ctx: AudioContext | null = null;

export function unlockAudio(): void {
  if (!ctx) {
    const AC = window.AudioContext ?? (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!AC) return;
    ctx = new AC();
  }
  if (ctx.state === 'suspended') void ctx.resume();
}

function tone(f0: number, f1: number, dur: number, type: OscillatorType, peak = 0.3, when = 0): void {
  if (!ctx) return;
  const t0 = ctx.currentTime + when;
  const osc = ctx.createOscillator();
  const g = ctx.createGain();
  osc.type = type;
  osc.frequency.setValueAtTime(f0, t0);
  osc.frequency.exponentialRampToValueAtTime(Math.max(30, f1), t0 + dur);
  g.gain.setValueAtTime(peak, t0);
  g.gain.exponentialRampToValueAtTime(0.001, t0 + dur);
  osc.connect(g).connect(ctx.destination);
  osc.start(t0);
  osc.stop(t0 + dur + 0.05);
}

export function playSting(name: string): void {
  if (!ctx || ctx.state !== 'running') return;
  switch (name) {
    case 'boom': // Scatterblast detonation: sub-drop with a body
      tone(120, 35, 0.8, 'sine', 0.6);
      tone(80, 30, 0.5, 'triangle', 0.4);
      break;
    case 'bell': // Poison Pitch round bell
      tone(880, 860, 1.1, 'sine', 0.25);
      tone(1760, 1720, 0.7, 'sine', 0.1);
      break;
    case 'burn': // a card dies
      tone(400, 60, 0.35, 'sawtooth', 0.22);
      break;
    case 'descend': // going deeper
      tone(300, 55, 1.4, 'triangle', 0.35);
      break;
    case 'judgment': // the gavel chord
      tone(220, 218, 1.5, 'square', 0.14);
      tone(277, 275, 1.5, 'square', 0.1, 0.12);
      tone(330, 328, 1.5, 'square', 0.1, 0.24);
      break;
    default:
      tone(500, 200, 0.2, 'square', 0.18);
  }
}
