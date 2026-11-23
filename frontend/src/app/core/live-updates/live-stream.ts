export type LiveStream = 'public' | 'student' | 'staff' | 'admin';

export const LIVE_STREAM_PATHS: Record<LiveStream, string> = {
  public: '/api/public/queue-stream',
  student: '/api/tickets/me/stream',
  staff: '/api/staff/stream',
  admin: '/api/admin/stream',
};

export function liveStreamNeedsToken(liveStream: LiveStream): boolean {
  return liveStream !== 'public';
}
