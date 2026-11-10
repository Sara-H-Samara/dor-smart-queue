import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';

import { CatalogItem, CatalogResource } from '../../../core/models/catalog.model';
import { CatalogService } from '../../../core/services/catalog.service';

@Component({
  selector: 'app-catalog-panel',
  imports: [ReactiveFormsModule],
  templateUrl: './catalog-panel.html',
  styleUrl: './catalog-panel.scss',
})
export class CatalogPanel implements OnInit {
  private readonly catalog = inject(CatalogService);

  readonly resource = input.required<CatalogResource>();
  readonly title = input.required<string>();
  readonly singular = input.required<string>();

  protected readonly items = signal<CatalogItem[]>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);

  protected readonly addControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(100)],
  });
  protected readonly renameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(100)],
  });

  protected readonly codeControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/^[A-Za-z]{1,2}$/)],
  });
  protected readonly codeServerError = signal<string | null>(null);
  protected readonly submitted = signal(false);

  protected get hasCode(): boolean {
    return this.resource() === 'services';
  }

  protected get showCodeError(): boolean {
    return this.codeControl.invalid && (this.codeControl.touched || this.submitted());
  }

  ngOnInit(): void {
    this.reload();
  }

  protected onCodeInput(): void {
    const cleaned = this.codeControl.value.replace(/[^A-Za-z]/g, '').toUpperCase().slice(0, 2);
    if (cleaned !== this.codeControl.value) {
      this.codeControl.setValue(cleaned);
    }
    this.codeServerError.set(null);
  }

  protected add(): void {
    const name = this.addControl.value.trim();
    if (this.hasCode) {
      this.submitted.set(true);
    }
    if (!name) {
      this.addControl.markAsTouched();
    }
    if (!name || (this.hasCode && this.codeControl.invalid)) {
      return;
    }
    const request = this.hasCode
      ? this.catalog.create(this.resource(), name, this.codeControl.value.toUpperCase())
      : this.catalog.create(this.resource(), name);
    this.codeServerError.set(null);
    this.run(request, () => {
      this.addControl.reset('');
      this.codeControl.reset('');
      this.submitted.set(false);
    });
  }

  protected startEdit(item: CatalogItem): void {
    this.error.set(null);
    this.renameControl.setValue(item.name);
    this.editingId.set(item.id);
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
  }

  protected saveEdit(item: CatalogItem): void {
    const name = this.renameControl.value.trim();
    if (!name) {
      this.renameControl.markAsTouched();
      return;
    }
    this.run(this.catalog.rename(this.resource(), item.id, name), () => this.editingId.set(null));
  }

  protected toggle(item: CatalogItem): void {
    this.run(this.catalog.setActive(this.resource(), item.id, !item.active));
  }

  private run(request: Observable<unknown>, onSuccess?: () => void): void {
    this.error.set(null);
    this.busy.set(true);
    request.subscribe({
      next: () => {
        onSuccess?.();
        this.busy.set(false);
        this.reload();
      },
      error: (err: unknown) => {
        this.busy.set(false);
        const duplicateCode = this.duplicateCodeMessage(err);
        if (duplicateCode) {
          this.codeServerError.set(duplicateCode);
        } else {
          this.error.set(this.messageFor(err));
        }
      },
    });
  }

  /** The server's message when a 409 is about the code rather than the name. */
  private duplicateCodeMessage(err: unknown): string | null {
    if (!this.hasCode || !(err instanceof HttpErrorResponse) || err.status !== 409) {
      return null;
    }
    const body = err.error as { message?: unknown } | string | null;
    const message = typeof body === 'string' ? body : body?.message;
    return typeof message === 'string' && /\bcode\b/i.test(message) && /already used/i.test(message)
      ? message
      : null;
  }

  private reload(): void {
    this.loading.set(true);
    this.catalog.list(this.resource()).subscribe({
      next: (items) => {
        this.items.set(items);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(this.messageFor(err));
      },
    });
  }

  private messageFor(err: unknown): string {
    const status = err instanceof HttpErrorResponse ? err.status : 0;
    switch (status) {
      case 409:
        return 'This name already exists. Please choose a different name.';
      case 404:
        return 'This item was not found. It may have been removed; the list is refreshed.';
      case 400:
        return 'The name is not valid. Please enter a name of 1 to 100 characters.';
      default:
        return 'Something went wrong. Please try again.';
    }
  }
}
