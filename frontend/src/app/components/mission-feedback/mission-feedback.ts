import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { GameStore } from '../../game.store';
import { TranslatePipe } from '../../translate.pipe';

@Component({
  imports: [TranslatePipe],
  selector: 'app-mission-feedback',
  styleUrl: './mission-feedback.scss',
  templateUrl: './mission-feedback.html',
})
export class MissionFeedbackComponent {
  private readonly store = inject(GameStore);
  private readonly destroyRef = inject(DestroyRef);
  private observedGameId: string | null = null;
  private observedTurn = -1;
  private feedbackTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly feedback = signal<{ successful: boolean; message: string } | null>(null);

  constructor() {
    effect(() => this.showLatestMissionFeedback());
    this.destroyRef.onDestroy(() => {
      if (this.feedbackTimer) clearTimeout(this.feedbackTimer);
    });
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
    this.feedback.set({ successful: latest.successful, message: latest.description });
    if (this.feedbackTimer) clearTimeout(this.feedbackTimer);
    this.feedbackTimer = setTimeout(() => this.feedback.set(null), 1_400);
  }
}
