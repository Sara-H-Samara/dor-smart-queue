import { Role } from './models/auth.model';

export const ROLE_HOME: Record<Role, string> = {
  STUDENT: '/student',
  STAFF: '/staff',
  ADMIN: '/admin',
};
