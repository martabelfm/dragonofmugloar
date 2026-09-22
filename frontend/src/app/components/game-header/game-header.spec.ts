import { TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { GameApiService } from '../../game-api.service';
import { Decision, GameView, PlayerState, StrategyMode } from '../../game.models';
import { GameStore } from '../../game.store';
import { GameHeaderComponent } from './game-header';

function playerState(overrides: Partial<PlayerState> = {}): PlayerState {
  return { gameId: 'game-1', lives: 3, gold: 0, level: 0, score: 0, highScore: 0, turn: 0, ...overrides };
}

function recommendation(): Decision {
  return { action: 'STOP', targetId: null, title: 'Stop', reason: '', utility: 0 };
}

function fixture(overrides: Partial<GameView> = {}): GameView {
  return {
    player: playerState(),
    ads: [],
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

describe('GameHeaderComponent', () => {
  let api: FakeGameApiService;

  beforeAll(() => {
    // jsdom does not implement the native <dialog> methods; stub them so opening a
    // confirmation dialog in a test doesn't throw.
    HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) {
      this.setAttribute('open', '');
    };
    HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) {
      this.removeAttribute('open');
    };
  });

  beforeEach(() => {
    api = new FakeGameApiService();
    TestBed.configureTestingModule({ providers: [{ provide: GameApiService, useValue: api }] });
  });

  it('shows "Begin adventure" and emits newGame directly when no game has started', () => {
    const componentFixture = TestBed.createComponent(GameHeaderComponent);
    componentFixture.detectChanges();
    let emitted = false;
    componentFixture.componentInstance.newGame.subscribe(() => (emitted = true));

    const button = componentFixture.nativeElement.querySelector('.new-game') as HTMLButtonElement;
    expect(button.textContent).toContain('Begin adventure');
    button.click();

    expect(emitted).toBe(true);
  });

  it('labels the strategy button "Off" until a guidance mode is selected', async () => {
    const componentFixture = TestBed.createComponent(GameHeaderComponent);
    await TestBed.inject(GameStore).start();
    componentFixture.detectChanges();

    const strategyButton = componentFixture.nativeElement.querySelector('.strategy-button strong');
    expect(strategyButton.textContent).toBe('Off');
  });

  it('labels the strategy button with the active guidance mode', async () => {
    api.updateStrategyMode.mockReturnValueOnce(of(fixture({ strategyMode: 'HIGH_RISK' })));
    const componentFixture = TestBed.createComponent(GameHeaderComponent);
    const store = TestBed.inject(GameStore);
    await store.start();
    await store.updateStrategyMode('HIGH_RISK');
    componentFixture.detectChanges();

    const strategyButton = componentFixture.nativeElement.querySelector('.strategy-button strong');
    expect(strategyButton.textContent).toBe('High risk');
  });

  it('does not emit newGame immediately when a game is already active', async () => {
    const componentFixture = TestBed.createComponent(GameHeaderComponent);
    await TestBed.inject(GameStore).start();
    componentFixture.detectChanges();
    let emitted = false;
    componentFixture.componentInstance.newGame.subscribe(() => (emitted = true));

    const button = componentFixture.nativeElement.querySelector('.new-game') as HTMLButtonElement;
    button.click();

    expect(emitted).toBe(false);
  });
});
