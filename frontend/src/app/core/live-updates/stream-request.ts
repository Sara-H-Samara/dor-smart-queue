import { Injectable, inject } from '@angular/core';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { LIVE_STREAM_PATHS, LiveStream, liveStreamNeedsToken } from './live-stream';
import { ServerEvent, ServerEventParser } from './server-event-parser';

const SUCCESS_STATUS = 200;

function ignoreCancelFailure(): void {
  return;
}

export class StreamRejectedError extends Error {
  constructor(readonly status: number) {
    super(`The server rejected the live stream with status ${status}.`);
    this.name = 'StreamRejectedError';
  }
}

@Injectable({ providedIn: 'root' })
export class StreamRequest {
  private readonly authService = inject(AuthService);

  async openStream(
    liveStream: LiveStream,
    abortSignal: AbortSignal,
  ): Promise<AsyncIterable<ServerEvent>> {
    const response = await fetch(`${environment.apiUrl}${LIVE_STREAM_PATHS[liveStream]}`, {
      headers: this.headersFor(liveStream),
      signal: abortSignal,
    });
    if (response.status !== SUCCESS_STATUS) {
      throw new StreamRejectedError(response.status);
    }
    if (response.body === null) {
      throw new Error('The live stream response has no body.');
    }
    return this.readEventsFrom(response.body);
  }

  private headersFor(liveStream: LiveStream): Record<string, string> {
    const headers: Record<string, string> = { Accept: 'text/event-stream' };
    const token = this.authService.getToken();
    if (liveStreamNeedsToken(liveStream) && token !== null) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  }

  private async *readEventsFrom(body: ReadableStream<Uint8Array>): AsyncGenerator<ServerEvent> {
    const reader = body.getReader();
    const textDecoder = new TextDecoder();
    const parser = new ServerEventParser();
    try {
      let chunk = await reader.read();
      while (!chunk.done) {
        yield* parser.readEventsFrom(textDecoder.decode(chunk.value, { stream: true }));
        chunk = await reader.read();
      }
    } finally {
      reader.cancel().catch(ignoreCancelFailure);
    }
  }
}
