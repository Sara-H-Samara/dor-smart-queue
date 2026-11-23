import { signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';

import { LiveUpdateWatch } from '../core/live-updates/live-update-watch';

export class FakeLiveUpdateWatch implements LiveUpdateWatch {
  private readonly refreshSubject = new Subject<void>();
  private readonly connected = signal(false);

  readonly refreshRequests: Observable<void> = this.refreshSubject.asObservable();
  readonly isConnected = this.connected.asReadonly();
  stopCallCount = 0;

  requestRefresh(): void {
    this.refreshSubject.next();
  }

  setConnected(isConnected: boolean): void {
    this.connected.set(isConnected);
  }

  stop(): void {
    this.stopCallCount++;
  }
}
