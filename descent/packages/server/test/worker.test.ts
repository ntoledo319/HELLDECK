import { describe, expect, it } from 'vitest';
import { verifyUnlock } from '../src/entitle.js';
import worker, { type Env } from '../src/worker.js';

const DEVICE = 'workerrouteclient0001';
const SECRET = 'worker-route-test-secret';

function env(overrides: Partial<Env> = {}): Env {
  return {
    ROOM: {} as DurableObjectNamespace,
    LEDGER: {} as DurableObjectNamespace,
    ASSETS: { fetch: async () => new Response('asset') } as unknown as Fetcher,
    ENV: 'production',
    UNLOCK_SECRET: SECRET,
    ...overrides,
  };
}

describe('worker development-unlock boundary', () => {
  it('is hidden for production, staging, missing, and misspelled environments', async () => {
    for (const environment of ['production', 'staging', '', 'development']) {
      const response = await worker.fetch(
        new Request(`https://hell.test/api/entitle/dev-unlock?dev=${DEVICE}`, { method: 'POST' }),
        env({ ENV: environment }),
      );
      expect(response.status, environment).toBe(404);
    }
  });

  it('mints a device-bound token only for explicit dev with a signing secret', async () => {
    const response = await worker.fetch(
      new Request(`https://hell.test/api/entitle/dev-unlock?dev=${DEVICE}`, { method: 'POST' }),
      env({ ENV: 'dev' }),
    );
    expect(response.status).toBe(200);
    const body = (await response.json()) as { unlock: string };
    expect(await verifyUnlock(SECRET, DEVICE, body.unlock)).toBe(true);

    const noSecret = await worker.fetch(
      new Request(`https://hell.test/api/entitle/dev-unlock?dev=${DEVICE}`, { method: 'POST' }),
      env({ ENV: 'dev', UNLOCK_SECRET: undefined }),
    );
    expect(noSecret.status).toBe(404);
  });

  it('advertises dev unlock on checkout failure only when the route can actually mint it', async () => {
    const checkout = (environment: string, secret: string | undefined): Promise<Response> =>
      worker.fetch(
        new Request('https://hell.test/api/entitle/checkout', {
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify({ device: DEVICE }),
        }),
        env({ ENV: environment, UNLOCK_SECRET: secret }),
      );

    expect(await (await checkout('dev', SECRET)).json()).toMatchObject({ devUnlock: true });
    expect(await (await checkout('dev', undefined)).json()).toMatchObject({ devUnlock: false });
    expect(await (await checkout('production', SECRET)).json()).toMatchObject({ devUnlock: false });
  });
});
