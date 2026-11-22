import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { loggedOutOnlyGuard } from './core/guards/logged-out-only.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    canActivate: [loggedOutOnlyGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    canActivate: [loggedOutOnlyGuard],
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
    loadComponent: () =>
      import('./features/staff/staff-desk-page/staff-desk-page').then((m) => m.StaffDeskPage),
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadComponent: () => import('./features/admin/admin-home/admin-home').then((m) => m.AdminHome),
  },
  {
    path: 'waiting-room',
    loadComponent: () =>
      import('./features/waiting-screen/waiting-screen-page/waiting-screen-page').then(
        (m) => m.WaitingScreenPage,
      ),
  },
  { path: '**', redirectTo: 'login' },
];
