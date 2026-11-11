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
  readonly showCode = input(false);
  readonly busy = input(false);
  readonly error = input<string | null>(null);
  readonly codeServerError = input<string | null>(null);

  readonly submitted = output<CatalogFormValue>();
  readonly closed = output<void>();
  /** The user typed in the code field (the parent clears its server error). */
  readonly codeEdited = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly nameInput = viewChild.required<ElementRef<HTMLInputElement>>('nameInput');

  protected readonly nameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(100)],
  });
  protected readonly codeControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/^[A-Za-z]{1,2}$/)],
  });
  protected readonly attempted = signal(false);

  protected get showCodeError(): boolean {
    return this.codeControl.invalid && (this.codeControl.touched || this.attempted());
  }

  /** Opens the dialog; an initial name pre-fills the field and is selected. */
  open(initialName?: string): void {
    if (initialName !== undefined) {
      this.nameControl.setValue(initialName);
    }
    this.dialog().nativeElement.showModal();
    const input = this.nameInput().nativeElement;
    input.focus();
    input.select();
  }

  close(): void {
    this.dialog().nativeElement.close();
    this.reset();
  }

  /** Runs when the dialog closes by any route, including the Esc key. */
  protected onDialogClose(): void {
    this.reset();
    this.closed.emit();
  }

  protected onCodeInput(): void {
    const cleaned = this.codeControl.value.replace(/[^A-Za-z]/g, '').toUpperCase().slice(0, 2);
    if (cleaned !== this.codeControl.value) {
      this.codeControl.setValue(cleaned);
    }
    this.codeEdited.emit();
  }

  protected submit(): void {
    const name = this.nameControl.value.trim();
    if (this.showCode()) {
      this.attempted.set(true);
    }
    if (!name) {
      this.nameControl.markAsTouched();
    }
    if (!name || (this.showCode() && this.codeControl.invalid)) {
      return;
    }
    this.submitted.emit(
      this.showCode() ? { name, code: this.codeControl.value.toUpperCase() } : { name },
    );
  }

  private reset(): void {
    this.nameControl.reset('');
    this.codeControl.reset('');
    this.attempted.set(false);
  }
}
