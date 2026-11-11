import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, input, OnInit, signal, viewChild } from '@angular/core';
import { Observable } from 'rxjs';

import { CatalogItem, CatalogResource } from '../../../../core/models/catalog.model';
import { CatalogService } from '../../../../core/services/catalog.service';
import { CatalogFormDialog, CatalogFormValue } from './catalog-form-dialog/catalog-form-dialog';

@Component({
  selector: 'app-catalog-panel',
  imports: [CatalogFormDialog],
  templateUrl: './catalog-panel.html',
  styleUrl: './catalog-panel.scss',
})
export class CatalogPanel implements OnInit {
  private readonly catalogService = inject(CatalogService);

  readonly catalogResource = input.required<CatalogResource>();
  readonly heading = input.required<string>();
  readonly itemLabel = input.required<string>();

  protected readonly items = signal<CatalogItem[]>([]);
  protected readonly isLoadingList = signal(false);
  protected readonly isSaving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly dialogOpen = signal(false);
  protected readonly dialogMode = signal<'add' | 'rename'>('add');
  private readonly renamingItem = signal<CatalogItem | null>(null);

  private readonly formDialog = viewChild.required(CatalogFormDialog);

  protected readonly codeServerError = signal<string | null>(null);

  protected readonly supportsCode = computed(() => this.catalogResource() === 'services');

  ngOnInit(): void {
    this.reloadItems();
  }

  protected openAdd(): void {
    this.error.set(null);
    this.dialogMode.set('add');
    this.dialogOpen.set(true);
    this.formDialog().open();
  }

  protected openRename(item: CatalogItem): void {
    this.error.set(null);
    this.dialogMode.set('rename');
    this.renamingItem.set(item);
    this.dialogOpen.set(true);
    this.formDialog().open(item.name);
  }

  /** Runs when the dialog closes by any route, including the Esc key. */
  protected onDialogClosed(): void {
    this.dialogOpen.set(false);
    this.dialogMode.set('add');
    this.renamingItem.set(null);
    this.codeServerError.set(null);
    this.error.set(null);
  }

  protected onFormSubmitted(formValue: CatalogFormValue): void {
    const item = this.renamingItem();
    if (this.dialogMode() === 'rename' && item) {
      this.renameItem(item, formValue.name);
    } else {
      this.createItem(formValue);
    }
  }

  private createItem(value: CatalogFormValue): void {
    const createCall$ = this.supportsCode()
      ? this.catalogService.create(this.catalogResource(), value.name, value.code ?? '')
      : this.catalogService.create(this.catalogResource(), value.name);
    this.codeServerError.set(null);
    this.sendAndReload(createCall$, () => this.formDialog().closeAndReset());
  }

  private renameItem(item: CatalogItem, name: string): void {
    this.sendAndReload(this.catalogService.rename(this.catalogResource(), item.id, name), () => this.formDialog().closeAndReset());
  }

  protected toggleItemActive(item: CatalogItem): void {
    this.sendAndReload(this.catalogService.setActive(this.catalogResource(), item.id, !item.active));
  }

  private sendAndReload(request: Observable<unknown>, onSuccess?: () => void): void {
    this.error.set(null);
    this.isSaving.set(true);
    request.subscribe({
      next: () => {
        onSuccess?.();
        this.isSaving.set(false);
        this.reloadItems();
      },
      error: (err: unknown) => {
        this.isSaving.set(false);
        const duplicateCode = this.duplicateCodeMessage(err);
        if (duplicateCode) {
          this.codeServerError.set(duplicateCode);
        } else {
          this.error.set(this.errorMessageForStatus(err));
        }
      },
    });
  }

  /** The server's message when a 409 is about the code rather than the name. */
  private duplicateCodeMessage(err: unknown): string | null {
    if (!this.supportsCode() || !(err instanceof HttpErrorResponse) || err.status !== 409) {
      return null;
    }
    const body = err.error as { message?: unknown } | string | null;
    const message = typeof body === 'string' ? body : body?.message;
    // Matches the server message text, so it breaks if that wording changes.
    return typeof message === 'string' && /\bcode\b/i.test(message) && /already used/i.test(message)
      ? message
      : null;
  }

  private reloadItems(): void {
    this.isLoadingList.set(true);
    this.catalogService.listItems(this.catalogResource()).subscribe({
      next: (items) => {
        this.items.set(items);
        this.isLoadingList.set(false);
      },
      error: (err: unknown) => {
        this.isLoadingList.set(false);
        this.error.set(this.errorMessageForStatus(err));
      },
    });
  }

  private errorMessageForStatus(err: unknown): string {
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
