import { Role } from './models/auth.model';

export const HOME_PATH_BY_ROLE: Record<Role, string> = {
  STUDENT: '/student',
  STAFF: '/staff',
  ADMIN: '/admin',
};
