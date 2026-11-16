import { Component, Injector, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';

import { CONFLICT_STATUS, NOT_FOUND_STATUS, isHttpStatus, serverMessageOf } from '../../../core/api-failure';
import { AvailableCounter, CalledStudent } from '../../../core/models/staff-desk.model';
import { AuthService } from '../../../core/services/auth.service';
import { ChosenCounterStorage } from '../../../core/services/chosen-counter-storage';
import { StaffCounterService } from '../../../core/services/staff-counter.service';
import { StaffDeskService } from '../../../core/services/staff-desk.service';
import { CallNextPanel } from './call-next-panel/call-next-panel';
import { CounterPickerDialog } from './counter-picker-dialog/counter-picker-dialog';
import { CurrentStudentCard } from './current-student-card/current-student-card';

type StaffDeskState = 'loading' | 'noCounterChosen' | 'ready' | 'failed';

const LOAD_DESK_FAILURE_MESSAGE = 'Could not load the staff desk. Please try again.';
const LOAD_COUNTERS_FAILURE_MESSAGE = 'Could not load the counters. Please try again.';
const ACTION_FAILURE_MESSAGE = 'The action could not be completed. Please try again.';
const COUNTER_GONE_NOTICE = 'Your counter is no longer available. Choose another one.';

@Component({
  selector: 'app-staff-desk-page',
  imports: [CallNextPanel, CurrentStudentCard, CounterPickerDialog],
  templateUrl: './staff-desk-page.html',
  styleUrl: './staff-desk-page.scss',
})
export class StaffDeskPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly staffCounterService = inject(StaffCounterService);
  private readonly staffDeskService = inject(StaffDeskService);
  private readonly chosenCounterStorage = inject(ChosenCounterStorage);

  private readonly counterPicker = viewChild.required(CounterPickerDialog);

  protected readonly screenState = signal<StaffDeskState>('loading');
  protected readonly availableCounters = signal<AvailableCounter[]>([]);
  protected readonly chosenCounterId = signal<number | null>(this.chosenCounterStorage.read());
  protected readonly currentStudent = signal<CalledStudent | null>(null);
  protected readonly isSubmitting = signal(false);
  protected readonly failureMessage = signal<string | null>(null);
  protected readonly actionFailureMessage = signal<string | null>(null);
  protected readonly counterNotice = signal<string | null>(null);
  protected readonly nobodyWaitingMessage = signal<string | null>(null);
  protected readonly isLoadingCounters = signal(false);
  protected readonly counterListFailureMessage = signal<string | null>(null);

  protected readonly chosenCounter = computed(() =>
    this.availableCounters().find((counter) => counter.id === this.chosenCounterId()),
  );
  protected readonly canCallNext = computed(
    () => this.screenState() === 'ready' && this.currentStudent() === null,
  );

  constructor() {
    this.loadDesk();
  }

  protected changeCounter(): void {
    this.counterPicker().open();
    this.refreshAvailableCounters();
  }

  protected chooseCounter(counterId: number): void {
    this.chosenCounterStorage.save(counterId);
    this.chosenCounterId.set(counterId);
    this.counterNotice.set(null);
    this.actionFailureMessage.set(null);
    this.nobodyWaitingMessage.set(null);
    this.currentStudent.set(null);
    this.screenState.set('loading');
    this.loadCurrentStudent(counterId);
  }

  protected callNextStudent(): void {
    const counterId = this.chosenCounterId();
    if (counterId === null) {
      return;
    }
    this.startAction();
    this.staffDeskService.callNextStudent(counterId).subscribe({
      next: (calledStudent) => {
        if (calledStudent === null) {
          this.nobodyWaitingMessage.set(
            `Nobody is waiting for ${this.chosenCounter()?.serviceName ?? 'this service'}`,
          );
        }
        this.loadCurrentStudent(counterId);
      },
      error: (error: unknown) => this.handleActionFailure(error),
    });
  }

  protected markCurrentStudentDone(): void {
    this.finishCurrentStudent((ticketId) => this.staffDeskService.markTicketDone(ticketId));
  }

  protected markCurrentStudentNoShow(): void {
    this.finishCurrentStudent((ticketId) => this.staffDeskService.markTicketNoShow(ticketId));
  }

  protected retryAfterFailure(): void {
    this.failureMessage.set(null);
    this.screenState.set('loading');
    this.loadDesk();
  }

  protected signOut(): void {
    this.chosenCounterStorage.clear();
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  private finishCurrentStudent(markTicket: (ticketId: number) => Observable<CalledStudent>): void {
    const counterId = this.chosenCounterId();
    const student = this.currentStudent();
    if (counterId === null || student === null) {
      return;
    }
    this.startAction();
    markTicket(student.id).subscribe({
      next: () => this.loadCurrentStudent(counterId),
      error: (error: unknown) => this.handleActionFailure(error),
    });
  }

  private loadDesk(): void {
    this.isSubmitting.set(true);
    this.staffCounterService.listAvailableCounters().subscribe({
      next: (counters) => {
        this.availableCounters.set(counters);
        this.resumeWithChosenCounter();
      },
      error: (error: unknown) => this.showLoadFailure(error, LOAD_DESK_FAILURE_MESSAGE),
    });
  }

  private resumeWithChosenCounter(): void {
    if (this.chosenCounterId() === null) {
      this.showNoCounterChosen();
    } else if (this.chosenCounter() === undefined) {
      this.chosenCounterStorage.clear();
      this.chosenCounterId.set(null);
      this.counterNotice.set(COUNTER_GONE_NOTICE);
      this.showNoCounterChosen();
    } else {
      this.loadCurrentStudent(this.chosenCounterId()!);
    }
  }

  private loadCurrentStudent(counterId: number): void {
    this.isSubmitting.set(true);
    this.staffDeskService.fetchCurrentStudent(counterId).subscribe({
      next: (student) => {
        this.currentStudent.set(student);
        this.screenState.set('ready');
        this.isSubmitting.set(false);
      },
      error: (error: unknown) => this.showLoadFailure(error, LOAD_DESK_FAILURE_MESSAGE),
    });
  }

  private refreshAvailableCounters(): void {
    this.isLoadingCounters.set(true);
    this.counterListFailureMessage.set(null);
    this.staffCounterService.listAvailableCounters().subscribe({
      next: (counters) => {
        this.availableCounters.set(counters);
        this.isLoadingCounters.set(false);
      },
      error: (error: unknown) => {
        this.counterListFailureMessage.set(serverMessageOf(error, LOAD_COUNTERS_FAILURE_MESSAGE));
        this.isLoadingCounters.set(false);
      },
    });
  }

  private showNoCounterChosen(): void {
    this.currentStudent.set(null);
    this.screenState.set('noCounterChosen');
    this.isSubmitting.set(false);
    afterNextRender(() => this.counterPicker().open(), { injector: this.injector });
  }

  private startAction(): void {
    this.actionFailureMessage.set(null);
    this.nobodyWaitingMessage.set(null);
    this.isSubmitting.set(true);
  }

  private handleActionFailure(error: unknown): void {
    this.actionFailureMessage.set(serverMessageOf(error, ACTION_FAILURE_MESSAGE));
    if (isHttpStatus(error, NOT_FOUND_STATUS) || isHttpStatus(error, CONFLICT_STATUS)) {
      this.loadDesk();
    } else {
      this.isSubmitting.set(false);
    }
  }

  private showLoadFailure(error: unknown, fallbackMessage: string): void {
    this.failureMessage.set(serverMessageOf(error, fallbackMessage));
    this.screenState.set('failed');
    this.isSubmitting.set(false);
  }
}
