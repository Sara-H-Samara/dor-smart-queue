import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';
import { ServerEvent } from './server-event-parser';
import { StreamRejectedError, StreamRequest } from './stream-request';

function responseWithChunks(chunks: string[], status = 200): Response {
  const textEncoder = new TextEncoder();
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(textEncoder.encode(chunk)));
      controller.close();
    },
  });
  return new Response(body, { status });
}

async function collectEvents(events: AsyncIterable<ServerEvent>): Promise<ServerEvent[]> {
  const collectedEvents: ServerEvent[] = [];
  for await (const event of events) {
    collectedEvents.push(event);
  }
  return collectedEvents;
}

describe('StreamRequest', () => {
  let fetchStub: ReturnType<typeof vi.fn>;
  let streamRequest: StreamRequest;
  let abortController: AbortController;

  beforeEach(() => {
    fetchStub = vi.fn();
    vi.stubGlobal('fetch', fetchStub);
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { getToken: () => 'student-token' } }],
    });
    streamRequest = TestBed.inject(StreamRequest);
    abortController = new AbortController();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('asks for an event stream and sends the token on a protected stream', async () => {
    fetchStub.mockResolvedValue(responseWithChunks([]));

    await streamRequest.openStream('student', abortController.signal);

    expect(fetchStub).toHaveBeenCalledWith(`${environment.apiUrl}/api/tickets/me/stream`, {
      headers: { Accept: 'text/event-stream', Authorization: 'Bearer student-token' },
      signal: abortController.signal,
    });
  });

  it('does not send the token on the public stream', async () => {
    fetchStub.mockResolvedValue(responseWithChunks([]));

    await streamRequest.openStream('public', abortController.signal);

    expect(fetchStub).toHaveBeenCalledWith(`${environment.apiUrl}/api/public/queue-stream`, {
      headers: { Accept: 'text/event-stream' },
      signal: abortController.signal,
    });
  });

  it('opens the staff and admin streams on their own paths', async () => {
    fetchStub.mockImplementation(() => Promise.resolve(responseWithChunks([])));

    await streamRequest.openStream('staff', abortController.signal);
    await streamRequest.openStream('admin', abortController.signal);

    expect(fetchStub.mock.calls.map((call) => call[0])).toEqual([
      `${environment.apiUrl}/api/staff/stream`,
      `${environment.apiUrl}/api/admin/stream`,
    ]);
  });

  it('throws a rejection with status 401 when the token is not accepted', async () => {
    fetchStub.mockResolvedValue(responseWithChunks([], 401));

    const opening = streamRequest.openStream('student', abortController.signal);

    await expect(opening).rejects.toBeInstanceOf(StreamRejectedError);
    await expect(opening).rejects.toMatchObject({ status: 401 });
  });

  it('throws a rejection with status 403 when the role is wrong', async () => {
    fetchStub.mockResolvedValue(responseWithChunks([], 403));

    await expect(streamRequest.openStream('staff', abortController.signal)).rejects.toMatchObject({
      status: 403,
    });
  });

  it('throws a rejection with status 503 when the server is unavailable', async () => {
    fetchStub.mockResolvedValue(responseWithChunks([], 503));

    await expect(streamRequest.openStream('public', abortController.signal)).rejects.toMatchObject({
      status: 503,
    });
  });

  it('passes on a network failure of the request', async () => {
    fetchStub.mockRejectedValue(new TypeError('Failed to fetch'));

    await expect(streamRequest.openStream('public', abortController.signal)).rejects.toThrow(
      'Failed to fetch',
    );
  });

  it('reads the events of the response body', async () => {
    fetchStub.mockResolvedValue(
      responseWithChunks(['event:ready\ndata:{}\n\n', 'event:ticket-changed\ndata:{}\n\n']),
    );

    const events = await streamRequest.openStream('public', abortController.signal);

    expect(await collectEvents(events)).toEqual([
      { name: 'ready', data: '{}' },
      { name: 'ticket-changed', data: '{}' },
    ]);
  });

  it('reads an event that arrives split across two chunks of the body', async () => {
    fetchStub.mockResolvedValue(responseWithChunks(['event:ready\nda', 'ta:{}\n\n']));

    const events = await streamRequest.openStream('public', abortController.signal);

    expect(await collectEvents(events)).toEqual([{ name: 'ready', data: '{}' }]);
  });

  it('finishes reading when the server closes the stream', async () => {
    fetchStub.mockResolvedValue(responseWithChunks(['event:ready\ndata:{}\n\n']));

    const events = await streamRequest.openStream('public', abortController.signal);

    expect(await collectEvents(events)).toHaveLength(1);
  });

  it('keeps reading when a heartbeat comment arrives between events', async () => {
    fetchStub.mockResolvedValue(
      responseWithChunks([
        'event:ready\ndata:{}\n\n',
        ':heartbeat\n\n',
        'event:ticket-changed\ndata:{}\n\n',
      ]),
    );

    const events = await streamRequest.openStream('public', abortController.signal);

    expect((await collectEvents(events)).map((event) => event.name)).toEqual([
      'ready',
      'ticket-changed',
    ]);
  });
});
