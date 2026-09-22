import { Advertisement } from './game.models';

/** UI-only values. Game policy remains authoritative in the backend. */
export const TARGET_SCORE = 1_000;

/** Keep in sync with backend/src/main/java/com/mugloar/domain/Probability.java. */
export const ADVERTISEMENT_SAFETY_RANK: Readonly<Record<string, number>> = {
  'Sure thing': 11,
  'Piece of cake': 10,
  'Walk in the park': 9,
  'Quite likely': 8,
  'Hmmm....': 7,
  Gamble: 6,
  Risky: 5,
  'Playing with fire': 4,
  'Rather detrimental': 3,
  'Suicide mission': 2,
  Impossible: 1,
};

export function riskClass(ad: Advertisement): string {
  const rank = ADVERTISEMENT_SAFETY_RANK[ad.probability] ?? 0;
  if (rank >= 9) return 'risk safe';
  if (rank >= 6) return 'risk medium';
  return 'risk danger';
}

/** Matches the layout breakpoint the rest of the app treats as "mobile" (see app.scss). */
export const MOBILE_MAX_WIDTH_QUERY = '(max-width: 900px)';

/** Score display abbreviates sooner on a narrow (mobile) viewport, where header space is tighter. */
export const MOBILE_SCORE_ABBREVIATION_THRESHOLD = 1_000_000;
export const DESKTOP_SCORE_ABBREVIATION_THRESHOLD = 100_000_000;

/** Formats a large score compactly, e.g. 12_345_000 -> "12.3M". Callers decide when to use this
 * instead of the exact number; below a million it would round away too much precision. */
export function abbreviateScore(score: number): string {
  const useBillions = score >= 1_000_000_000;
  const divisor = useBillions ? 1_000_000_000 : 1_000_000;
  const suffix = useBillions ? 'B' : 'M';
  return `${(score / divisor).toFixed(1)}${suffix}`;
}
