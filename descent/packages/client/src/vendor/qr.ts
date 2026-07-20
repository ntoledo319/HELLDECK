// Minimal QR encoder — byte mode, error correction level M, versions 1–6 (up to 106 chars).
// Hand-rolled for HELLDECK per spec 6.3 (client-generated QR, zero deps, zero network).
// Algorithm per ISO/IEC 18004; structure informed by Project Nayuki's MIT reference
// implementation (nayuki.io/page/qr-code-generator-library). Version <= 6 on purpose:
// join URLs are short, and it keeps the (v>=7) version-info blocks out of the code.

const MAX_VERSION = 6;
const TOTAL_CODEWORDS = [26, 44, 70, 100, 134, 172] as const; // versions 1..6
const EC_PER_BLOCK = [10, 16, 26, 18, 24, 16] as const; // level M
const NUM_BLOCKS = [1, 1, 1, 2, 2, 4] as const; // level M

// ===== GF(256), reduction polynomial 0x11D =====
const EXP = new Uint8Array(512);
const LOG = new Uint8Array(256);
{
  let x = 1;
  for (let i = 0; i < 255; i++) {
    EXP[i] = x;
    LOG[x] = i;
    x <<= 1;
    if (x & 0x100) x ^= 0x11d;
  }
  for (let i = 255; i < 512; i++) EXP[i] = EXP[i - 255]!;
}

function gfMul(a: number, b: number): number {
  return a && b ? EXP[LOG[a]! + LOG[b]!]! : 0;
}

/** Reed–Solomon generator polynomial coefficients (highest degree first, leading 1 dropped). */
export function rsDivisor(degree: number): number[] {
  const result = new Array<number>(degree).fill(0);
  result[degree - 1] = 1;
  let root = 1;
  for (let i = 0; i < degree; i++) {
    for (let j = 0; j < degree; j++) {
      result[j] = gfMul(result[j]!, root);
      if (j + 1 < degree) result[j]! ^= result[j + 1]!;
    }
    root = gfMul(root, 2);
  }
  return result;
}

/** Remainder of data * x^degree mod divisor — the EC codewords. */
export function rsRemainder(data: readonly number[], divisor: readonly number[]): number[] {
  const result = new Array<number>(divisor.length).fill(0);
  for (const b of data) {
    const factor = b ^ result.shift()!;
    result.push(0);
    for (let i = 0; i < divisor.length; i++) result[i]! ^= gfMul(divisor[i]!, factor);
  }
  return result;
}

