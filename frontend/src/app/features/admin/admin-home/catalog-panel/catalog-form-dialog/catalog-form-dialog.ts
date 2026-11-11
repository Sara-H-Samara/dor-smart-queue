import { Component, ElementRef, input, output, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

export interface CatalogFormValue {
  name: string;
  code?: string;
}

/** Modal form for adding or renaming a catalog item. The parent owns the API calls. */
@Component({
  selector: 'app-catalog-form-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './catalog-form-dialog.html',
  styleUrl: './catalog-form-dialog.scss',
})
export class CatalogFormDialog {
  /** Prefix for the element ids (aria wiring), e.g. the resource name. */
  readonly idPrefix = input.required<string>();
  readonly heading = input.required<string>();
  readonly nameLabel = input.required<string>();
  readonly submitLabel = input('Add');
  readonly showCodeField = input(false);
  readonly isSaving = input(false);
  readonly errorMessage = input<string | null>(null);
  readonly codeConflictMessage = input<string | null>(null);

  readonly submitted = output<CatalogFormValue>();
  readonly closed = output<void>();
  /** The user typed in the code field (the parent clears its server error). */
  readonly codeEdited = output<void>();

  private readonly dialogElement = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly nameInput = viewChild.required<ElementRef<HTMLInputElement>>('nameInput');

  protected readonly nameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(100)],
  });
  protected readonly codeControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/^[A-Za-z]{1,2}$/)],
  });
  protected readonly submitAttempted = signal(false);

  protected get shouldShowCodeError(): boolean {
    return this.codeControl.invalid && (this.codeControl.touched || this.submitAttempted());
  }

  /** Opens the dialog; an initial name pre-fills the field and is selected. */
  open(initialName?: string): void {
    if (initialName !== undefined) {
      this.nameControl.setValue(initialName);
    }
    this.dialogElement().nativeElement.showModal();
    const nameField = this.nameInput().nativeElement;
    nameField.focus();
    nameField.select();
  }

  closeAndReset(): void {
    this.dialogElement().nativeElement.close();
    this.reset();
  }

  /** Runs when the dialog closes by any route, including the Esc key. */
  protected onDialogClose(): void {
    this.reset();
    this.closed.emit();
  }

  protected normalizeCodeInput(): void {
    const cleaned = this.codeControl.value.replace(/[^A-Za-z]/g, '').toUpperCase().slice(0, 2);
    if (cleaned !== this.codeControl.value) {
      this.codeControl.setValue(cleaned);
    }
    this.codeEdited.emit();
  }

  protected validateAndEmit(): void {
    const name = this.nameControl.value.trim();
    if (this.showCodeField()) {
      this.submitAttempted.set(true);
    }
    if (!name) {
      this.nameControl.markAsTouched();
    }
    if (!name || (this.showCodeField() && this.codeControl.invalid)) {
      return;
    }
    this.submitted.emit(
      this.showCodeField() ? { name, code: this.codeControl.value.toUpperCase() } : { name },
    );
  }

  private reset(): void {
    this.nameControl.reset('');
    this.codeControl.reset('');
    this.submitAttempted.set(false);
  }
}
