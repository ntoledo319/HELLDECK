// Judgment share card — canvas-rendered PNG (spec 6.1): night stats + wordmark +
// "HOST YOUR OWN DESCENT" + url. Web Share API with download fallback (spec 9.3 loop).
export interface ShareCardData {
  winnerName: string;
  superlatives: { name: string; title: string }[];
  stats: { players: number; circles: number; fires: number };
  url: string;
}

const W = 1080;
const H = 1920;
const PIT = '#0B0A0C';
const BLOOD = '#8E1B1B';
const EMBER = '#E2571B';
const BONE = '#E8E2D6';
const ASH = '#4A4544';
const DISPLAY = "'Avenir Next Condensed','Arial Narrow','Roboto Condensed','Helvetica Neue',system-ui,sans-serif";

export function drawShareCard(canvas: HTMLCanvasElement, d: ShareCardData): void {
  canvas.width = W;
  canvas.height = H;
  const c = canvas.getContext('2d');
  if (!c) throw new Error('no 2d context');

  c.fillStyle = PIT;
  c.fillRect(0, 0, W, H);
  // Ember glow rising from the floor of the pit — heat, not decoration.
  const glow = c.createRadialGradient(W / 2, H + 200, 120, W / 2, H + 200, 1100);
  glow.addColorStop(0, 'rgba(226,87,27,0.4)');
  glow.addColorStop(1, 'rgba(226,87,27,0)');
  c.fillStyle = glow;
  c.fillRect(0, H - 1100, W, 1100);

  // Wordmark: HELL bone / DECK ember
  c.font = `900 128px ${DISPLAY}`;
  c.textAlign = 'left';
  const wHell = c.measureText('HELL').width;
  const wDeck = c.measureText('DECK').width;
  let x = (W - wHell - wDeck) / 2;
  c.fillStyle = BONE;
  c.fillText('HELL', x, 210);
  c.fillStyle = EMBER;
  c.fillText('DECK', x + wHell, 210);

  c.textAlign = 'center';
  c.fillStyle = ASH;
  c.font = `700 40px ${DISPLAY}`;
  c.fillText('T H E   D E S C E N T   I S   C O M P L E T E', W / 2, 300);

  hairline(c, 380);

  c.fillStyle = EMBER;
  c.font = `800 44px ${DISPLAY}`;
  c.fillText("TONIGHT'S DEVIL", W / 2, 480);

  // Winner name in lights — shrink to fit
  let px = 170;
  c.font = `900 ${px}px ${DISPLAY}`;
  while (px > 60 && c.measureText(d.winnerName).width > W - 120) {
    px -= 10;
    c.font = `900 ${px}px ${DISPLAY}`;
  }
  c.shadowColor = EMBER;
  c.shadowBlur = 60;
  c.fillStyle = BONE;
  c.fillText(d.winnerName, W / 2, 650);
  c.shadowBlur = 0;

  hairline(c, 740);

  // Superlatives (up to 6)
  let y = 850;
  for (const s of d.superlatives.slice(0, 6)) {
    c.fillStyle = EMBER;
    c.font = `800 42px ${DISPLAY}`;
    c.fillText(s.title.toUpperCase(), W / 2, y);
    c.fillStyle = BONE;
    c.font = `900 60px ${DISPLAY}`;
    c.fillText(s.name, W / 2, y + 64);
    y += 150;
  }

  // Night stats
  const statsY = Math.max(y + 40, 1500);
  c.fillStyle = BLOOD;
  c.fillRect(W / 2 - 420, statsY - 58, 840, 96);
  c.fillStyle = BONE;
  c.font = `800 44px ${DISPLAY}`;
  c.fillText(
    `${d.stats.players} SINNERS  ·  ${d.stats.circles} CIRCLES  ·  ${d.stats.fires} FIRES`,
    W / 2,
    statsY + 6,
  );

  // The hook
  c.fillStyle = EMBER;
  c.font = `900 64px ${DISPLAY}`;
  c.fillText('HOST YOUR OWN DESCENT', W / 2, H - 180);
  c.fillStyle = BONE;
  c.font = `700 46px ${DISPLAY}`;
  c.fillText(d.url.replace(/^https?:\/\//, ''), W / 2, H - 100);
}

function hairline(c: CanvasRenderingContext2D, y: number): void {
  c.fillStyle = ASH;
  c.fillRect(120, y, W - 240, 2);
}

export async function shareCard(d: ShareCardData): Promise<'shared' | 'downloaded'> {
  const canvas = document.createElement('canvas');
  drawShareCard(canvas, d);
  const blob = await new Promise<Blob | null>((res) => canvas.toBlob(res, 'image/png'));
  if (!blob) throw new Error('canvas export failed');
  const file = new File([blob], 'helldeck-descent.png', { type: 'image/png' });
  const nav = navigator as Navigator & { canShare?: (data: ShareData) => boolean };
  if (typeof navigator.share === 'function' && nav.canShare?.({ files: [file] })) {
    try {
      await navigator.share({ files: [file], title: 'HELLDECK' });
      return 'shared';
    } catch {
      /* user dismissed the sheet — fall through to download */
    }
  }
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'helldeck-descent.png';
  a.click();
  setTimeout(() => URL.revokeObjectURL(a.href), 10_000);
  return 'downloaded';
}
