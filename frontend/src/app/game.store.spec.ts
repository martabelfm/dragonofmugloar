import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { GameApiService } from './game-api.service';
import { GameStore, sortAds } from './game.store';
import { Advertisement, Decision, GameView, PlayerState, StrategyMode } from './game.models';

describe('advertisement ranking', () => {
  it('puts safety before reward', () => {
    const result = sortAds([
      ad('gamble', 999, 'Gamble'),
      ad('safe', 10, 'Piece of cake'),
      ad('likely', 300, 'Quite likely'),
    ]);
    expect(result.map((value) => value.adId)).toEqual(['safe', 'likely', 'gamble']);
  });

  it('puts higher rewards first within the same tier', () => {
    const result = sortAds([ad('small', 10, 'Sure thing'), ad('large', 80, 'Sure thing')]);
    expect(result.map((value) => value.adId)).toEqual(['large', 'small']);
  });
});

describe('GameStore', () => {
  let api: FakeGameApiService;

  beforeEach(() => {
    api = new FakeGameApiService();
    TestBed.configureTestingModule({
      providers: [{ provide: GameApiService, useValue: api }],
    });
  });

  it('starts a game and clears the busy flag', async () => {
    const store = TestBed.inject(GameStore);

    await store.start();

    expect(store.busy()).toBe(false);
    expect(store.player()?.gameId).toBe('game-1');
    expect(api.start).toHaveBeenCalledTimes(1);
  });

  it('does nothing and makes no request when there is no active game', async () => {
    const store = TestBed.inject(GameStore);

    await store.solve('ad-1');
    await store.purchase('item-1');
    await store.investigate();
    await store.refresh();
    const result = await store.updateStrategyMode('CONSERVATIVE');

    expect(api.solve).not.toHaveBeenCalled();
    expect(api.purchase).not.toHaveBeenCalled();
    expect(api.investigate).not.toHaveBeenCalled();
    expect(api.refresh).not.toHaveBeenCalled();
    expect(api.updateStrategyMode).not.toHaveBeenCalled();
    expect(result).toBeNull();
  });

  it('forwards the active game id to solve, purchase, and investigate', async () => {
    const store = TestBed.inject(GameStore);
    await store.start();

    await store.solve('ad-1');
    await store.purchase('item-1');
    await store.investigate();

    expect(api.solve).toHaveBeenCalledWith('game-1', 'ad-1');
    expect(api.purchase).toHaveBeenCalledWith('game-1', 'item-1');
    expect(api.investigate).toHaveBeenCalledWith('game-1');
  });

  it('surfaces the problem detail from a failed request', async () => {
    api.start.mockReturnValueOnce(
      throwError(
        () => new HttpErrorResponse({ error: { detail: 'Not enough gold.' }, status: 400 }),
      ),
    );
    const store = TestBed.inject(GameStore);

    await store.start();

    expect(store.error()).toBe('Not enough gold.');
    expect(store.busy()).toBe(false);
    expect(store.game()).toBeNull();
  });

  it('falls back to a generic message for a non-HTTP error', async () => {
    api.start.mockReturnValueOnce(throwError(() => new Error('boom')));
    const store = TestBed.inject(GameStore);

    await store.start();

    expect(store.error()).toBe('An unexpected error occurred.');
  });

  it('dismissError clears the stored error', async () => {
    api.start.mockReturnValueOnce(throwError(() => new Error('boom')));
    const store = TestBed.inject(GameStore);
    await store.start();

    store.dismissError();

    expect(store.error()).toBeNull();
  });

  describe('guidance', () => {
    it('is disabled with no active game', () => {
      const store = TestBed.inject(GameStore);

      expect(store.guidanceMode()).toBe('OFF');
      expect(store.guidanceEnabled()).toBe(false);
      expect(store.guidance()).toBeNull();
    });

    it('exposes the recommended mission once a strategy is selected', async () => {
      api.updateStrategyMode.mockReturnValueOnce(
        of(fixture({ strategyMode: 'CONSERVATIVE', recommendation: decision('SOLVE', 'ad-1', 'Piece of cake') })),
      );
      const store = TestBed.inject(GameStore);
      await store.start();

      await store.updateStrategyMode('CONSERVATIVE');

      expect(store.guidanceEnabled()).toBe(true);
      expect(store.guidance()).toEqual({ type: 'mission', targetId: 'ad-1', label: 'Piece of cake' });
      expect(store.isGuided('mission', 'ad-1')).toBe(true);
      expect(store.isGuided('mission', 'other-ad')).toBe(false);
      expect(store.isGuided('item', 'ad-1')).toBe(false);
    });

    it('treats HEAL and PURCHASE recommendations as an item to guide toward', async () => {
      api.updateStrategyMode.mockReturnValueOnce(
        of(fixture({ strategyMode: 'CONSERVATIVE', recommendation: decision('PURCHASE', 'hpot', 'Healing potion') })),
      );
      const store = TestBed.inject(GameStore);
      await store.start();

      await store.updateStrategyMode('CONSERVATIVE');

      expect(store.guidance()).toEqual({ type: 'item', targetId: 'hpot', label: 'Healing potion' });
      expect(store.isGuided('item', 'hpot')).toBe(true);
    });

    it('is null once the game has finished, even with a strategy selected', async () => {
      api.updateStrategyMode.mockReturnValueOnce(
        of(fixture({ strategyMode: 'CONSERVATIVE', finished: true })),
      );
      const store = TestBed.inject(GameStore);
      await store.start();

      await store.updateStrategyMode('CONSERVATIVE');

      expect(store.guidance()).toBeNull();
      expect(store.isGuided('mission', 'ad-1')).toBe(false);
    });

    it('disableGuidance stops automation and turns the strategy off', async () => {
      const store = TestBed.inject(GameStore);
      await store.start();
      await store.updateStrategyMode('CONSERVATIVE');

      store.disableGuidance();
      await Promise.resolve();

      expect(store.automationRunning()).toBe(false);
      expect(api.updateStrategyMode).toHaveBeenCalledWith('game-1', 'OFF');
    });
  });

  describe('runDecisionAutomation', () => {
    it('stops on its own once the recommendation is STOP', async () => {
      api.start.mockReturnValueOnce(of(fixture({ recommendation: decision('SOLVE', 'ad-1', 'Mission') })));
      api.autoStep.mockReturnValueOnce(of(fixture({ recommendation: decision('STOP', null, 'Stop') })));
      const store = TestBed.inject(GameStore);
      await store.start();

      await store.runDecisionAutomation();

      expect(api.autoStep).toHaveBeenCalledTimes(1);
      expect(store.automationRunning()).toBe(false);
    });

    it('can be interrupted early by stopAutomation', async () => {
      api.start.mockReturnValueOnce(of(fixture({ recommendation: decision('SOLVE', 'ad-1', 'Mission') })));
      api.autoStep.mockReturnValue(of(fixture({ recommendation: decision('SOLVE', 'ad-1', 'Mission') })));
      const store = TestBed.inject(GameStore);
      await store.start();

      const run = store.runDecisionAutomation();
      store.stopAutomation();
      await run;

      expect(store.automationRunning()).toBe(false);
      expect(api.autoStep.mock.calls.length).toBeLessThanOrEqual(1);
    });
  });
});

