import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { Role } from '../models/auth.model';
import { AuthService } from '../services/auth.service';
import { loggedOutOnlyGuard } from './logged-out-only.guard';

describe('loggedOutOnlyGuard', () => {
  function runGuardWithRole(role: Role | null): boolean | UrlTree {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { role: () => role } }],
    });
    return TestBed.runInInjectionContext(() =>
      loggedOutOnlyGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as boolean | UrlTree;
  }

  function pathOf(result: boolean | UrlTree): string {
    return TestBed.inject(Router).serializeUrl(result as UrlTree);
  }

  it('A logged-out user can open the login and register pages', () => {
    expect(runGuardWithRole(null)).toBe(true);
  });

  it('A logged-in student is sent to the student home page', () => {
    expect(pathOf(runGuardWithRole('STUDENT'))).toBe('/student');
  });

  it('A logged-in staff member is sent to the staff home page', () => {
    expect(pathOf(runGuardWithRole('STAFF'))).toBe('/staff');
  });

  it('A logged-in admin is sent to the admin home page', () => {
    expect(pathOf(runGuardWithRole('ADMIN'))).toBe('/admin');
  });
});
