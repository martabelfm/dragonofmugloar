import { TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { GameApiService } from '../../game-api.service';
import { Advertisement, Decision, GameView, PlayerState, StrategyMode } from '../../game.models';
import { GameStore } from '../../game.store';
import { MissionCardComponent } from './mission-card';

const ad: Advertisement = {
  adId: 'ad-1',
  message: 'Deliver squirrels',
  reward: 42,
  expiresIn: 1,
  encrypted: null,
  probability: 'Sure thing',
};

function playerState(overrides: Partial<PlayerState> = {}): PlayerState {
  return { gameId: 'game-1', lives: 3, gold: 100, level: 0, score: 0, highScore: 0, turn: 0, ...overrides };
}

function recommendation(overrides: Partial<Decision> = {}): Decision {
  return { action: 'STOP', targetId: null, title: 'Stop', reason: '', utility: 0, ...overrides };
}

function fixture(overrides: Partial<GameView> = {}): GameView {
  return {
    player: playerState(),
    ads: [ad],
    shop: [],
    reputation: null,
    strategyMode: 'OFF',
    purchasedItems: {},
    recommendation: recommendation(),
    history: [],
    finished: false,
    targetReached: false,
    ...overrides,
  };
}

class FakeGameApiService {
  start = vi.fn((): Observable<GameView> => of(fixture()));
  updateStrategyMode = vi.fn(
    (_gameId: string, mode: StrategyMode): Observable<GameView> => of(fixture({ strategyMode: mode })),
  );
}

describe('MissionCardComponent', () => {
  let api: FakeGameApiService;

  beforeEach(() => {
    api = new FakeGameApiService();
    TestBed.configureTestingModule({ providers: [{ provide: GameApiService, useValue: api }] });
  });

  function createComponent() {
    const componentFixture = TestBed.createComponent(MissionCardComponent);
    componentFixture.componentRef.setInput('ad', ad);
    componentFixture.detectChanges();
    return componentFixture;
  }

  it('renders the mission message, reward, and an urgent expiry', () => {
    const componentFixture = createComponent();
    const text = componentFixture.nativeElement.textContent as string;

    expect(text).toContain('Deliver squirrels');
    expect(text).toContain('42g');
    const expires = componentFixture.nativeElement.querySelector('.expires');
    expect(expires.classList.contains('urgent')).toBe(true);
  });

  it('marks a safe probability with the safe risk class', () => {
    const componentFixture = createComponent();
    const risk = componentFixture.nativeElement.querySelector('.risk');
    expect(risk.classList.contains('safe')).toBe(true);
  });

  it('calls store.solve with its own advertisement id when accepted', async () => {
    api.updateStrategyMode.mockReturnValueOnce(of(fixture({ strategyMode: 'CONSERVATIVE' })));
    const componentFixture = createComponent();
    const store = TestBed.inject(GameStore);
    await store.start();
    const solveSpy = vi.spyOn(store, 'solve').mockResolvedValue(undefined);

    componentFixture.nativeElement.querySelector('button').click();

    expect(solveSpy).toHaveBeenCalledWith('ad-1');
  });

  it('highlights itself as recommended when the store guides toward it', async () => {
    api.updateStrategyMode.mockReturnValueOnce(
      of(fixture({ strategyMode: 'CONSERVATIVE', recommendation: recommendation({ action: 'SOLVE', targetId: 'ad-1' }) })),
    );
    const componentFixture = createComponent();
    const store = TestBed.inject(GameStore);
    await store.start();
    await store.updateStrategyMode('CONSERVATIVE');
    componentFixture.detectChanges();

    const article = componentFixture.nativeElement.querySelector('.mission');
    expect(article.classList.contains('recommended')).toBe(true);
  });

  it('does not highlight itself when the store guides toward a different mission', async () => {
    api.updateStrategyMode.mockReturnValueOnce(
      of(fixture({ strategyMode: 'CONSERVATIVE', recommendation: recommendation({ action: 'SOLVE', targetId: 'other-ad' }) })),
    );
    const componentFixture = createComponent();
    const store = TestBed.inject(GameStore);
    await store.start();
    await store.updateStrategyMode('CONSERVATIVE');
    componentFixture.detectChanges();

    const article = componentFixture.nativeElement.querySelector('.mission');
    expect(article.classList.contains('recommended')).toBe(false);
  });

  it('disables the accept button while a request is in flight', () => {
    const componentFixture = createComponent();
    const store = TestBed.inject(GameStore);

    void store.start();
    componentFixture.detectChanges();

    expect(componentFixture.nativeElement.querySelector('button').disabled).toBe(true);
  });
});
