import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, startWith } from 'rxjs';
import { Advertisement, StrategyMode } from './game.models';
import { GameStore } from './game.store';

@Component({
  imports: [DecimalPipe],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly store = inject(GameStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private loadedGameId: string | null = null;
  private observedGameId: string | null = null;
  private observedTurn = -1;
  private feedbackTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly missionFeedback = signal<{ successful: boolean; message: string } | null>(null);
  protected readonly guidanceEnabled = signal(true);
  protected readonly guidanceMode = computed(() => this.store.game()?.strategyMode ?? 'SAFE_1000');
  protected readonly guidance = computed(() => {
    const game = this.store.game();
    if (!game || game.finished || !this.guidanceEnabled()) return null;
    const recommendation = game.recommendation;
    return {
      type: recommendation.action === 'SOLVE' ? 'mission' :
        recommendation.action === 'PURCHASE' || recommendation.action === 'HEAL' ? 'item' : 'none',
      targetId: recommendation.targetId,
      label: recommendation.title,
    };
  });

  constructor() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      startWith(null),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.restoreRouteGame());

    effect(() => this.showLatestMissionFeedback());
    effect(() => this.scrollToMobileRecommendation());

    this.destroyRef.onDestroy(() => {
      if (this.feedbackTimer) clearTimeout(this.feedbackTimer);
    });
  }

  protected async startGame(): Promise<void> {
    const game = await this.store.start();
    if (game) await this.router.navigate(['/games', game.player.gameId]);
  }

  protected runSelectedStrategy(): void {
    void this.store.runDecisionAutomation();
  }

  protected selectGuidanceMode(mode: StrategyMode): void {
    this.guidanceEnabled.set(true);
    if (mode !== this.guidanceMode()) void this.store.updateStrategyMode(mode);
  }

  protected disableGuidance(): void {
    this.store.stopAutomation();
    this.guidanceEnabled.set(false);
  }

  protected openChronicle(dialog: HTMLDialogElement): void {
    dialog.showModal();
  }

  protected closeOnBackdrop(event: MouseEvent, dialog: HTMLDialogElement): void {
    if (event.target === dialog) dialog.close();
  }

  protected isGuided(type: 'mission' | 'item', targetId: string): boolean {
    if (!this.guidanceEnabled()) return false;
    const guidance = this.guidance();
    return guidance?.type === type && guidance.targetId === targetId;
  }

  protected riskClass(ad: Advertisement): string {
    if (['Piece of cake', 'Walk in the park', 'Sure thing'].includes(ad.probability)) return 'risk safe';
    if (['Quite likely', 'Hmmm....', 'Gamble'].includes(ad.probability)) return 'risk medium';
    return 'risk danger';
  }

  private showLatestMissionFeedback(): void {
    const player = this.store.player();
    const latest = this.store.history()[0];
    if (!player) return;
    if (player.gameId !== this.observedGameId) {
      this.observedGameId = player.gameId;
      this.observedTurn = player.turn;
      return;
    }
    if (!latest || player.turn <= this.observedTurn) return;
    this.observedTurn = player.turn;
    if (latest.action !== 'SOLVE') return;
    this.missionFeedback.set({ successful: latest.successful, message: latest.description });
    if (this.feedbackTimer) clearTimeout(this.feedbackTimer);
    this.feedbackTimer = setTimeout(() => this.missionFeedback.set(null), 1_400);
  }

  private scrollToMobileRecommendation(): void {
    const game = this.store.game();
    if (!this.guidanceEnabled() || !game || game.finished || game.recommendation.action === 'STOP') return;
    setTimeout(() => {
      if (!window.matchMedia('(max-width: 900px)').matches || !this.guidanceEnabled()) return;
      document.querySelector<HTMLElement>('.recommended, .investigate-recommended')
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  }

  private restoreRouteGame(): void {
    const match = this.router.url.match(/^\/games\/([A-Za-z0-9_-]{1,64})(?:[?#]|$)/);
    const gameId = match?.[1] ?? null;
    if (!gameId || gameId === this.loadedGameId || this.store.player()?.gameId === gameId) return;
    this.loadedGameId = gameId;
    void this.store.load(gameId);
  }
}
