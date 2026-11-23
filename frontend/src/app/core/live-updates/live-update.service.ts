import {
  DestroyRef,
  Injectable,
  InjectionToken,
  Injector,
  effect,
  inject,
  untracked,
} from '@angular/core';

import { LiveStream } from './live-stream';
import { LiveUpdateWatch } from './live-update-watch';
import { PageVisibility } from './page-visibility';
import { SessionExpiryHandler } from './session-expiry-handler';
import { StreamLiveUpdateWatch } from './stream-live-update-watch';
import { StreamRequest } from './stream-request';

export const RANDOM_FRACTION = new InjectionToken<() => number>('RANDOM_FRACTION', {
  providedIn: 'root',
  factory: () => Math.random,
});

@Injectable({ providedIn: 'root' })
export class LiveUpdateService {
  private readonly streamRequest = inject(StreamRequest);
  private readonly sessionExpiryHandler = inject(SessionExpiryHandler);
  private readonly pageVisibility = inject(PageVisibility);
  private readonly injector = inject(Injector);
  private readonly randomFraction = inject(RANDOM_FRACTION);

  watch(liveStream: LiveStream, destroyRef: DestroyRef): LiveUpdateWatch {
    const watch = new StreamLiveUpdateWatch(
      liveStream,
      this.streamRequest,
      this.sessionExpiryHandler,
      this.pageVisibility.isVisible(),
      this.randomFraction,
    );
    const visibilityEffect = effect(
      () => {
        const isPageVisible = this.pageVisibility.isVisible();
        untracked(() => watch.pageVisibilityChanged(isPageVisible));
      },
      { injector: this.injector, manualCleanup: true },
    );
    destroyRef.onDestroy(() => {
      visibilityEffect.destroy();
      watch.stop();
    });
    watch.start();
    return watch;
  }
}
