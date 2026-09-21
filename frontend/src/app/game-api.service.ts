import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GameView, StrategyMode } from './game.models';

@Injectable({ providedIn: 'root' })
export class GameApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/games';

  start() {
    return this.http.post<GameView>(this.baseUrl, {});
  }
  get(gameId: string) {
    return this.http.get<GameView>(`${this.baseUrl}/${gameId}`);
  }
  refresh(gameId: string) {
    return this.http.post<GameView>(`${this.baseUrl}/${gameId}/refresh`, {});
  }
  solve(gameId: string, adId: string) {
    return this.http.post<GameView>(`${this.baseUrl}/${gameId}/ads/${adId}/solve`, {});
  }
  purchase(gameId: string, itemId: string) {
    return this.http.post<GameView>(`${this.baseUrl}/${gameId}/shop/${itemId}/purchase`, {});
  }
  investigate(gameId: string) {
    return this.http.post<GameView>(`${this.baseUrl}/${gameId}/reputation`, {});
  }
  autoStep(gameId: string) {
    return this.http.post<GameView>(`${this.baseUrl}/${gameId}/auto/step`, {});
  }
  updateStrategyMode(gameId: string, mode: StrategyMode) {
    return this.http.put<GameView>(`${this.baseUrl}/${gameId}/strategy-mode`, { mode });
  }
}
