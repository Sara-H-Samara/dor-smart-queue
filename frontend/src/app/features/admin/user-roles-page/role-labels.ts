import { UserRole } from '../../../core/models/user-role.model';

export const ROLE_LABELS: Readonly<Record<UserRole, string>> = {
  STUDENT: 'Student',
  STAFF: 'Staff',
  ADMIN: 'Admin',
};

export function needsConfirmation(oldRole: UserRole, newRole: UserRole): boolean {
  return oldRole === 'ADMIN' || newRole === 'ADMIN';
}
