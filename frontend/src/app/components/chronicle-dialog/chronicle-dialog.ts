import { Component, ElementRef, inject, viewChild } from '@angular/core';
import { closeDialogOnBackdrop } from '../../dialog.util';
import { GameStore } from '../../game.store';
import { TranslatePipe } from '../../translate.pipe';

@Component({
  imports: [TranslatePipe],
  selector: 'app-chronicle-dialog',
  styleUrl: './chronicle-dialog.scss',
  templateUrl: './chronicle-dialog.html',
})
export class ChronicleDialogComponent {
  protected readonly store = inject(GameStore);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  open(): void {
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    this.dialog().nativeElement.close();
  }

  protected closeOnBackdrop(event: MouseEvent): void {
    closeDialogOnBackdrop(event, this.dialog().nativeElement);
  }
}
