import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, OnInit, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';

import { CatalogItem, CatalogResource } from '../../../../core/models/catalog.model';
import { CatalogService } from '../../../../core/services/catalog.service';
import { CatalogFormDialog, CatalogFormValue } from './catalog-form-dialog/catalog-form-dialog';

@Component({
  selector: 'app-catalog-panel',
  imports: [ReactiveFormsModule, CatalogFormDialog],
  templateUrl: './catalog-panel.html',
  styleUrl: './catalog-panel.scss',
})
export class CatalogPanel implements OnInit {
  private readonly catalog = inject(CatalogService);

  readonly resource = input.required<CatalogResource>();
  readonly heading = input.required<string>();
  readonly singular = input.required<string>();

  protected readonly items = signal<CatalogItem[]>([]);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly editingId = signal<number | null>(null);
  protected readonly addOpen = signal(false);

  private readonly addDialog = viewChild.required(CatalogFormDialog);

  protected readonly renameControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(100)],
  });

  protected readonly codeServerError = signal<string | null>(null);

  protected get hasCode(): boolean {
    return this.resource() === 'services';
  }

  ngOnInit(): void {
    this.reload();
  }

  protected openAdd(): void {
    this.error.set(null);
    this.addOpen.set(true);
    this.addDialog().open();
  }

  /** Runs when the dialog closes by any route, including the Esc key. */
  protected onAddClosed(): void {
    this.addOpen.set(false);
    this.codeServerError.set(null);
    this.error.set(null);
  }

  protected add(value: CatalogFormValue): void {
    const request = this.hasCode
      ? this.catalog.create(this.resource(), value.name, value.code ?? '')
      : this.catalog.create(this.resource(), value.name);
    this.codeServerError.set(null);
    this.run(request, () => this.addDialog().close());
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
