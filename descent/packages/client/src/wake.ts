// Wake Lock (spec 6.3): acquire on join, re-acquire on visibilitychange.
// Failure is non-fatal — the game survives a dimming phone; it just shouldn't happen.
interface WakeLockLike {
  request(type: 'screen'): Promise<unknown>;
}

let installed = false;

async function acquire(): Promise<void> {
  try {
    const wl = (navigator as Navigator & { wakeLock?: WakeLockLike }).wakeLock;
    if (wl && document.visibilityState === 'visible') await wl.request('screen');
  } catch {
    /* denied (low battery / unsupported) — nothing to do */
  }
}

export function keepAwake(): void {
  if (installed) return;
  installed = true;
  void acquire();
  // The UA releases the sentinel on hide; re-request every time we surface.
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') void acquire();
  });
}
