import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, catchError, exhaustMap, ignoreElements, interval, tap } from 'rxjs';

import { WaitingRoom } from '../../../core/models/waiting-room.model';
import { WaitingRoomService } from '../../../core/services/waiting-room.service';
import { CounterTile } from './counter-tile/counter-tile';
import { NextTicketList } from './next-ticket-list/next-ticket-list';
import { ORGANIZATION_NAME } from './organization-name';
import { WaitingScreenHeader } from './waiting-screen-header/waiting-screen-header';
import { WAITING_SCREEN_REFRESH_INTERVAL_MS } from './waiting-screen-refresh';

type WaitingScreenState = 'loading' | 'ready' | 'failed';

@Component({
  selector: 'app-waiting-screen-page',
  imports: [WaitingScreenHeader, CounterTile, NextTicketList],
  templateUrl: './waiting-screen-page.html',
  styleUrl: './waiting-screen-page.scss',
})
export class WaitingScreenPage {
  private readonly destroyRef = inject(DestroyRef);
  private readonly waitingRoomService = inject(WaitingRoomService);

  protected readonly organizationName = ORGANIZATION_NAME;
  protected readonly screenState = signal<WaitingScreenState>('loading');
  protected readonly waitingRoom = signal<WaitingRoom | null>(null);

  constructor() {
    this.loadWaitingRoom();
    this.startAutoRefresh();
  }

  private loadWaitingRoom(): void {
    this.refreshWaitingRoom().subscribe();
  }

  private startAutoRefresh(): void {
    interval(WAITING_SCREEN_REFRESH_INTERVAL_MS)
      .pipe(
        exhaustMap(() => this.refreshWaitingRoom()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private refreshWaitingRoom(): Observable<void> {
    return this.waitingRoomService.fetchWaitingRoom().pipe(
      tap({
        next: (waitingRoom) => {
          this.waitingRoom.set(waitingRoom);
          this.screenState.set('ready');
        },
        error: () => {
          if (this.screenState() !== 'ready') {
            this.screenState.set('failed');
          }
        },
      }),
      catchError(() => EMPTY),
      ignoreElements(),
    );
  }
}
