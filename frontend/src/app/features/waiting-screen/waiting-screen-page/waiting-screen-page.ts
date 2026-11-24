import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, catchError, ignoreElements, merge, switchMap, tap } from 'rxjs';

import { LiveUpdateService } from '../../../core/live-updates/live-update.service';
import { WaitingRoom } from '../../../core/models/waiting-room.model';
import { WaitingRoomService } from '../../../core/services/waiting-room.service';
import { CounterTile } from './counter-tile/counter-tile';
import { NextTicketList } from './next-ticket-list/next-ticket-list';
import { ORGANIZATION_NAME } from './organization-name';
import { WaitingScreenHeader } from './waiting-screen-header/waiting-screen-header';

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
  private readonly liveUpdateService = inject(LiveUpdateService);

  protected readonly organizationName = ORGANIZATION_NAME;
  protected readonly screenState = signal<WaitingScreenState>('loading');
  protected readonly waitingRoom = signal<WaitingRoom | null>(null);

  constructor() {
    this.loadAfterStreamIsReadyAndRefreshOnLiveUpdates();
  }

  private loadAfterStreamIsReadyAndRefreshOnLiveUpdates(): void {
    const liveUpdateWatch = this.liveUpdateService.watch('public', this.destroyRef);
    merge(liveUpdateWatch.firstLoadRequest, liveUpdateWatch.refreshRequests)
      .pipe(
        switchMap(() => this.refreshWaitingRoom()),
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
