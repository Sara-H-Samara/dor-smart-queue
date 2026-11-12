import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

const placeholderHome = () => import('./features/home/placeholder-home').then((m) => m.PlaceholderHome);

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
    loadComponent: () =>
      import('./features/student/student-ticket-page/student-ticket-page').then(
        (m) => m.StudentTicketPage,
      ),
  },
  {
    path: 'staff',
    canActivate: [authGuard, roleGuard('STAFF')],
    data: { title: 'Staff desk' },
    loadComponent: placeholderHome,
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/admin/admin-home/admin-home').then((m) => m.AdminHome),
  },
  { path: '**', redirectTo: 'login' },
];
