import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../models/auth.model';
import { HOME_PATH_BY_ROLE } from '../role-routes';
import { AuthService } from '../services/auth.service';

export const roleGuard =
  (requiredRole: Role): CanActivateFn =>
  () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const currentRole = authService.role();
    if (!currentRole) {
      return router.createUrlTree(['/login']);
    }
    return currentRole === requiredRole ? true : router.createUrlTree([HOME_PATH_BY_ROLE[currentRole]]);
  };
