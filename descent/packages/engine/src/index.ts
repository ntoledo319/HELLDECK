export * from './types.js';
export * from './engine.js';
export * from './arc.js';
export * from './consent.js';
export * from './deal.js';
export * from './spotlight.js';
export * from './scoring.js';
export { rng, pick } from './rng.js';
export type { GameModule, GameCtx, GameStep } from './games/module.js';

// Deck injection (D-127): the Worker loads the real corpus into each module at startup.
export { setRoastDeck } from './games/roast.js';
export { setFillinDeck, type FillinCard } from './games/fillin.js';
export { setOverUnderDeck } from './games/overunder.js';
export { setConfessionDeck } from './games/confession.js';
export { setScatterDeck } from './games/scatter.js';
export { setPoisonDeck } from './games/poison.js';
export { setRedflagDeck } from './games/redflag.js';
export { setAlibiDeck } from './games/alibi.js';
export { setTitleFightDeck } from './games/titlefight.js';
