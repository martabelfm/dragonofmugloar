import { DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { GameStore } from '../../game.store';
import { TranslatePipe } from '../../translate.pipe';

@Component({
  imports: [DecimalPipe, TranslatePipe],
  selector: 'app-shop-panel',
  styleUrl: './shop-panel.scss',
  templateUrl: './shop-panel.html',
})
export class ShopPanelComponent {
  protected readonly store = inject(GameStore);
}
