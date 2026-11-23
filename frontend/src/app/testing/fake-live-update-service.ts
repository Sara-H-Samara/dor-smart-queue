import { LiveStream } from '../core/live-updates/live-stream';
import { LiveUpdateWatch } from '../core/live-updates/live-update-watch';
import { FakeLiveUpdateWatch } from './fake-live-update-watch';

export class FakeLiveUpdateService {
  readonly watchedLiveStreams: LiveStream[] = [];
  readonly liveUpdateWatch = new FakeLiveUpdateWatch();

  watch(liveStream: LiveStream): LiveUpdateWatch {
    this.watchedLiveStreams.push(liveStream);
    return this.liveUpdateWatch;
  }
}
