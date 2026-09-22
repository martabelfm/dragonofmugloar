import { Component, inject, input } from '@angular/core';
import { riskClass } from '../../game.constants';
import { Advertisement } from '../../game.models';
import { GameStore } from '../../game.store';
import { TranslatePipe } from '../../translate.pipe';

@Component({
  imports: [TranslatePipe],
  selector: 'app-mission-card',
  styleUrl: './mission-card.scss',
  templateUrl: './mission-card.html',
})
export class MissionCardComponent {
  protected readonly store = inject(GameStore);
  readonly ad = input.required<Advertisement>();

  protected riskClass(): string {
    return riskClass(this.ad());
  }
}
