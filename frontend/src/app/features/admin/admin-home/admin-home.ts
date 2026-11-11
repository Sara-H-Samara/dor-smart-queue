import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { CatalogPanel } from './catalog-panel/catalog-panel';

@Component({
  selector: 'app-admin-home',
  imports: [CatalogPanel],
  templateUrl: './admin-home.html',
  styleUrl: './admin-home.scss',
})
export class AdminHome {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly fullName = this.auth.fullName;

  protected logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