function ad(adId: string, reward: number, probability: string): Advertisement {
  return { adId, reward, probability, message: adId, expiresIn: 5, encrypted: null };
}

function decision(action: Decision['action'], targetId: string | null, title: string): Decision {
  return { action, targetId, title, reason: 'because', utility: 0 };
}

function playerState(overrides: Partial<PlayerState> = {}): PlayerState {
  return { gameId: 'game-1', lives: 3, gold: 100, level: 0, score: 0, highScore: 0, turn: 0, ...overrides };
}

function fixture(overrides: Partial<GameView> = {}): GameView {
  return {
    player: playerState(),
    ads: [],
    shop: [],
    reputation: null,
    strategyMode: 'OFF',
    purchasedItems: {},
    recommendation: decision('STOP', null, 'Stop'),
    history: [],
    finished: false,
    targetReached: false,
    ...overrides,
  };
}

class FakeGameApiService {
  start = vi.fn((): Observable<GameView> => of(fixture()));
  get = vi.fn((gameId: string): Observable<GameView> => of(fixture({ player: playerState({ gameId }) })));
  refresh = vi.fn((): Observable<GameView> => of(fixture()));
  solve = vi.fn((): Observable<GameView> => of(fixture()));
  purchase = vi.fn((): Observable<GameView> => of(fixture()));
  investigate = vi.fn((): Observable<GameView> => of(fixture()));
  autoStep = vi.fn((): Observable<GameView> => of(fixture()));
  updateStrategyMode = vi.fn(
    (_gameId: string, mode: StrategyMode): Observable<GameView> => of(fixture({ strategyMode: mode })),
  );
}
