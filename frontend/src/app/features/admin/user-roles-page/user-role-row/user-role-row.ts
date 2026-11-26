import { Component, input, output } from '@angular/core';

import { UserRole, UserRow } from '../../../../core/models/user-role.model';
import { ROLE_LABELS } from '../role-labels';

@Component({
  selector: 'app-user-role-row',
  templateUrl: './user-role-row.html',
  styleUrl: './user-role-row.scss',
})
export class UserRoleRow {
  readonly user = input.required<UserRow>();
  readonly isBusy = input(false);

  readonly roleChangeRequested = output<UserRole>();

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roles = Object.keys(ROLE_LABELS) as UserRole[];

  protected onRoleSelected(event: Event): void {
    const roleSelect = event.target as HTMLSelectElement;
    const chosenRole = roleSelect.value as UserRole;
    roleSelect.value = this.user().role;
    if (chosenRole !== this.user().role) {
      this.roleChangeRequested.emit(chosenRole);
    }
  }
}
