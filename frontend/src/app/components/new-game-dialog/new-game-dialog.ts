import { Component, ElementRef, inject, output, viewChild } from '@angular/core';
import { closeDialogOnBackdrop } from '../../dialog.util';
import { GameStore } from '../../game.store';
import { TranslatePipe } from '../../translate.pipe';

@Component({
  imports: [TranslatePipe],
  selector: 'app-new-game-dialog',
  styleUrl: './new-game-dialog.scss',
  templateUrl: './new-game-dialog.html',
})
export class NewGameDialogComponent {
  protected readonly store = inject(GameStore);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly confirmed = output<void>();

  open(): void {
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    this.dialog().nativeElement.close();
  }

  protected confirm(): void {
    this.close();
    this.confirmed.emit();
  }

  protected closeOnBackdrop(event: MouseEvent): void {
    closeDialogOnBackdrop(event, this.dialog().nativeElement);
  }
}
