import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, startWith } from 'rxjs';
import { ADVERTISEMENT_SAFETY_RANK } from './game.constants';
import { Advertisement, StrategyMode } from './game.models';
import { GameStore } from './game.store';
import { translate, TranslatePipe } from './translate.pipe';

@Component({
  imports: [DecimalPipe, TranslatePipe],
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

  protected readonly missionFeedback = signal<{ successful: boolean; message: string } | null>(
    null,
  );
  protected readonly guidanceEnabled = computed(() => this.guidanceMode() !== 'OFF');
  protected readonly strategySelected = signal(false);
  protected readonly guidanceMode = computed(() => this.store.game()?.strategyMode ?? 'OFF');
  protected readonly guidance = computed(() => {
    const game = this.store.game();
    if (!game || game.finished || !this.guidanceEnabled()) return null;
    const recommendation = game.recommendation;
    return {
      type:
        recommendation.action === 'SOLVE'
          ? 'mission'
          : recommendation.action === 'PURCHASE' || recommendation.action === 'HEAL'
            ? 'item'
            : 'none',
      targetId: recommendation.targetId,
      label: recommendation.title,
    };
  });

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        startWith(null),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.restoreRouteGame());

    effect(() => this.showLatestMissionFeedback());
    effect(() => this.scrollToMobileRecommendation());

    this.destroyRef.onDestroy(() => {
      if (this.feedbackTimer) clearTimeout(this.feedbackTimer);
    });
  }

  protected async startGame(): Promise<void> {
    this.disableGuidance();
    const game = await this.store.start();
    if (game) await this.router.navigate(['/games', game.player.gameId]);
  }

  protected runSelectedStrategy(): void {
    void this.store.runDecisionAutomation();
  }

  protected async selectGuidanceMode(mode: StrategyMode): Promise<void> {
    const game =
      mode === this.guidanceMode() ? this.store.game() : await this.store.updateStrategyMode(mode);
    if (game) this.strategySelected.set(true);
  }

  protected disableGuidance(): void {
    this.store.stopAutomation();
    this.strategySelected.set(false);
    void this.store.updateStrategyMode('OFF');
  }

  protected openChronicle(dialog: HTMLDialogElement): void {
    dialog.showModal();
  }

  protected openGuidance(dialog: HTMLDialogElement): void {
    this.strategySelected.set(false);
    dialog.showModal();
  }

  protected runAutomationFromDialog(dialog: HTMLDialogElement): void {
    dialog.close();
    this.runSelectedStrategy();
  }

  protected openNewGameConfirmation(dialog: HTMLDialogElement): void {
    dialog.showModal();
  }

  protected confirmNewGame(dialog: HTMLDialogElement): void {
    dialog.close();
    void this.startGame();
  }

  protected guidanceButtonLabel(): string {
    if (!this.guidanceEnabled()) return translate('off');
    return this.guidanceMode() === 'HIGH_RISK'
      ? translate('highRisk')
      : translate('conservative');
  }

  protected scrollToSection(sectionId: 'controls' | 'missions' | 'shop'): void {
    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  protected scrollToTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
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
    const rank = ADVERTISEMENT_SAFETY_RANK[ad.probability] ?? 0;
    if (rank >= 9) return 'risk safe';
    if (rank >= 6) return 'risk medium';
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
    if (!this.guidanceEnabled() || !game || game.finished || game.recommendation.action === 'STOP')
      return;
    setTimeout(() => {
      if (!window.matchMedia('(max-width: 900px)').matches || !this.guidanceEnabled()) return;
      document
        .querySelector<HTMLElement>('.recommended, .investigate-recommended')
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
