import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { LiveUpdateService } from '../../../core/live-updates/live-update.service';
import { AuthService } from '../../../core/services/auth.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import { CatalogPanel } from './catalog-panel/catalog-panel';
import { SectionOption, SectionSwitch } from './section-switch/section-switch';

@Component({
  selector: 'app-admin-home',
  imports: [CatalogPanel, PageHeader, RouterLink, SectionSwitch],
  templateUrl: './admin-home.html',
  styleUrl: './admin-home.scss',
})
export class AdminHome {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly liveUpdateService = inject(LiveUpdateService);

  private readonly liveUpdateWatch = this.liveUpdateService.watch('admin', this.destroyRef);

  protected readonly firstLoadRequest = this.liveUpdateWatch.firstLoadRequest;
  protected readonly refreshRequests = this.liveUpdateWatch.refreshRequests;
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
