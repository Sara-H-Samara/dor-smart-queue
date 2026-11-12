import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-placeholder-home',
  templateUrl: './placeholder-home.html',
})
export class PlaceholderHome {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly routeTitle: string = inject(ActivatedRoute).snapshot.data['title'];
  protected readonly fullName = this.authService.fullName;

  protected signOut(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
