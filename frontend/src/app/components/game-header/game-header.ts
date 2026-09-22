import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, effect, inject, output, signal, viewChild } from '@angular/core';
import {
  DESKTOP_SCORE_ABBREVIATION_THRESHOLD,
  MOBILE_MAX_WIDTH_QUERY,
  MOBILE_SCORE_ABBREVIATION_THRESHOLD,
  abbreviateScore,
} from '../../game.constants';
import { GameStore } from '../../game.store';
import { translate, TranslatePipe } from '../../translate.pipe';
import { GuidanceDialogComponent } from '../guidance-dialog/guidance-dialog';
import { NewGameDialogComponent } from '../new-game-dialog/new-game-dialog';

@Component({
  imports: [DecimalPipe, TranslatePipe, NewGameDialogComponent, GuidanceDialogComponent],
  selector: 'app-game-header',
  styleUrl: './game-header.scss',
  templateUrl: './game-header.html',
})
export class GameHeaderComponent {
  protected readonly store = inject(GameStore);
  private readonly destroyRef = inject(DestroyRef);
  private readonly newGameDialog = viewChild.required(NewGameDialogComponent);
  private readonly guidanceDialog = viewChild.required(GuidanceDialogComponent);
  readonly newGame = output<void>();

  private readonly isNarrowViewport = signal(window.matchMedia(MOBILE_MAX_WIDTH_QUERY).matches);
  private readonly scoreAbbreviationThreshold = computed(() =>
    this.isNarrowViewport() ? MOBILE_SCORE_ABBREVIATION_THRESHOLD : DESKTOP_SCORE_ABBREVIATION_THRESHOLD,
  );
  protected readonly scoreExpanded = signal(false);

  constructor() {
    const viewportQuery = window.matchMedia(MOBILE_MAX_WIDTH_QUERY);
    const onViewportChange = (event: MediaQueryListEvent) => this.isNarrowViewport.set(event.matches);
    viewportQuery.addEventListener('change', onViewportChange);
    this.destroyRef.onDestroy(() => viewportQuery.removeEventListener('change', onViewportChange));

    // A fresh game shouldn't inherit "expanded" from a previous run that crossed the threshold.
    let observedGameId: string | null = null;
    effect(() => {
      const gameId = this.store.player()?.gameId ?? null;
      if (gameId !== observedGameId) {
        observedGameId = gameId;
        this.scoreExpanded.set(false);
      }
    });
  }

  protected requestNewGame(): void {
    if (this.store.player()) {
      this.newGameDialog().open();
    } else {
      this.newGame.emit();
    }
  }

  protected openGuidance(): void {
    this.guidanceDialog().open();
  }

  protected guidanceButtonLabel(): string {
    if (!this.store.guidanceEnabled()) return translate('off');
    return this.store.guidanceMode() === 'HIGH_RISK' ? translate('highRisk') : translate('conservative');
  }

  protected isScoreAbbreviated(score: number): boolean {
    return score >= this.scoreAbbreviationThreshold();
  }

  protected scoreText(score: number): string {
    return this.scoreExpanded() ? score.toLocaleString('en-US') : abbreviateScore(score);
  }
}
