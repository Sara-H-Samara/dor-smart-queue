import { ServerEventParser } from './server-event-parser';

describe('ServerEventParser', () => {
  let parser: ServerEventParser;

  beforeEach(() => {
    parser = new ServerEventParser();
  });

  it('reads one event with a name and data', () => {
    expect(parser.readEventsFrom('event:ticket-changed\ndata:{}\n\n')).toEqual([
      { name: 'ticket-changed', data: '{}' },
    ]);
  });

  it('reads several events that arrive in one chunk', () => {
    const events = parser.readEventsFrom(
      'event:ready\ndata:{}\n\nevent:configuration-changed\ndata:{}\n\n',
    );

    expect(events.map((event) => event.name)).toEqual(['ready', 'configuration-changed']);
  });

  it('reads an event that arrives split across two chunks', () => {
    expect(parser.readEventsFrom('event:ticket-ch')).toEqual([]);
    expect(parser.readEventsFrom('anged\ndata:{}\n')).toEqual([]);

    expect(parser.readEventsFrom('\n')).toEqual([{ name: 'ticket-changed', data: '{}' }]);
  });

  it('ignores heartbeat comment lines', () => {
    expect(parser.readEventsFrom(':heartbeat\n\n')).toEqual([]);
  });

  it('reads an event that comes after a heartbeat comment line', () => {
    const events = parser.readEventsFrom(':heartbeat\n\nevent:ready\ndata:{}\n\n');

    expect(events).toEqual([{ name: 'ready', data: '{}' }]);
  });

  it('accepts a space after the colon', () => {
    expect(parser.readEventsFrom('event: ready\ndata: {}\n\n')).toEqual([
      { name: 'ready', data: '{}' },
    ]);
  });

  it('accepts carriage return and line feed line endings', () => {
    expect(parser.readEventsFrom('event:ready\r\ndata:{}\r\n\r\n')).toEqual([
      { name: 'ready', data: '{}' },
    ]);
  });

  it('keeps a carriage return and line feed pair together when a chunk ends between them', () => {
    expect(parser.readEventsFrom('event:ready\r')).toEqual([]);

    expect(parser.readEventsFrom('\ndata:{}\r\n\r\n')).toEqual([{ name: 'ready', data: '{}' }]);
  });

  it('joins several data lines with a line feed', () => {
    expect(parser.readEventsFrom('event:note\ndata:first\ndata:second\n\n')).toEqual([
      { name: 'note', data: 'first\nsecond' },
    ]);
  });

  it('uses the name message when an event has no name', () => {
    expect(parser.readEventsFrom('data:{}\n\n')).toEqual([{ name: 'message', data: '{}' }]);
  });

  it('does not carry the name of one event into the next event', () => {
    const events = parser.readEventsFrom('event:ready\ndata:{}\n\ndata:{}\n\n');

    expect(events.map((event) => event.name)).toEqual(['ready', 'message']);
  });

  it('skips an event that has no data line', () => {
    expect(parser.readEventsFrom('event:ready\n\n')).toEqual([]);
  });

  it('ignores fields it does not use', () => {
    expect(parser.readEventsFrom('id:7\nretry:1000\nevent:ready\ndata:{}\n\n')).toEqual([
      { name: 'ready', data: '{}' },
    ]);
  });
});
