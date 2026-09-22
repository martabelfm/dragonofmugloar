import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, startWith } from 'rxjs';
import { ActionRailComponent } from './components/action-rail/action-rail';
import { GameHeaderComponent } from './components/game-header/game-header';
import { MissionBoardComponent } from './components/mission-board/mission-board';
import { MissionFeedbackComponent } from './components/mission-feedback/mission-feedback';
import { MobileNavComponent } from './components/mobile-nav/mobile-nav';
import { ShopPanelComponent } from './components/shop-panel/shop-panel';
import { GameStore } from './game.store';
import { TranslatePipe } from './translate.pipe';

@Component({
  imports: [
    TranslatePipe,
    GameHeaderComponent,
    ActionRailComponent,
    MissionBoardComponent,
    ShopPanelComponent,
    MobileNavComponent,
    MissionFeedbackComponent,
  ],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly store = inject(GameStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private loadedGameId: string | null = null;

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        startWith(null),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.restoreRouteGame());
  }

  protected async startGame(): Promise<void> {
    this.store.disableGuidance();
    const game = await this.store.start();
    if (game) await this.router.navigate(['/games', game.player.gameId]);
  }

  private restoreRouteGame(): void {
    const match = this.router.url.match(/^\/games\/([A-Za-z0-9_-]{1,64})(?:[?#]|$)/);
    const gameId = match?.[1] ?? null;
    if (!gameId || gameId === this.loadedGameId || this.store.player()?.gameId === gameId) return;
    this.loadedGameId = gameId;
    void this.store.load(gameId);
  }
}
