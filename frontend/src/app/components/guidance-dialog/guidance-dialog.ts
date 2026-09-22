import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { closeDialogOnBackdrop } from '../../dialog.util';
import { StrategyMode } from '../../game.models';
import { GameStore } from '../../game.store';
import { TranslatePipe } from '../../translate.pipe';

@Component({
  imports: [TranslatePipe],
  selector: 'app-guidance-dialog',
  styleUrl: './guidance-dialog.scss',
  templateUrl: './guidance-dialog.html',
})
export class GuidanceDialogComponent {
  protected readonly store = inject(GameStore);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected readonly strategySelected = signal(false);

  open(): void {
    this.strategySelected.set(false);
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    this.dialog().nativeElement.close();
  }

  protected async selectMode(mode: StrategyMode): Promise<void> {
    const game =
      mode === this.store.guidanceMode() ? this.store.game() : await this.store.updateStrategyMode(mode);
    if (game) this.strategySelected.set(true);
  }

  protected disable(): void {
    this.store.disableGuidance();
    this.strategySelected.set(false);
    this.close();
  }

  protected runAutomation(): void {
    this.close();
    void this.store.runDecisionAutomation();
  }

  protected closeOnBackdrop(event: MouseEvent): void {
    closeDialogOnBackdrop(event, this.dialog().nativeElement);
  }
}
