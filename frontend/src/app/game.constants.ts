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
