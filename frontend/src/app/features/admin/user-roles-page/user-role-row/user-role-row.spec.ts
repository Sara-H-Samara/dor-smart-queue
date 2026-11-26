import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserRole, UserRow } from '../../../../core/models/user-role.model';
import { UserRoleRow } from './user-role-row';

const student: UserRow = {
  id: 3,
  fullName: 'Ann Lee',
  email: 'lee@example.com',
  role: 'STUDENT',
  currentUser: false,
};

describe('UserRoleRow', () => {
  let fixture: ComponentFixture<UserRoleRow>;
  let rowElement: HTMLElement;
  let requestedRoles: UserRole[];

  beforeEach(() => {
    fixture = TestBed.createComponent(UserRoleRow);
    fixture.componentRef.setInput('user', student);
    rowElement = fixture.nativeElement as HTMLElement;
    requestedRoles = [];
    fixture.componentInstance.roleChangeRequested.subscribe((role) => requestedRoles.push(role));
  });

  function roleSelect(): HTMLSelectElement | null {
    return rowElement.querySelector<HTMLSelectElement>('select');
  }

  function chooseRole(role: UserRole): void {
    const select = roleSelect()!;
    select.value = role;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  it('shows the name, the email and the role as a chip', () => {
    fixture.detectChanges();

    expect(rowElement.querySelector('.list-row-name')?.textContent?.trim()).toBe('Ann Lee');
    expect(rowElement.querySelector('.list-row-detail')?.textContent?.trim()).toBe(
      'lee@example.com',
    );
    expect(rowElement.querySelector('.state-chip')?.textContent?.trim()).toBe('Student');
  });

  it('labels the role control with the name of the user', () => {
    fixture.detectChanges();

    expect(roleSelect()?.getAttribute('aria-label')).toBe('Change role for Ann Lee');
    expect(roleSelect()?.value).toBe('STUDENT');
  });

  it('emits the chosen role when a different role is selected', () => {
    fixture.detectChanges();

    chooseRole('STAFF');

    expect(requestedRoles).toEqual(['STAFF']);
  });

  it('shows the old role in the select again after a different role is selected', () => {
    fixture.detectChanges();

    chooseRole('ADMIN');

    expect(roleSelect()?.value).toBe('STUDENT');
  });

  it('shows the new role in the select when the row gets the updated user', () => {
    fixture.detectChanges();
    chooseRole('STAFF');

    fixture.componentRef.setInput('user', { ...student, role: 'STAFF' });
    fixture.detectChanges();

    expect(roleSelect()?.value).toBe('STAFF');
    expect(rowElement.querySelector('.state-chip')?.textContent?.trim()).toBe('Staff');
  });

  it('emits nothing when the same role is selected', () => {
    fixture.detectChanges();

    chooseRole('STUDENT');

    expect(requestedRoles).toEqual([]);
  });

  it('shows you instead of a control for the current user', () => {
    fixture.componentRef.setInput('user', { ...student, currentUser: true });

    fixture.detectChanges();

    expect(roleSelect()).toBeNull();
    expect(rowElement.querySelector('.own-row-note')?.textContent).toContain('You');
    expect(rowElement.querySelector('.own-row-note')?.textContent).toContain(
      'You cannot change your own role.',
    );
  });

  it('disables the control while busy', () => {
    fixture.componentRef.setInput('isBusy', true);

    fixture.detectChanges();

    expect(roleSelect()?.disabled).toBe(true);
  });
});
