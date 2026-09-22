import { TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { GameApiService } from '../../game-api.service';
import { Decision, GameView, PlayerState, TurnRecord } from '../../game.models';
import { GameStore } from '../../game.store';
import { MissionFeedbackComponent } from './mission-feedback';

function playerState(overrides: Partial<PlayerState> = {}): PlayerState {
  return { gameId: 'game-1', lives: 3, gold: 0, level: 0, score: 0, highScore: 0, turn: 0, ...overrides };
}

function recommendation(): Decision {
  return { action: 'STOP', targetId: null, title: 'Stop', reason: '', utility: 0 };
}

function turn(overrides: Partial<TurnRecord> = {}): TurnRecord {
  return {
    turn: 1,
    action: 'SOLVE',
    targetId: 'ad-1',
    description: 'Solved it',
    successful: true,
    score: 10,
    gold: 10,
    lives: 3,
    occurredAt: new Date().toISOString(),
    ...overrides,
  };
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
  solve = vi.fn((): Observable<GameView> => of(fixture()));
  investigate = vi.fn((): Observable<GameView> => of(fixture()));
}

describe('MissionFeedbackComponent', () => {
  let api: FakeGameApiService;

  beforeEach(() => {
    api = new FakeGameApiService();
    TestBed.configureTestingModule({ providers: [{ provide: GameApiService, useValue: api }] });
  });

  it('shows a toast for a newly solved turn and hides it again after the timeout', async () => {
    const componentFixture = TestBed.createComponent(MissionFeedbackComponent);
    const store = TestBed.inject(GameStore);
    componentFixture.detectChanges();
    await componentFixture.whenStable();

    await store.start();
    componentFixture.detectChanges();
    await componentFixture.whenStable();
    expect(componentFixture.nativeElement.querySelector('.mission-feedback')).toBeNull();

    api.solve.mockReturnValueOnce(
      of(
        fixture({
          player: playerState({ turn: 1 }),
          history: [turn({ turn: 1, successful: true, description: 'Delivered the squirrels' })],
        }),
      ),
    );
    await store.solve('ad-1');
    componentFixture.detectChanges();
    await componentFixture.whenStable();

    const toast = componentFixture.nativeElement.querySelector('.mission-feedback');
    expect(toast).not.toBeNull();
    expect(toast.classList.contains('success')).toBe(true);
    expect(componentFixture.nativeElement.textContent).toContain('Delivered the squirrels');

    await new Promise((resolve) => setTimeout(resolve, 1_500));
    componentFixture.detectChanges();
    expect(componentFixture.nativeElement.querySelector('.mission-feedback')).toBeNull();
  }, 10_000);

  it('ignores turns that are not a solved mission', async () => {
    const componentFixture = TestBed.createComponent(MissionFeedbackComponent);
    const store = TestBed.inject(GameStore);
    componentFixture.detectChanges();
    await componentFixture.whenStable();

    await store.start();
    componentFixture.detectChanges();
    await componentFixture.whenStable();

    api.investigate.mockReturnValueOnce(
      of(
        fixture({
          player: playerState({ turn: 1 }),
          history: [turn({ turn: 1, action: 'INVESTIGATE', description: 'Investigated' })],
        }),
      ),
    );
    await store.investigate();
    componentFixture.detectChanges();
    await componentFixture.whenStable();

    expect(componentFixture.nativeElement.querySelector('.mission-feedback')).toBeNull();
  });
});
