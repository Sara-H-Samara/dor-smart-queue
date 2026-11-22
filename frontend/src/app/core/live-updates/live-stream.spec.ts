import { LIVE_STREAM_PATHS, liveStreamNeedsToken } from './live-stream';

describe('LIVE_STREAM_PATHS', () => {
  it('uses the server path of each stream', () => {
    expect(LIVE_STREAM_PATHS).toEqual({
      public: '/api/public/queue-stream',
      student: '/api/tickets/me/stream',
      staff: '/api/staff/stream',
      admin: '/api/admin/stream',
    });
  });
});

describe('liveStreamNeedsToken', () => {
  it('says the public stream needs no token', () => {
    expect(liveStreamNeedsToken('public')).toBe(false);
  });

  it('says the student, staff and admin streams need a token', () => {
    expect(liveStreamNeedsToken('student')).toBe(true);
    expect(liveStreamNeedsToken('staff')).toBe(true);
    expect(liveStreamNeedsToken('admin')).toBe(true);
  });
});
