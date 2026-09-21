export interface PlayerState {
  gameId: string;
  lives: number;
  gold: number;
  level: number;
  score: number;
  highScore: number;
  turn: number;
}

export interface Advertisement {
  adId: string;
  message: string;
  reward: number;
  expiresIn: number;
  encrypted: unknown;
  probability: string;
}

export interface ShopItem { id: string; name: string; cost: number; }
export interface Reputation { people: number; state: number; underworld: number; }
export type StrategyMode = 'SAFE_1000' | 'HIGH_SCORE';
export type DecisionAction = 'SOLVE' | 'HEAL' | 'PURCHASE' | 'INVESTIGATE' | 'STOP';

export interface Decision {
  action: DecisionAction;
  targetId: string | null;
  title: string;
  reason: string;
  utility: number;
}

export interface TurnRecord {
  turn: number;
  action: DecisionAction;
  targetId: string;
  description: string;
  successful: boolean;
  score: number;
  gold: number;
  lives: number;
  occurredAt: string;
}

export interface GameView {
  player: PlayerState;
  ads: Advertisement[];
  shop: ShopItem[];
  reputation: Reputation | null;
  strategyMode: StrategyMode;
  purchasedItems: Record<string, number>;
  recommendation: Decision;
  history: TurnRecord[];
  finished: boolean;
  targetReached: boolean;
}
