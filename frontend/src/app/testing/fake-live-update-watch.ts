import { signal } from '@angular/core';
import { Observable, Subject } from 'rxjs';

import { LiveUpdateWatch } from '../core/live-updates/live-update-watch';

export class FakeLiveUpdateWatch implements LiveUpdateWatch {
  private readonly refreshSubject = new Subject<void>();

  readonly refreshRequests: Observable<void> = this.refreshSubject.asObservable();
  readonly isConnected = signal(false).asReadonly();

  requestRefresh(): void {
    this.refreshSubject.next();
  }

  stop(): void {
    return;
  }
}
