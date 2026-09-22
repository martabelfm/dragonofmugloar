import { DecimalPipe } from '@angular/common';
import { Component, inject, output, viewChild } from '@angular/core';
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
  private readonly newGameDialog = viewChild.required(NewGameDialogComponent);
  private readonly guidanceDialog = viewChild.required(GuidanceDialogComponent);
  readonly newGame = output<void>();

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
}