/** 15-bit format info: 5 data bits (2 EC + 3 mask) + 10 BCH bits, XOR-masked. M = 0b00. */
export function formatBitsFor(ecBits: number, mask: number): number {
  const data = (ecBits << 3) | mask;
  let rem = data;
  for (let i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
  return ((data << 10) | rem) ^ 0x5412;
}

export interface Codewords {
  version: number;
  dataCodewords: number;
  blocks: { data: number[]; ec: number[] }[];
  all: number[]; // interleaved, ready for placement
}

export function buildCodewords(text: string): Codewords {
  const bytes = new TextEncoder().encode(text);
  let version = 0;
  for (let v = 1; v <= MAX_VERSION; v++) {
    const cap = TOTAL_CODEWORDS[v - 1]! - EC_PER_BLOCK[v - 1]! * NUM_BLOCKS[v - 1]! - 2; // mode+len = 2 bytes
    if (bytes.length <= cap) {
      version = v;
      break;
    }
  }
  if (version === 0) throw new Error(`qr: text too long (${bytes.length} bytes)`);
  const dataCodewords = TOTAL_CODEWORDS[version - 1]! - EC_PER_BLOCK[version - 1]! * NUM_BLOCKS[version - 1]!;

  // Bit stream: mode 0100 (byte), 8-bit count (versions 1..9), data, terminator, pads.
  const bits: number[] = [];
  const push = (len: number, val: number): void => {
    for (let i = len - 1; i >= 0; i--) bits.push((val >>> i) & 1);
  };
  push(4, 0b0100);
  push(8, bytes.length);
  for (const b of bytes) push(8, b);
  const capBits = dataCodewords * 8;
  push(Math.min(4, capBits - bits.length), 0);
  while (bits.length % 8 !== 0) bits.push(0);
  for (let i = 0; bits.length < capBits; i++) push(8, i % 2 === 0 ? 0xec : 0x11);

  const data: number[] = [];
  for (let i = 0; i < bits.length; i += 8) {
    let b = 0;
    for (let j = 0; j < 8; j++) b = (b << 1) | bits[i + j]!;
    data.push(b);
  }

  // Split into blocks (all blocks equal size for v1-6 level M), append EC, interleave.
  const nb = NUM_BLOCKS[version - 1]!;
  const ecLen = EC_PER_BLOCK[version - 1]!;
  const per = dataCodewords / nb;
  const divisor = rsDivisor(ecLen);
  const blocks = Array.from({ length: nb }, (_, i) => {
    const d = data.slice(i * per, (i + 1) * per);
    return { data: d, ec: rsRemainder(d, divisor) };
  });
  const all: number[] = [];
  for (let i = 0; i < per; i++) for (const b of blocks) all.push(b.data[i]!);
  for (let i = 0; i < ecLen; i++) for (const b of blocks) all.push(b.ec[i]!);
  return { version, dataCodewords, blocks, all };
}

/** Encode text into a QR module matrix (true = dark). Renderer must add a 4-module quiet zone. */
export function qrEncode(text: string): boolean[][] {
  const { version, all } = buildCodewords(text);
  const size = 17 + 4 * version;
  const modules: boolean[][] = Array.from({ length: size }, () => new Array<boolean>(size).fill(false));
  const isFunction: boolean[][] = Array.from({ length: size }, () => new Array<boolean>(size).fill(false));

  const set = (x: number, y: number, dark: boolean): void => {
    modules[y]![x] = dark;
    isFunction[y]![x] = true;
  };

  // Timing patterns
  for (let i = 0; i < size; i++) {
    set(6, i, i % 2 === 0);
    set(i, 6, i % 2 === 0);
  }
  // Finder patterns (+separators)
  const drawFinder = (cx: number, cy: number): void => {
    for (let dy = -4; dy <= 4; dy++) {
      for (let dx = -4; dx <= 4; dx++) {
        const x = cx + dx;
        const y = cy + dy;
        if (x < 0 || x >= size || y < 0 || y >= size) continue;
        const dist = Math.max(Math.abs(dx), Math.abs(dy));
        set(x, y, dist !== 2 && dist !== 4);
      }
    }
  };
  drawFinder(3, 3);
  drawFinder(size - 4, 3);
  drawFinder(3, size - 4);
  // Alignment pattern (versions 2..6 have exactly one, at (4v+10, 4v+10))
  if (version >= 2) {
    const c = 4 * version + 10;
    for (let dy = -2; dy <= 2; dy++) {
      for (let dx = -2; dx <= 2; dx++) {
        set(c + dx, c + dy, Math.max(Math.abs(dx), Math.abs(dy)) !== 1);
      }
    }
  }

  const drawFormat = (mask: number): void => {
    const fmt = formatBitsFor(0b00 /* level M */, mask);
    const bit = (i: number): boolean => ((fmt >>> i) & 1) !== 0;
    for (let i = 0; i <= 5; i++) set(8, i, bit(i));
    set(8, 7, bit(6));
    set(8, 8, bit(7));
    set(7, 8, bit(8));
    for (let i = 9; i < 15; i++) set(14 - i, 8, bit(i));
    for (let i = 0; i < 8; i++) set(size - 1 - i, 8, bit(i));
    for (let i = 8; i < 15; i++) set(8, size - 15 + i, bit(i));
    set(8, size - 8, true); // the always-dark module
  };
  drawFormat(0); // reserve the cells before data placement

  // Zigzag data placement (skips function modules)
  {
    let i = 0;
    const nBits = all.length * 8;
    for (let right = size - 1; right >= 1; right -= 2) {
      if (right === 6) right = 5;
      for (let vert = 0; vert < size; vert++) {
        for (let j = 0; j < 2; j++) {
          const x = right - j;
          const upward = ((right + 1) & 2) === 0;
          const y = upward ? size - 1 - vert : vert;
          if (!isFunction[y]![x]! && i < nBits) {
            modules[y]![x] = ((all[i >>> 3]! >>> (7 - (i & 7))) & 1) !== 0;
            i++;
          }
        }
      }
    }
  }

  const MASKS: readonly ((x: number, y: number) => boolean)[] = [
    (x, y) => (x + y) % 2 === 0,
    (_x, y) => y % 2 === 0,
    (x) => x % 3 === 0,
    (x, y) => (x + y) % 3 === 0,
    (x, y) => (Math.floor(x / 3) + Math.floor(y / 2)) % 2 === 0,
    (x, y) => ((x * y) % 2) + ((x * y) % 3) === 0,
    (x, y) => (((x * y) % 2) + ((x * y) % 3)) % 2 === 0,
    (x, y) => (((x + y) % 2) + ((x * y) % 3)) % 2 === 0,
  ];
  const applyMask = (m: number): void => {
    const f = MASKS[m]!;
    for (let y = 0; y < size; y++) {
      for (let x = 0; x < size; x++) {
        if (!isFunction[y]![x]! && f(x, y)) modules[y]![x] = !modules[y]![x]!;
      }
    }
  };

  // Pick the mask with the lowest penalty (applying twice is identity — XOR).
  let best = 0;
  let bestScore = Infinity;
  for (let m = 0; m < 8; m++) {
    applyMask(m);
    drawFormat(m);
    const score = penalty(modules);
    if (score < bestScore) {
      bestScore = score;
      best = m;
    }
    applyMask(m);
  }
  applyMask(best);
  drawFormat(best);
  return modules;
}

/** ISO 18004 penalty rules N1..N4 (used only to choose the mask; any mask decodes). */
export function penalty(m: readonly (readonly boolean[])[]): number {
  const size = m.length;
  let result = 0;
  // N1: runs of same color >= 5 in rows and columns
  for (let y = 0; y < size; y++) {
    let runColorRow = m[y]![0]!;
    let runLenRow = 1;
    let runColorCol = m[0]![y]!;
    let runLenCol = 1;
    for (let x = 1; x < size; x++) {
      if (m[y]![x]! === runColorRow) {
        runLenRow++;
        if (runLenRow === 5) result += 3;
        else if (runLenRow > 5) result++;
      } else {
        runColorRow = m[y]![x]!;
        runLenRow = 1;
      }
      if (m[x]![y]! === runColorCol) {
        runLenCol++;
        if (runLenCol === 5) result += 3;
        else if (runLenCol > 5) result++;
      } else {
        runColorCol = m[x]![y]!;
        runLenCol = 1;
      }
    }
  }
  // N2: 2x2 blocks of same color
  for (let y = 0; y < size - 1; y++) {
    for (let x = 0; x < size - 1; x++) {
      const c = m[y]![x]!;
      if (c === m[y]![x + 1]! && c === m[y + 1]![x]! && c === m[y + 1]![x + 1]!) result += 3;
    }
  }
  // N3: finder-like pattern 1011101 with 0000 on either side, rows and columns
  const A = [false, false, false, false, true, false, true, true, true, false, true];
  const B = [true, false, true, true, true, false, true, false, false, false, false];
  const windowMatches = (get: (i: number) => boolean, start: number, pat: boolean[]): boolean => {
    for (let i = 0; i < 11; i++) if (get(start + i) !== pat[i]!) return false;
    return true;
  };
  for (let y = 0; y < size; y++) {
    for (let x = 0; x <= size - 11; x++) {
      const row = (i: number): boolean => m[y]![i]!;
      const col = (i: number): boolean => m[i]![y]!;
      if (windowMatches(row, x, A) || windowMatches(row, x, B)) result += 40;
      if (windowMatches(col, x, A) || windowMatches(col, x, B)) result += 40;
    }
  }
  // N4: dark module proportion deviation from 50%
  let dark = 0;
  for (const row of m) for (const c of row) if (c) dark++;
  const total = size * size;
  result += Math.floor(Math.abs(dark * 20 - total * 10) / total) * 10;
  return result;
}
