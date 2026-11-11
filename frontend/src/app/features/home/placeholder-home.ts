import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-placeholder-home',
  templateUrl: './placeholder-home.html',
})
export class PlaceholderHome {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly title: string = inject(ActivatedRoute).snapshot.data['title'];
  protected readonly fullName = this.auth.fullName;

  protected logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
