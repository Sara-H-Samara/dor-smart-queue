import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../models/auth.model';
import { ROLE_HOME } from '../role-routes';
import { AuthService } from '../services/auth.service';

export const roleGuard =
  (role: Role): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    const current = auth.role();
    if (!current) {
      return router.createUrlTree(['/login']);
    }
    return current === role ? true : router.createUrlTree([ROLE_HOME[current]]);
  };
