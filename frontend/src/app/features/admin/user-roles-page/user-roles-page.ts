import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  EMPTY,
  Subject,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  skip,
  startWith,
  switchMap,
} from 'rxjs';

import { NOT_FOUND_STATUS, UNAUTHORIZED_STATUS, isHttpStatus } from '../../../core/api-failure';
import { UserPage, UserRole, UserRow } from '../../../core/models/user-role.model';
import { AuthService } from '../../../core/services/auth.service';
import { UserRoleService } from '../../../core/services/user-role.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import {
  PendingRoleChange,
  RoleChangeConfirmation,
} from './role-change-confirmation/role-change-confirmation';
import { roleChangeErrorMessage, userListErrorMessage } from './role-change-errors';
import { ROLE_LABELS, needsConfirmation } from './role-labels';
import { UserRoleRow } from './user-role-row/user-role-row';

const PAGE_SIZE = 20;
const SEARCH_DELAY_MILLIS = 300;

interface UserListQuery {
  search: string;
  page: number;
}

interface RoleChangeTarget {
  user: UserRow;
  newRole: UserRole;
}

@Component({
  selector: 'app-user-roles-page',
  imports: [PageHeader, ReactiveFormsModule, RoleChangeConfirmation, RouterLink, UserRoleRow],
  templateUrl: './user-roles-page.html',
  styleUrl: './user-roles-page.scss',
})
export class UserRolesPage {
  private readonly userRoleService = inject(UserRoleService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly loadRequests = new Subject<UserListQuery>();
  private readonly roleChangeTarget = signal<RoleChangeTarget | null>(null);
  private currentSearch = '';

  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly users = signal<UserRow[]>([]);
  protected readonly currentPage = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly totalUsers = signal(0);
  protected readonly loadedSearch = signal('');
  protected readonly hasLoaded = signal(false);
  protected readonly isLoading = signal(false);
  protected readonly loadErrorMessage = signal<string | null>(null);
  protected readonly changeErrorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly savingUserId = signal<number | null>(null);

  protected readonly pendingRoleChange = computed<PendingRoleChange | null>(() => {
    const target = this.roleChangeTarget();
    return target
      ? { fullName: target.user.fullName, oldRole: target.user.role, newRole: target.newRole }
      : null;
  });

  protected readonly rowsAreBusy = computed(
    () => this.isLoading() || this.savingUserId() !== null || this.roleChangeTarget() !== null,
  );

  protected readonly isFirstPage = computed(() => this.currentPage() === 0);
  protected readonly isLastPage = computed(() => this.currentPage() + 1 >= this.totalPages());
  protected readonly userCountText = computed(() =>
    this.totalUsers() === 1 ? '1 user' : `${this.totalUsers()} users`,
  );

  constructor() {
    this.loadUsersOnRequest();
    this.searchAfterTyping();
    this.requestLoad(0);
  }

  protected goToPreviousPage(): void {
    if (this.isFirstPage() || this.isLoading()) {
      return;
    }
    this.clearMessages();
    this.requestLoad(this.currentPage() - 1);
  }

  protected goToNextPage(): void {
    if (this.isLastPage() || this.isLoading()) {
      return;
    }
    this.clearMessages();
    this.requestLoad(this.currentPage() + 1);
  }

  protected retryLoading(): void {
    this.clearMessages();
    this.requestLoad(this.currentPage());
  }

  protected clearSearch(): void {
    this.searchControl.setValue('');
  }

  protected requestRoleChange(user: UserRow, newRole: UserRole): void {
    if (this.rowsAreBusy()) {
      return;
    }
    this.clearMessages();
    if (needsConfirmation(user.role, newRole)) {
      this.roleChangeTarget.set({ user, newRole });
      return;
    }
    this.saveRoleChange(user, newRole);
  }

  protected confirmRoleChange(): void {
    const target = this.roleChangeTarget();
    this.roleChangeTarget.set(null);
    if (target) {
      this.saveRoleChange(target.user, target.newRole);
    }
  }

  protected cancelRoleChange(): void {
    this.roleChangeTarget.set(null);
  }

  protected signOut(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  private loadUsersOnRequest(): void {
    this.loadRequests
      .pipe(
        switchMap((query) => {
          this.isLoading.set(true);
          return this.userRoleService.listUsers(query.search, query.page, PAGE_SIZE).pipe(
            map((userPage) => ({ query, userPage })),
            catchError((error: unknown) => {
              this.showLoadError(error);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ query, userPage }) => this.showLoadedPage(query, userPage));
  }

  private searchAfterTyping(): void {
    this.searchControl.valueChanges
      .pipe(
        map((text) => text.trim()),
        debounceTime(SEARCH_DELAY_MILLIS),
        startWith(''),
        distinctUntilChanged(),
        skip(1),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((search) => {
        this.currentSearch = search;
        this.clearMessages();
        this.requestLoad(0);
      });
  }

  private requestLoad(page: number): void {
    this.loadRequests.next({ search: this.currentSearch, page });
  }

  private showLoadedPage(query: UserListQuery, userPage: UserPage): void {
    this.users.set(userPage.users);
    this.currentPage.set(userPage.page);
    this.totalPages.set(userPage.totalPages);
    this.totalUsers.set(userPage.totalUsers);
    this.loadedSearch.set(query.search);
    this.hasLoaded.set(true);
    this.isLoading.set(false);
  }

  private showLoadError(error: unknown): void {
    this.isLoading.set(false);
    if (!isHttpStatus(error, UNAUTHORIZED_STATUS)) {
      this.loadErrorMessage.set(userListErrorMessage(error));
    }
  }

  private saveRoleChange(user: UserRow, newRole: UserRole): void {
    this.savingUserId.set(user.id);
    this.userRoleService
      .changeRole(user.id, newRole)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedUser) => {
          this.savingUserId.set(null);
          this.replaceRow(updatedUser);
          const roleLabel = ROLE_LABELS[updatedUser.role];
          this.successMessage.set(
            `${updatedUser.fullName} is now ${roleLabel}. ${updatedUser.fullName} must sign in again.`,
          );
        },
        error: (error: unknown) => {
          this.savingUserId.set(null);
          this.showRoleChangeError(error);
        },
      });
  }

  private showRoleChangeError(error: unknown): void {
    if (isHttpStatus(error, UNAUTHORIZED_STATUS)) {
      return;
    }
    this.changeErrorMessage.set(roleChangeErrorMessage(error));
    if (isHttpStatus(error, NOT_FOUND_STATUS)) {
      this.requestLoad(this.currentPage());
    }
  }

  private replaceRow(updatedUser: UserRow): void {
    this.users.update((rows) => rows.map((row) => (row.id === updatedUser.id ? updatedUser : row)));
  }

  private clearMessages(): void {
    this.loadErrorMessage.set(null);
    this.changeErrorMessage.set(null);
    this.successMessage.set(null);
  }
}
