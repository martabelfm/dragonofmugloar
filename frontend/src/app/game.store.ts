import { computed, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { Advertisement, GameView, StrategyMode } from './game.models';
import { GameApiService } from './game-api.service';

interface GameStoreState {
  game: GameView | null;
  busy: boolean;
  automationRunning: boolean;
  error: string | null;
}

const initialState: GameStoreState = { game: null, busy: false, automationRunning: false, error: null };

export const GameStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ game }) => ({
    player: computed(() => game()?.player ?? null),
    ads: computed(() => sortAds(game()?.ads ?? [])),
    shop: computed(() => game()?.shop ?? []),
    history: computed(() => [...(game()?.history ?? [])].reverse()),
    progress: computed(() => Math.min(100, ((game()?.player.score ?? 0) / 1000) * 100)),
  })),
  withMethods((store, api = inject(GameApiService)) => {
    let stopRequested = false;
    const execute = async (request: () => Observable<GameView>) => {
      patchState(store, { busy: true, error: null });
      try {
        const game = await firstValueFrom(request());
        patchState(store, { game, busy: false });
        sessionStorage.setItem('mugloar-game-id', game.player.gameId);
        return game;
      } catch (error) {
        patchState(store, { busy: false, error: errorMessage(error) });
        return null;
      }
    };

    return {
      async start() { stopRequested = true; return execute(() => api.start()); },
      async load(gameId: string) { stopRequested = true; return execute(() => api.get(gameId)); },
      async refresh() { const id = store.player()?.gameId; if (id) await execute(() => api.refresh(id)); },
      async solve(adId: string) { const id = store.player()?.gameId; if (id) await execute(() => api.solve(id, adId)); },
      async purchase(itemId: string) { const id = store.player()?.gameId; if (id) await execute(() => api.purchase(id, itemId)); },
      async investigate() { const id = store.player()?.gameId; if (id) await execute(() => api.investigate(id)); },
      async updateStrategyMode(mode: StrategyMode) {
        const id = store.player()?.gameId;
        return id ? execute(() => api.updateStrategyMode(id, mode)) : null;
      },
      async runDecisionAutomation() {
        if (!store.player() || store.automationRunning()) return;
        stopRequested = false;
        patchState(store, { automationRunning: true, error: null });
        while (!stopRequested) {
          const current = store.game();
          if (!current || current.finished) break;
          if (current.recommendation.action === 'STOP') break;
          const result = await execute(() => api.autoStep(current.player.gameId));
          if (!result) break;
          await new Promise(resolve => setTimeout(resolve, 140));
        }
        patchState(store, { automationRunning: false, busy: false });
      },
      stopAutomation() { stopRequested = true; patchState(store, { automationRunning: false }); },
      dismissError() { patchState(store, { error: null }); },
    };
  }),
);

export function sortAds(ads: Advertisement[]): Advertisement[] {
  const rank: Record<string, number> = {
    'Sure thing': 11, 'Piece of cake': 10, 'Walk in the park': 9, 'Quite likely': 8,
    'Hmmm....': 7, Gamble: 6, Risky: 5, 'Playing with fire': 4,
    'Rather detrimental': 3, 'Suicide mission': 2, Impossible: 1,
  };
  return [...ads].sort((a, b) => (rank[b.probability] ?? -1) - (rank[a.probability] ?? -1) || b.reward - a.reward);
}

function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    return error.error?.detail ?? error.error?.title ?? 'The request could not be completed.';
  }
  return 'An unexpected error occurred.';
}
