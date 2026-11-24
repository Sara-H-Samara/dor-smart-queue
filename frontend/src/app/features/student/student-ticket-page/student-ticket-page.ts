import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { EMPTY, Observable, catchError, ignoreElements, switchMap, tap } from 'rxjs';

import { LiveUpdateService } from '../../../core/live-updates/live-update.service';
import { ActiveService } from '../../../core/models/active-service.model';
import { ActiveTicket, TicketDistance } from '../../../core/models/ticket.model';
import { AuthService } from '../../../core/services/auth.service';
import { StudentCatalogService } from '../../../core/services/student-catalog.service';
import { TicketService } from '../../../core/services/ticket.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import { DistancePicker } from './distance-picker/distance-picker';
import { firstNameOf } from './first-name';
import { ServiceTileList } from './service-tile-list/service-tile-list';
import { TicketCard } from './ticket-card/ticket-card';
import {
  isConflict,
  isNoActiveTicket,
  messageForTicketFailure,
  serverMessageOf,
} from './ticket-failure';

type StudentScreenState = 'loading' | 'ticket' | 'noTicket' | 'failed';

@Component({
  selector: 'app-student-ticket-page',
  imports: [PageHeader, TicketCard, ServiceTileList, DistancePicker],
  templateUrl: './student-ticket-page.html',
  styleUrl: './student-ticket-page.scss',
})
export class StudentTicketPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ticketService = inject(TicketService);
  private readonly studentCatalogService = inject(StudentCatalogService);
  private readonly liveUpdateService = inject(LiveUpdateService);

  protected readonly studentFirstName = computed(() => firstNameOf(this.authService.fullName()));
  protected readonly screenState = signal<StudentScreenState>('loading');
  protected readonly screenHeading = computed(() =>
    this.screenState() === 'noTicket' ? 'You have no ticket' : 'Your ticket',
  );
  protected readonly activeTicket = signal<ActiveTicket | null>(null);
  protected readonly activeServices = signal<ActiveService[]>([]);
  protected readonly chosenDistance = signal<TicketDistance>('NEAR');
  protected readonly isSubmitting = signal(false);
  protected readonly failureMessage = signal<string | null>(null);
  protected readonly takeTicketConflictMessage = signal<string | null>(null);

  constructor() {
    this.loadAfterStreamIsReadyAndRefreshOnLiveUpdates();
  }

  protected chooseDistance(distance: TicketDistance): void {
    this.chosenDistance.set(distance);
  }

  protected takeTicketForService(service: ActiveService): void {
    this.startSubmitting();
    this.ticketService.takeTicket(service.id, this.chosenDistance()).subscribe({
      next: () => this.loadMyActiveTicket(),
      error: (error: unknown) => {
        if (isConflict(error)) {
          this.takeTicketConflictMessage.set(serverMessageOf(error));
          this.loadMyActiveTicket();
        } else {
          this.showFailure(error);
        }
      },
    });
  }

  protected leaveQueue(): void {
    this.startSubmitting();
    this.ticketService.cancelMyActiveTicket().subscribe({
      next: () => this.loadMyActiveTicket(),
      error: (error: unknown) =>
        isNoActiveTicket(error) || isConflict(error)
          ? this.loadMyActiveTicket()
          : this.showFailure(error),
    });
  }

  protected signOut(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  protected retryAfterFailure(): void {
    this.failureMessage.set(null);
    this.loadMyActiveTicket();
  }

  private loadMyActiveTicket(): void {
    this.screenState.set('loading');
    this.ticketService.fetchMyActiveTicket().subscribe({
      next: (ticket) => {
        this.takeTicketConflictMessage.set(null);
        this.activeTicket.set(ticket);
        this.screenState.set('ticket');
        this.isSubmitting.set(false);
      },
      error: (error: unknown) => {
        if (isNoActiveTicket(error)) {
          this.showServiceListWithoutTicket();
        } else {
          this.showFailure(error);
        }
      },
    });
  }

  private loadAfterStreamIsReadyAndRefreshOnLiveUpdates(): void {
    const liveUpdateWatch = this.liveUpdateService.watch('student', this.destroyRef);
    liveUpdateWatch.firstLoadRequest
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadMyActiveTicket());
    liveUpdateWatch.refreshRequests
      .pipe(
        switchMap(() => this.refreshForCurrentScreenState()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private refreshForCurrentScreenState(): Observable<void> {
    if (this.hasTicketToRefresh()) {
      return this.refreshActiveTicket();
    }
    if (this.hasServiceListToRefresh()) {
      return this.refreshActiveServices();
    }
    return EMPTY;
  }

  private refreshActiveTicket(): Observable<void> {
    return this.ticketService.fetchMyActiveTicket().pipe(
      tap({
        next: (ticket) => {
          if (this.hasTicketToRefresh()) {
            this.activeTicket.set(ticket);
          }
        },
        error: (error: unknown) => {
          if (this.hasTicketToRefresh() && isNoActiveTicket(error)) {
            this.showServiceListWithoutTicket();
          }
        },
      }),
      catchError(() => EMPTY),
      ignoreElements(),
    );
  }

  private refreshActiveServices(): Observable<void> {
    const servicesBeforeRequest = this.activeServices();
    return this.studentCatalogService.listActiveServices().pipe(
      tap((services) => {
        if (this.hasServiceListToRefresh() && this.activeServices() === servicesBeforeRequest) {
          this.activeServices.set(services);
        }
      }),
      catchError(() => EMPTY),
      ignoreElements(),
    );
  }

  private hasServiceListToRefresh(): boolean {
    return this.screenState() === 'noTicket' && !this.isSubmitting();
  }

  private hasTicketToRefresh(): boolean {
    const status = this.activeTicket()?.status;
    return (
      this.screenState() === 'ticket' &&
      !this.isSubmitting() &&
      (status === 'WAITING' || status === 'CALLED')
    );
  }

  private showServiceListWithoutTicket(): void {
    this.activeTicket.set(null);
    this.screenState.set('loading');
    this.loadActiveServices();
  }

  private loadActiveServices(): void {
    this.studentCatalogService.listActiveServices().subscribe({
      next: (services) => {
        this.activeServices.set(services);
        this.screenState.set('noTicket');
        this.isSubmitting.set(false);
      },
      error: (error: unknown) => this.showFailure(error),
    });
  }

  private startSubmitting(): void {
    this.failureMessage.set(null);
    this.takeTicketConflictMessage.set(null);
    this.isSubmitting.set(true);
  }

  private showFailure(error: unknown): void {
    this.takeTicketConflictMessage.set(null);
    this.failureMessage.set(messageForTicketFailure(error));
    this.isSubmitting.set(false);
    this.screenState.set('failed');
  }
}
