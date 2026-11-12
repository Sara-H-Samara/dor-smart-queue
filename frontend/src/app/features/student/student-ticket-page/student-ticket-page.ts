import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { ActiveService } from '../../../core/models/active-service.model';
import { ActiveTicket } from '../../../core/models/ticket.model';
import { AuthService } from '../../../core/services/auth.service';
import { StudentCatalogService } from '../../../core/services/student-catalog.service';
import { TicketService } from '../../../core/services/ticket.service';
import { firstNameOf } from './first-name';
import { ServiceTileList } from './service-tile-list/service-tile-list';
import { TicketCard } from './ticket-card/ticket-card';
import { isNoActiveTicket, isTicketAlreadyTaken, messageForTicketFailure } from './ticket-failure';

type StudentScreenState = 'loading' | 'ticket' | 'noTicket' | 'failed';

@Component({
  selector: 'app-student-ticket-page',
  imports: [TicketCard, ServiceTileList],
  templateUrl: './student-ticket-page.html',
  styleUrl: './student-ticket-page.scss',
})
export class StudentTicketPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ticketService = inject(TicketService);
  private readonly studentCatalogService = inject(StudentCatalogService);

  protected readonly studentFirstName = computed(() => firstNameOf(this.authService.fullName()));
  protected readonly screenState = signal<StudentScreenState>('loading');
  protected readonly activeTicket = signal<ActiveTicket | null>(null);
  protected readonly activeServices = signal<ActiveService[]>([]);
  protected readonly isSubmitting = signal(false);
  protected readonly failureMessage = signal<string | null>(null);

  constructor() {
    this.loadMyActiveTicket();
  }

  protected takeTicketForService(service: ActiveService): void {
    this.startSubmitting();
    this.ticketService.takeTicket(service.id).subscribe({
      next: () => this.loadMyActiveTicket(),
      error: (error: unknown) =>
        isTicketAlreadyTaken(error) ? this.loadMyActiveTicket() : this.showFailure(error),
    });
  }

  protected leaveQueue(): void {
    this.startSubmitting();
    this.ticketService.cancelMyActiveTicket().subscribe({
      next: () => this.loadMyActiveTicket(),
      error: (error: unknown) =>
        isNoActiveTicket(error) || isTicketAlreadyTaken(error)
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
        this.activeTicket.set(ticket);
        this.screenState.set('ticket');
        this.isSubmitting.set(false);
      },
      error: (error: unknown) => {
        if (isNoActiveTicket(error)) {
          this.activeTicket.set(null);
          this.loadActiveServices();
        } else {
          this.showFailure(error);
        }
      },
    });
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
    this.isSubmitting.set(true);
  }

  private showFailure(error: unknown): void {
    this.failureMessage.set(messageForTicketFailure(error));
    this.isSubmitting.set(false);
    this.screenState.set('failed');
  }
}
