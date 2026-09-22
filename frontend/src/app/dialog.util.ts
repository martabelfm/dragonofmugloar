/** Closes a native `<dialog>` when the click lands on its backdrop rather than its content. */
export function closeDialogOnBackdrop(event: MouseEvent, dialog: HTMLDialogElement): void {
  if (event.target === dialog) dialog.close();
}
