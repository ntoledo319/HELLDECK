// A socket write is only an optimistic local action until the next authoritative
// STATE. Keep that fast feedback during a healthy connection, but ignore it after
// a reconnect so a frame lost in flight can always be retried. Draft-bearing state
// deliberately keeps using ordinary useState and survives the same reconnect.
import { createContext } from 'preact';
import { useContext, useState } from 'preact/hooks';

export const ConnectionGeneration = createContext(0);

interface OptimisticEntry<T> {
  generation: number;
  value: T;
}

export function useConnectionOptimistic<T>(fallback: T): [T, (next: T) => void] {
  const generation = useContext(ConnectionGeneration);
  const [entry, setEntry] = useState<OptimisticEntry<T>>(() => ({ generation, value: fallback }));
  const value = entry.generation === generation ? entry.value : fallback;
  const setValue = (next: T): void => setEntry({ generation, value: next });
  return [value, setValue];
}
