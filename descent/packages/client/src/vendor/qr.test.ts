// QR encoder verification: Reed–Solomon math properties, known-answer format bits,
// and structural invariants of the emitted matrix (finders, timing, format copies).
import { describe, expect, it } from 'vitest';
import { buildCodewords, formatBitsFor, qrEncode, rsDivisor, rsRemainder } from './qr';

const URL_SAMPLE = 'https://helldeck-beta.example.workers.dev/HRLM';

describe('reed-solomon', () => {
  it('data+ec codewords are divisible by the generator (remainder zero)', () => {
    for (const text of ['HELL', URL_SAMPLE, 'x'.repeat(100)]) {
      const { blocks } = buildCodewords(text);
      for (const b of blocks) {
        const divisor = rsDivisor(b.ec.length);
        const rem = rsRemainder([...b.data, ...b.ec], divisor);
        expect(rem.every((v) => v === 0)).toBe(true);
      }
    }
  });

  it('generator polynomial for degree 10 matches the published constants', () => {
    // ISO 18004 g(x) degree-10 coefficients as alpha exponents: 251,67,46,61,118,70,64,94,32,45
    const alphaExp = [251, 67, 46, 61, 118, 70, 64, 94, 32, 45];
    const EXP: number[] = [];
    let x = 1;
    for (let i = 0; i < 255; i++) {
      EXP.push(x);
      x <<= 1;
      if (x & 0x100) x ^= 0x11d;
    }
    expect(rsDivisor(10)).toEqual(alphaExp.map((e) => EXP[e]!));
  });
});

describe('format info', () => {
  it('matches known answers (M-0 is the XOR mask itself; L-0 is the textbook example)', () => {
    expect(formatBitsFor(0b00, 0)).toBe(0b101010000010010); // M, mask 0
    expect(formatBitsFor(0b01, 0)).toBe(0b111011111000100); // L, mask 0
  });

  it('every (M, mask) format code passes the BCH check after unmasking', () => {
    for (let mask = 0; mask < 8; mask++) {
      const unmasked = formatBitsFor(0b00, mask) ^ 0x5412;
      const data = unmasked >>> 10;
      let rem = data;
      for (let i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
      expect(((data << 10) | rem) >>> 0).toBe(unmasked >>> 0);
      expect(data >>> 3).toBe(0b00); // EC level M
      expect(data & 7).toBe(mask);
    }
  });
});

describe('codeword assembly', () => {
  it('selects the smallest version that fits (byte capacity = dataCW - 2)', () => {
    expect(buildCodewords('x'.repeat(14)).version).toBe(1); // v1-M: 16 data cw
    expect(buildCodewords('x'.repeat(15)).version).toBe(2);
    expect(buildCodewords('x'.repeat(42)).version).toBe(3); // v3-M: 44 data cw
    expect(buildCodewords('x'.repeat(106)).version).toBe(6);
  });

  it('throws when the text exceeds version 6 capacity', () => {
    expect(() => buildCodewords('x'.repeat(107))).toThrow(/too long/);
  });

  it('interleaved stream length equals the total codeword count', () => {
    expect(buildCodewords('HELL').all).toHaveLength(26); // v1
    expect(buildCodewords('x'.repeat(100)).all).toHaveLength(172); // v6: 4 blocks
  });

  it('pads with alternating 0xEC/0x11', () => {
    const { blocks } = buildCodewords('A'); // 1 byte -> heavy padding in v1
    const data = blocks[0]!.data;
    // mode+len+data = 3 bytes, terminator fits inside byte 3; pads from index 3
    expect(data.slice(3, 7)).toEqual([0xec, 0x11, 0xec, 0x11]);
  });
});

describe('matrix structure', () => {
  const m = qrEncode(URL_SAMPLE);
  const size = m.length;

  it('is square with size 17+4v', () => {
    expect((size - 17) % 4).toBe(0);
    for (const row of m) expect(row).toHaveLength(size);
  });

  it('has the three finder patterns with separators', () => {
    for (const [cx, cy] of [
      [3, 3],
      [size - 4, 3],
      [3, size - 4],
    ] as const) {
      expect(m[cy]![cx]).toBe(true); // center dark
      expect(m[cy - 1]![cx]).toBe(true); // inner 3x3
      expect(m[cy - 2]![cx]).toBe(false); // light ring
      expect(m[cy - 3]![cx]).toBe(true); // dark ring
      if (cy - 4 >= 0) expect(m[cy - 4]![cx]).toBe(false); // separator
    }
  });

  it('has alternating timing patterns on row/col 6', () => {
    for (let i = 8; i < size - 8; i++) {
      expect(m[6]![i]).toBe(i % 2 === 0);
      expect(m[i]![6]).toBe(i % 2 === 0);
    }
  });

  it('has the always-dark module', () => {
    expect(m[size - 8]![8]).toBe(true);
  });

  it('carries a valid, consistent format info in both copies', () => {
    const read1: boolean[] = [];
    for (let i = 0; i <= 5; i++) read1.push(m[i]![8]!);
    read1.push(m[7]![8]!, m[8]![8]!, m[8]![7]!);
    for (let i = 9; i < 15; i++) read1.push(m[8]![14 - i]!);
    const read2: boolean[] = [];
    for (let i = 0; i < 8; i++) read2.push(m[8]![size - 1 - i]!);
    for (let i = 8; i < 15; i++) read2.push(m[size - 15 + i]![8]!);
    expect(read2).toEqual(read1);
    const value = read1.reduce((acc, b, i) => acc | ((b ? 1 : 0) << i), 0);
    const unmasked = value ^ 0x5412;
    const data = unmasked >>> 10;
    let rem = data;
    for (let i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
    expect((data << 10) | rem).toBe(unmasked); // BCH-valid
    expect(data >>> 3).toBe(0b00); // level M
    expect(data & 7).toBeLessThan(8);
  });

  it('is deterministic', () => {
    expect(qrEncode(URL_SAMPLE)).toEqual(m);
  });

  it('encodes a short LAN dev URL without throwing', () => {
    expect(() => qrEncode('http://192.168.1.20:5173/BCDF')).not.toThrow();
  });
});
