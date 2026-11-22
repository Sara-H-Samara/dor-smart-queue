import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class SessionExpiryHandler {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  endSessionAndShowLogin(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
