import { Component, ElementRef, computed, effect, input, output, viewChild } from '@angular/core';

import { UserRole } from '../../../../core/models/user-role.model';
import { ROLE_LABELS } from '../role-labels';

export interface PendingRoleChange {
  fullName: string;
  oldRole: UserRole;
  newRole: UserRole;
}

@Component({
  selector: 'app-role-change-confirmation',
  templateUrl: './role-change-confirmation.html',
  styleUrl: './role-change-confirmation.scss',
})
export class RoleChangeConfirmation {
  readonly pendingChange = input<PendingRoleChange | null>(null);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  private readonly confirmationElement =
    viewChild.required<ElementRef<HTMLDialogElement>>('confirmation');
  private readonly cancelButton = viewChild.required<ElementRef<HTMLButtonElement>>('cancelButton');
  private confirmPressed = false;

  protected readonly makesAdmin = computed(() => this.pendingChange()?.newRole === 'ADMIN');

  protected readonly heading = computed(() => {
    const change = this.pendingChange();
    if (!change) {
      return '';
    }
    return this.makesAdmin()
      ? `Make ${change.fullName} an admin?`
      : `Remove admin rights from ${change.fullName}?`;
  });

  protected readonly explanation = computed(() => {
    const change = this.pendingChange();
    if (!change) {
      return '';
    }
    return this.makesAdmin()
      ? 'Admins can change services, counters and the roles of all users, including other admins.'
      : `${change.fullName} will be ${ROLE_LABELS[change.newRole]} and can no longer use the admin pages.`;
  });

  protected readonly signInNote = computed(() => {
    const change = this.pendingChange();
    if (!change) {
      return '';
    }
    return this.makesAdmin()
      ? `${change.fullName} must sign in again to get admin rights.`
      : `${change.fullName} must sign in again.`;
  });

  protected readonly confirmLabel = computed(() =>
    this.makesAdmin() ? 'Make admin' : 'Remove admin rights',
  );

  constructor() {
    effect(() => {
      const confirmation = this.confirmationElement().nativeElement;
      const hasPendingChange = this.pendingChange() !== null;
      if (hasPendingChange && !confirmation.open) {
        confirmation.showModal();
        this.cancelButton().nativeElement.focus();
      }
    });
  }

  protected confirm(): void {
    this.confirmPressed = true;
    this.confirmationElement().nativeElement.close();
  }

  protected cancel(): void {
    this.confirmationElement().nativeElement.close();
  }

  protected onEscapePressed(event: Event): void {
    event.preventDefault();
    this.cancel();
  }

  protected onConfirmationClicked(event: MouseEvent): void {
    const confirmation = this.confirmationElement().nativeElement;
    if (event.target !== confirmation) {
      return;
    }
    const box = confirmation.getBoundingClientRect();
    const clickedOutside =
      event.clientX < box.left ||
      event.clientX > box.right ||
      event.clientY < box.top ||
      event.clientY > box.bottom;
    if (clickedOutside) {
      this.cancel();
    }
  }

  protected onConfirmationClosed(): void {
    const wasConfirmed = this.confirmPressed;
    this.confirmPressed = false;
    if (wasConfirmed) {
      this.confirmed.emit();
    } else {
      this.cancelled.emit();
    }
  }
}
