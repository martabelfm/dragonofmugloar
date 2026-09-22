import { DecimalPipe } from '@angular/common';
import { Component, inject, output } from '@angular/core';
import { GameStore } from '../../game.store';
import { TranslatePipe } from '../../translate.pipe';
import { MissionCardComponent } from '../mission-card/mission-card';

@Component({
  imports: [DecimalPipe, TranslatePipe, MissionCardComponent],
  selector: 'app-mission-board',
  styleUrl: './mission-board.scss',
  templateUrl: './mission-board.html',
})
export class MissionBoardComponent {
  protected readonly store = inject(GameStore);
  readonly playAgain = output<void>();
}
