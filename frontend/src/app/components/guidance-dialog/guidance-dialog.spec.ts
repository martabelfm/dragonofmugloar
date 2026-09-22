import { TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { GameApiService } from '../../game-api.service';
import { Decision, GameView, PlayerState, StrategyMode } from '../../game.models';
import { GameStore } from '../../game.store';
import { GuidanceDialogComponent } from './guidance-dialog';

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

describe('GuidanceDialogComponent', () => {
  let api: FakeGameApiService;

  beforeAll(() => {
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

  async function createComponent() {
    const componentFixture = TestBed.createComponent(GuidanceDialogComponent);
    const store = TestBed.inject(GameStore);
    await store.start();
    componentFixture.detectChanges();
    return { componentFixture, store };
  }

  it('shows the follow-up prompt only after a mode is actively selected', async () => {
    const { componentFixture } = await createComponent();

    const conservativeButton = Array.from(componentFixture.nativeElement.querySelectorAll('button')).find(
      (button) => (button as HTMLButtonElement).textContent?.trim() === 'Conservative',
    ) as HTMLButtonElement;
    conservativeButton.click();
    await componentFixture.whenStable();
    componentFixture.detectChanges();

    expect(componentFixture.nativeElement.querySelector('.strategy-follow-up')).not.toBeNull();
    expect(api.updateStrategyMode).toHaveBeenCalledWith('game-1', 'CONSERVATIVE');
  });

  it('does not call the API again when re-selecting the already-active mode', async () => {
    const { componentFixture, store } = await createComponent();
    api.updateStrategyMode.mockReturnValueOnce(of(fixture({ strategyMode: 'CONSERVATIVE' })));
    await store.updateStrategyMode('CONSERVATIVE');
    componentFixture.detectChanges();
    api.updateStrategyMode.mockClear();

    const conservativeButton = Array.from(componentFixture.nativeElement.querySelectorAll('button')).find(
      (button) => (button as HTMLButtonElement).textContent?.trim() === 'Conservative',
    ) as HTMLButtonElement;
    conservativeButton.click();
    await componentFixture.whenStable();
    componentFixture.detectChanges();

    expect(api.updateStrategyMode).not.toHaveBeenCalled();
    expect(componentFixture.nativeElement.querySelector('.strategy-follow-up')).not.toBeNull();
  });

  it('disable() turns guidance off and clears the follow-up prompt', async () => {
    const { componentFixture, store } = await createComponent();
    api.updateStrategyMode.mockReturnValueOnce(of(fixture({ strategyMode: 'CONSERVATIVE' })));
    await store.updateStrategyMode('CONSERVATIVE');
    componentFixture.detectChanges();

    const offButton = Array.from(componentFixture.nativeElement.querySelectorAll('button')).find(
      (button) => (button as HTMLButtonElement).textContent?.trim() === 'Off',
    ) as HTMLButtonElement;
    offButton.click();
    await componentFixture.whenStable();

    expect(api.updateStrategyMode).toHaveBeenCalledWith('game-1', 'OFF');
    expect(store.automationRunning()).toBe(false);
  });
});
