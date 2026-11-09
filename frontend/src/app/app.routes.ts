import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

const roleHome = () => import('./features/home/role-home').then((m) => m.RoleHome);

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'student',
    canActivate: [authGuard, roleGuard('STUDENT')],
    data: { title: 'Student' },
    loadComponent: roleHome,
  },
  {
    path: 'staff',
    canActivate: [authGuard, roleGuard('STAFF')],
    data: { title: 'Staff desk' },
    loadComponent: roleHome,
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/admin/admin-home/admin-home').then((m) => m.AdminHome),
  },
  { path: '**', redirectTo: 'login' },
];
