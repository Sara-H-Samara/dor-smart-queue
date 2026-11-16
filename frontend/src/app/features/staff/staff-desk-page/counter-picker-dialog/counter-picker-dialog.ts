import { Component, ElementRef, input, output, viewChild } from '@angular/core';

import { AvailableCounter } from '../../../../core/models/staff-desk.model';

@Component({
  selector: 'app-counter-picker-dialog',
  templateUrl: './counter-picker-dialog.html',
  styleUrl: './counter-picker-dialog.scss',
})
export class CounterPickerDialog {
  readonly counters = input<AvailableCounter[]>([]);
  readonly chosenCounterId = input<number | null>(null);
  readonly isLoading = input(false);
  readonly failureMessage = input<string | null>(null);

  readonly counterChosen = output<number>();
  readonly closed = output<void>();

  private readonly counterPickerElement =
    viewChild.required<ElementRef<HTMLDialogElement>>('counterPickerElement');

  open(): void {
    this.counterPickerElement().nativeElement.showModal();
  }

  private counterWasChosen = false;

  protected chooseCounter(counter: AvailableCounter): void {
    this.counterWasChosen = true;
    this.counterPickerElement().nativeElement.close();
    this.counterChosen.emit(counter.id);
  }

  protected closeWithoutChoosing(): void {
    this.counterPickerElement().nativeElement.close();
  }

  protected emitClosedUnlessCounterWasChosen(): void {
    const wasDismissedWithoutChoice = !this.counterWasChosen;
    this.counterWasChosen = false;
    if (wasDismissedWithoutChoice) {
      this.closed.emit();
    }
  }
}
