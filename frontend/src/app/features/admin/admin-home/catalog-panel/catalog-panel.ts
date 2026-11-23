import {
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  ignoreElements,
  merge,
  switchMap,
  takeUntil,
  tap,
} from 'rxjs';

import { AppIcon } from '../../../../shared/icon/app-icon';
import { CatalogItem, CatalogResource } from '../../../../core/models/catalog.model';
import { CatalogService } from '../../../../core/services/catalog.service';
import {
  counterErrorMessageForStatus,
  errorMessageForStatus,
  findDuplicateCodeMessage,
  httpStatusOf,
  noActiveServiceMessage,
} from './catalog-errors';
import { CatalogFormDialog, CatalogFormValue } from './catalog-form-dialog/catalog-form-dialog';
import { CatalogRow } from './catalog-row/catalog-row';

@Component({
  selector: 'app-catalog-panel',
  imports: [AppIcon, CatalogFormDialog, CatalogRow],
  templateUrl: './catalog-panel.html',
  styleUrl: './catalog-panel.scss',
})
export class CatalogPanel implements OnInit {
  private readonly catalogService = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly itemsRequestStarted = new Subject<void>();
  private readonly droppedRefreshRetries = new Subject<void>();
  private hasDroppedRefreshRequest = false;

  readonly catalogResource = input.required<CatalogResource>();
  readonly refreshRequests = input.required<Observable<void>>();
  readonly heading = input.required<string>();
  readonly itemLabel = input.required<string>();
  readonly emptyText = input.required<string>();

  protected readonly items = signal<CatalogItem[]>([]);
  protected readonly isLoadingList = signal(false);
  protected readonly isSaving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly dialogOpen = signal(false);
  protected readonly dialogMode = signal<'add' | 'edit'>('add');
  private readonly editingItem = signal<CatalogItem | null>(null);
  private readonly loadedServices = signal<CatalogItem[]>([]);

  private readonly formDialog = viewChild.required(CatalogFormDialog);

  protected readonly codeConflictMessage = signal<string | null>(null);

  protected readonly supportsCode = computed(() => this.catalogResource() === 'services');
  protected readonly supportsService = computed(() => this.catalogResource() === 'counters');
  protected readonly editVerb = computed(() => (this.supportsService() ? 'Edit' : 'Rename'));

  protected readonly serviceOptions = computed(() => {
    const currentServiceId = this.editingItem()?.serviceId;
    return this.loadedServices().filter(
      (service) => service.active || service.id === currentServiceId,
    );
  });

  protected readonly itemCountText = computed(() => {
    const count = this.items().length;
    const label = count === 1 ? this.itemLabel() : this.heading();
    return `${count} ${label.toLowerCase()}`;
  });

  ngOnInit(): void {
    this.reloadItems();
    this.refreshItemsOnLiveUpdates();
  }

  protected tileTextFor(item: CatalogItem): string | null {
    return this.supportsCode() ? (item.code ?? null) : null;
  }

  protected detailTextFor(item: CatalogItem): string | null {
    if (this.supportsService()) {
      return item.serviceName ?? null;
    }
    return this.supportsCode() && item.code ? `Code ${item.code}` : null;
  }

  protected openAdd(): void {
    this.error.set(null);
    this.loadServices(() => {
      const activeServices = this.loadedServices().filter((service) => service.active);
      if (this.supportsService() && activeServices.length === 0) {
        this.error.set(noActiveServiceMessage);
        return;
      }
      if (this.dialogOpen()) {
        return;
      }
      this.dialogMode.set('add');
      this.dialogOpen.set(true);
      this.formDialog().open({
        serviceId: activeServices.length === 1 ? activeServices[0].id : undefined,
      });
    });
  }

  protected openEdit(item: CatalogItem): void {
    this.error.set(null);
    this.loadServices(() => {
      if (this.dialogOpen()) {
        return;
      }
      this.dialogMode.set('edit');
      this.editingItem.set(item);
      this.dialogOpen.set(true);
      this.formDialog().open({ name: item.name, serviceId: item.serviceId });
    });
  }

