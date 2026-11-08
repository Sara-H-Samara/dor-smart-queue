export type Role = 'STUDENT' | 'STAFF' | 'ADMIN';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  fullName: string;
  role: Role;
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: Role;
}
