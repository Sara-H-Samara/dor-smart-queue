import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { HOME_PATH_BY_ROLE } from '../role-routes';
import { AuthService } from '../services/auth.service';

export const loggedOutOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const currentRole = authService.role();
  return currentRole ? router.createUrlTree([HOME_PATH_BY_ROLE[currentRole]]) : true;
};