  protected onDialogClosed(): void {
    this.dialogOpen.set(false);
    this.dialogMode.set('add');
    this.editingItem.set(null);
    this.codeConflictMessage.set(null);
    this.error.set(null);
    if (this.hasDroppedRefreshRequest) {
      this.droppedRefreshRetries.next();
    }
  }

  protected onFormSubmitted(formValue: CatalogFormValue): void {
    const item = this.editingItem();
    if (this.dialogMode() === 'edit' && item) {
      this.saveEditedItem(item, formValue);
    } else {
      this.createItem(formValue);
    }
  }

  private createItem(value: CatalogFormValue): void {
    this.codeConflictMessage.set(null);
    this.sendAndReload(this.createRequestFor(value), () => this.formDialog().closeAndReset());
  }

  private createRequestFor(value: CatalogFormValue): Observable<unknown> {
    if (value.serviceId !== undefined) {
      return this.catalogService.createCounter(value.name, value.serviceId);
    }
    return this.catalogService.create(this.catalogResource(), value.name, value.code ?? '');
  }

  private saveEditedItem(item: CatalogItem, value: CatalogFormValue): void {
    const request =
      value.serviceId !== undefined
        ? this.catalogService.updateCounter(item.id, value.name, value.serviceId)
        : this.catalogService.rename(this.catalogResource(), item.id, value.name);
    this.sendAndReload(request, () => this.formDialog().closeAndReset());
  }

  protected toggleItemActive(item: CatalogItem): void {
    this.sendAndReload(
      this.catalogService.setActive(this.catalogResource(), item.id, !item.active),
    );
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
        const duplicateCode = findDuplicateCodeMessage(err, this.supportsCode());
        if (duplicateCode) {
          this.codeConflictMessage.set(duplicateCode);
        } else {
          this.error.set(this.errorMessageFor(err));
          this.refreshAfterRejection(err);
        }
      },
    });
  }

  private errorMessageFor(err: unknown): string {
    return this.supportsService() ? counterErrorMessageForStatus(err) : errorMessageForStatus(err);
  }

  private refreshAfterRejection(err: unknown): void {
    const status = httpStatusOf(err);
    if (status === 404) {
      this.reloadItems();
    }
    if (this.supportsService() && (status === 404 || status === 400)) {
      this.loadServices();
    }
  }

  private loadServices(onLoaded?: () => void): void {
    if (!this.supportsService()) {
      onLoaded?.();
      return;
    }
    this.catalogService.listItems('services').subscribe({
      next: (services) => {
        this.loadedServices.set(services);
        onLoaded?.();
      },
      error: (err: unknown) => this.error.set(errorMessageForStatus(err)),
    });
  }

  private refreshItemsOnLiveUpdates(): void {
    merge(this.refreshRequests(), this.droppedRefreshRetries)
      .pipe(
        switchMap(() => this.refreshItemsWhenNothingIsBeingEdited()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private refreshItemsWhenNothingIsBeingEdited(): Observable<void> {
    if (this.isSaving() || this.dialogOpen()) {
      this.hasDroppedRefreshRequest = true;
      return EMPTY;
    }
    return this.requestItemsAndDropOlderRequests().pipe(
      tap((items) => {
        this.items.set(items);
        this.isLoadingList.set(false);
      }),
      catchError(() => {
        this.isLoadingList.set(false);
        return EMPTY;
      }),
      ignoreElements(),
    );
  }

  private requestItemsAndDropOlderRequests(): Observable<CatalogItem[]> {
    this.hasDroppedRefreshRequest = false;
    this.itemsRequestStarted.next();
    return this.catalogService
      .listItems(this.catalogResource())
      .pipe(takeUntil(this.itemsRequestStarted));
  }

  private reloadItems(): void {
    this.isLoadingList.set(true);
    this.requestItemsAndDropOlderRequests().subscribe({
      next: (items) => {
        this.items.set(items);
        this.isLoadingList.set(false);
      },
      error: (err: unknown) => {
        this.isLoadingList.set(false);
        this.error.set(errorMessageForStatus(err));
      },
    });
  }
}
