import { DecimalPipe } from '@angular/common';
import { Component, ElementRef, inject, viewChild } from '@angular/core';
import { GameStore } from '../../game.store';
import { TranslatePipe } from '../../translate.pipe';
import { ChronicleDialogComponent } from '../chronicle-dialog/chronicle-dialog';

@Component({
  imports: [DecimalPipe, TranslatePipe, ChronicleDialogComponent],
  selector: 'app-action-rail',
  styleUrl: './action-rail.scss',
  templateUrl: './action-rail.html',
})
export class ActionRailComponent {
  protected readonly store = inject(GameStore);
  private readonly chronicleDialog = viewChild.required(ChronicleDialogComponent);

  protected openChronicle(): void {
    this.chronicleDialog().open();
  }
}
