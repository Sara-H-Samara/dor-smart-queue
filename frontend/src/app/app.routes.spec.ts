import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { routes } from './app.routes';
import { authGuard } from './core/guards/auth.guard';
import { Role } from './core/models/auth.model';
import { AuthService } from './core/services/auth.service';

describe('routes', () => {
  const usersRouteIndex = routes.findIndex((route) => route.path === 'admin/users');
  const usersRoute = routes[usersRouteIndex];

  function runRoleGuardAs(role: Role): ReturnType<CanActivateFn> {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { role: () => role } }],
    });
    const roleGuard = usersRoute.canActivate?.[1] as CanActivateFn;
    return TestBed.runInInjectionContext(() =>
      roleGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  }

  it('protects the users page with the login guard first', () => {
    expect(usersRoute.canActivate?.length).toBe(2);
    expect(usersRoute.canActivate?.[0]).toBe(authGuard);
  });

  it('lets an admin open the users page', () => {
    expect(runRoleGuardAs('ADMIN')).toBe(true);
  });

  it('sends staff to their own home instead of the users page', () => {
    const result = runRoleGuardAs('STAFF');

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/staff');
  });

  it('places the users page before the fallback route', () => {
    const fallbackIndex = routes.findIndex((route) => route.path === '**');

    expect(usersRouteIndex).toBeGreaterThan(-1);
    expect(usersRouteIndex).toBeLessThan(fallbackIndex);
  });
});
