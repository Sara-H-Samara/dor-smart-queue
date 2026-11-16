import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { CatalogPanel } from './catalog-panel/catalog-panel';
import { SectionOption, SectionSwitch } from './section-switch/section-switch';

@Component({
  selector: 'app-admin-home',
  imports: [CatalogPanel, SectionSwitch],
  templateUrl: './admin-home.html',
  styleUrl: './admin-home.scss',
})
export class AdminHome {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly sectionOptions: SectionOption[] = [
    { value: 'services', label: 'Services' },
    { value: 'counters', label: 'Counters' },
  ];
  protected readonly selectedSection = signal('services');

  protected signOut(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
