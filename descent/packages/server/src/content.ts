// content.ts — D-127: load the real card corpus into the engine at Worker startup.
// The engine game modules ship 8-card DEFAULT stubs; this replaces each with the
// funnel-verified deck JSON (content/decks/*.json) via the modules' setXDeck setters.
// Selection/night-dedup/consent all live in the modules (8.3) — this just injects the pool.
//
// Bundled statically (no KV round-trip): wrangler/esbuild inlines the JSON at build time.
import {
  setRoastDeck,
  setFillinDeck,
  setOverUnderDeck,
  setConfessionDeck,
  setScatterDeck,
  setPoisonDeck,
  setRedflagDeck,
  setAlibiDeck,
  setTitleFightDeck,
  type CardBase,
  type FillinCard,
  type OverUnderCard,
  type ScatterCard,
  type PoisonCard,
  type RedFlagCard,
  type AlibiCard,
} from '@helldeck/engine';
import roast from '../../../content/decks/roast.json';
import fillin from '../../../content/decks/fillin.json';
import overunder from '../../../content/decks/overunder.json';
import confession from '../../../content/decks/confession.json';
import scatter from '../../../content/decks/scatter.json';
import poison from '../../../content/decks/poison.json';
import redflag from '../../../content/decks/redflag.json';
import alibi from '../../../content/decks/alibi.json';
import titlefight from '../../../content/decks/titlefight.json';

// The JSON is lint+dedup+stats-verified; the casts assert the enum/tuple shapes the tools
// already guaranteed (register in the 10-set, chaos>=3, alibi words[3]/decoys[5], etc.).
const as = <T,>(cards: unknown): readonly T[] => cards as readonly T[];

let loaded = false;

/** Idempotent: replace every module's stub deck with the real corpus. Call once at Worker load. */
export function loadContent(): { deck: string; cards: number }[] {
  const report = [
    { deck: 'roast', cards: roast.cards.length },
    { deck: 'fillin', cards: fillin.cards.length },
    { deck: 'overunder', cards: overunder.cards.length },
    { deck: 'confession', cards: confession.cards.length },
    { deck: 'scatter', cards: scatter.cards.length },
    { deck: 'poison', cards: poison.cards.length },
    { deck: 'redflag', cards: redflag.cards.length },
    { deck: 'alibi', cards: alibi.cards.length },
    { deck: 'titlefight', cards: titlefight.cards.length },
  ];
  if (loaded) return report;
  loaded = true;
  setRoastDeck(as<CardBase>(roast.cards));
  setFillinDeck(as<FillinCard>(fillin.cards));
  setOverUnderDeck(as<OverUnderCard>(overunder.cards));
  setConfessionDeck(as<CardBase>(confession.cards));
  setScatterDeck(as<ScatterCard>(scatter.cards));
  setPoisonDeck(as<PoisonCard>(poison.cards));
  setRedflagDeck(as<RedFlagCard>(redflag.cards));
  setAlibiDeck(as<AlibiCard>(alibi.cards));
  setTitleFightDeck(as<CardBase>(titlefight.cards));
  return report;
}
