import { Role } from './auth.model';

export type UserRole = Role;

export interface UserRow {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  currentUser: boolean;
}

export interface UserPage {
  users: UserRow[];
  page: number;
  size: number;
  totalUsers: number;
  totalPages: number;
}
