import { ROLE_LABELS, needsConfirmation } from './role-labels';

describe('needsConfirmation', () => {
  it('needs a confirmation when the new role is admin', () => {
    expect(needsConfirmation('STUDENT', 'ADMIN')).toBe(true);
    expect(needsConfirmation('STAFF', 'ADMIN')).toBe(true);
  });

  it('needs a confirmation when the old role is admin', () => {
    expect(needsConfirmation('ADMIN', 'STAFF')).toBe(true);
    expect(needsConfirmation('ADMIN', 'STUDENT')).toBe(true);
  });

  it('needs no confirmation between student and staff', () => {
    expect(needsConfirmation('STUDENT', 'STAFF')).toBe(false);
    expect(needsConfirmation('STAFF', 'STUDENT')).toBe(false);
  });
});

describe('ROLE_LABELS', () => {
  it('names each role in plain English', () => {
    expect(ROLE_LABELS).toEqual({ STUDENT: 'Student', STAFF: 'Staff', ADMIN: 'Admin' });
  });
});
