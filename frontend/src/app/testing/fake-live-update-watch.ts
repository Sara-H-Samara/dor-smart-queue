import { signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';

import { LiveUpdateWatch } from '../core/live-updates/live-update-watch';

export class FakeLiveUpdateWatch implements LiveUpdateWatch {
  private readonly refreshSubject = new Subject<void>();
  private readonly firstLoadSubject = new Subject<void>();

  readonly firstLoadRequest: Observable<void> = this.firstLoadSubject.asObservable();
  readonly refreshRequests: Observable<void> = this.refreshSubject.asObservable();
  readonly isConnected = signal(false).asReadonly();

  requestFirstLoad(): void {
    this.firstLoadSubject.next();
  }

  requestRefresh(): void {
    this.refreshSubject.next();
  }

  stop(): void {
    return;
  }
}
