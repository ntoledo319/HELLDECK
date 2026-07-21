// CIRCLE_INTRO — descent depth art + game title + explainer (15s first time, 5s after; the
// server owns the clock, we just render). Explainer copy lives client-side: it's UI chrome.
// Spotlight games also host the 4.5 "WHO WANTS BLOOD?" claim here — tap to volunteer for the
// spike; a claim only ever raises your odds of being picked, so it's a dare, not a trap.
import { useConnectionOptimistic } from '../connection';
import type { Net } from '../net/ws';
import { asView, type IntroView } from '../view';

export const GAME_META: Record<string, { title: string; blurb: string }> = {
  roast: {
    title: 'ROAST CONSENSUS',
    blurb: 'A question. A room full of suspects. Vote who it burns — plurality puts their name in lights. You cannot vote for yourself. Nice try.',
  },
  fillin: {
    title: 'FILL-IN FINISHER',
    blurb: 'Finish the setup in writing. One Reader performs the lot out loud. The room votes for the line that did the damage.',
  },
  overunder: {
    title: 'OVER/UNDER',
    blurb: 'A stat. A subject. The room argues a line, bets OVER or UNDER — then the receipts come out.',
  },
  confession: {
    title: 'CONFESSION OR CAP',
    blurb: 'They pick a sin and sell it to your face. True or cap? The jury decides. A hung jury lets the liar walk.',
  },
  scatter: {
    title: 'SCATTERBLAST',
    blurb: 'Category. Letter. Answers out loud, pass the bomb with your mouth. Holding it at the boom means the table decides you died.',
  },
  poison: {
    title: 'POISON PITCH',
    blurb: 'Two horrors, two pitchers. Sell your poison like your life depends on it. The room votes the better PITCH, not the better option.',
  },
  redflag: {
    title: 'RED FLAG RALLY',
    blurb: 'A dream perk with a dealbreaker stapled to it. Defend the relationship anyway. SMASH or PASS.',
  },
  alibi: {
    title: 'ALIBI DROP',
    blurb: 'You stand accused. Three planted words must appear in your alibi. The jury hunts the plants.',
  },
  titlefight: {
    title: 'TITLE FIGHT',
    blurb: 'Two fighters. One duel verb. The crowd points at the winner.',
  },
};

export function CircleIntro({
  circleIdx,
  arcLength,
  gameView,
  net,
}: {
  circleIdx: number;
  arcLength: number;
  gameView: unknown;
  net: Net;
}) {
  const iv = asView<IntroView>(gameView, 'INTRO');
  const meta = iv ? GAME_META[iv.deck] : undefined;
  const title = meta?.title ?? 'THE NEXT CIRCLE';
  const blurb = meta?.blurb ?? '';
  const rungs = Math.max(arcLength, 1);
  const [tapped, setTapped] = useConnectionOptimistic<boolean>(false);
  const claimed = iv?.youVolunteered || tapped;
  const claim = (): void => {
    if (claimed) return;
    if (net.send({ t: 'CLAIM' })) setTapped(true);
  };
  return (
    <main class="screen intro">
      <div class="depth-strip">
        {Array.from({ length: rungs }, (_, i) => (
          <span key={i} class={i < circleIdx ? 'rung past' : i === circleIdx ? 'rung here' : 'rung'} />
        ))}
      </div>
      <div class="circle-label">CIRCLE</div>
      <div class="circle-num">{circleIdx + 1}</div>
      <div class="circle-of">OF {rungs}</div>
      <h1 class="game-title flash-in">{title}</h1>
      {blurb && (iv?.firstTime ?? true) && <p class="game-blurb">{blurb}</p>}
      {iv?.claimable && (
        <div class="blood-claim">
          {claimed ? (
            <div class="locked-banner flash-in" role="status" aria-live="polite">HAND UP. THE DECK SEES YOU.</div>
          ) : (
            <button class="btn-blood big" onClick={claim}>
              WHO WANTS BLOOD?
            </button>
          )}
          <span class="stat-about">
            {iv.claimed ?? 0} {(iv.claimed ?? 0) === 1 ? 'FOOL VOLUNTEERED' : 'FOOLS VOLUNTEERED'}
          </span>
        </div>
      )}
    </main>
  );
}
