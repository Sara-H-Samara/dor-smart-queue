import { Signal } from '@angular/core';
import { Observable } from 'rxjs';

export interface LiveUpdateWatch {
  readonly firstLoadRequest: Observable<void>;
  readonly refreshRequests: Observable<void>;
  readonly isConnected: Signal<boolean>;
  stop(): void;
}
