import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../../environments/environment';
import { NO_CONNECTION_MESSAGE } from '../../../core/api-failure';
import { UserPage, UserRole, UserRow } from '../../../core/models/user-role.model';
import { AuthService } from '../../../core/services/auth.service';
import { installDialogSupport } from '../../../testing/dialog-support';
import { UserRolesPage } from './user-roles-page';

const USERS_URL = `${environment.apiUrl}/api/admin/users`;

const ann: UserRow = {
  id: 3,
  fullName: 'Ann Lee',
  email: 'lee@example.com',
  role: 'STUDENT',
  currentUser: false,
};
const bob: UserRow = {
  id: 4,
  fullName: 'Bob Stone',
  email: 'bob@example.com',
  role: 'ADMIN',
  currentUser: false,
};
const carl: UserRow = {
  id: 5,
  fullName: 'Carl Diaz',
  email: 'carl@example.com',
  role: 'STAFF',
  currentUser: false,
};
const officeAdmin: UserRow = {
  id: 1,
  fullName: 'Office Admin',
  email: 'admin@dor.com',
  role: 'ADMIN',
  currentUser: true,
};

function pageOf(users: UserRow[], page = 0, totalPages = 1, totalUsers = users.length): UserPage {
  return { users, page, size: 20, totalUsers, totalPages };
}

describe('UserRolesPage', () => {
  let fixture: ComponentFixture<UserRolesPage>;
  let pageElement: HTMLElement;
  let httpTesting: HttpTestingController;

  beforeAll(() => installDialogSupport());

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { logout: vi.fn() } },
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(UserRolesPage);
    pageElement = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
    try {
      httpTesting.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  function expectListRequest(): TestRequest {
    return httpTesting.expectOne(
      (request) => request.url === USERS_URL && request.method === 'GET',
    );
  }

  function expectNoListRequest(): void {
    httpTesting.expectNone((request) => request.url === USERS_URL && request.method === 'GET');
  }

  function expectRoleRequest(user: UserRow): TestRequest {
    return httpTesting.expectOne(`${USERS_URL}/${user.id}/role`);
  }

  function expectNoRoleRequest(): void {
    httpTesting.expectNone((request) => request.url.endsWith('/role'));
  }

  function loadFirstPage(users: UserRow[], totalPages = 1): void {
    expectListRequest().flush(pageOf(users, 0, totalPages));
    fixture.detectChanges();
  }

  function typeSearch(text: string): void {
    const searchInput = pageElement.querySelector<HTMLInputElement>('#user-search')!;
    searchInput.value = text;
    searchInput.dispatchEvent(new Event('input'));
  }

  function waitForSearchDelay(): void {
    vi.advanceTimersByTime(300);
    fixture.detectChanges();
  }

  function roleSelects(): HTMLSelectElement[] {
    return Array.from(pageElement.querySelectorAll<HTMLSelectElement>('app-user-role-row select'));
  }

  function selectOf(user: UserRow): HTMLSelectElement {
    const select = roleSelects().find(
      (roleSelect) => roleSelect.getAttribute('aria-label') === `Change role for ${user.fullName}`,
    );
    return select!;
  }

  function chooseRole(user: UserRow, role: UserRole): void {
    const select = selectOf(user);
    select.value = role;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  function textOf(selector: string): string | undefined {
    return pageElement.querySelector(selector)?.textContent?.trim();
  }

  function chipTexts(): (string | undefined)[] {
    return Array.from(pageElement.querySelectorAll('app-user-role-row .state-chip')).map((chip) =>
      chip.textContent?.trim(),
    );
  }

  function click(selector: string): void {
    pageElement.querySelector<HTMLButtonElement>(selector)!.click();
    fixture.detectChanges();
  }

  function confirmationIsOpen(): boolean {
    return pageElement.querySelector('dialog')!.hasAttribute('open');
  }

  function confirmationButton(label: string): HTMLButtonElement {
    return Array.from(pageElement.querySelectorAll<HTMLButtonElement>('dialog button')).find(
      (button) => button.textContent?.trim() === label,
    )!;
  }

  it('loads the first page once when the page opens', () => {
    const request = expectListRequest();

    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.has('search')).toBe(false);
    request.flush(pageOf([ann]));
    fixture.detectChanges();
    expectNoListRequest();
  });

  it('shows a loading notice while the first page loads', () => {
    expect(textOf('.notice')).toBe('Loading users…');

    loadFirstPage([ann]);

    expect(pageElement.querySelector('.notice')).toBeNull();
  });

  it('shows each user with name, email and role', () => {
    loadFirstPage([ann, carl]);

    const names = Array.from(pageElement.querySelectorAll('.list-row-name')).map((name) =>
      name.textContent?.trim(),
    );
    const emails = Array.from(pageElement.querySelectorAll('.list-row-detail')).map((email) =>
      email.textContent?.trim(),
    );
    expect(names).toEqual(['Ann Lee', 'Carl Diaz']);
    expect(emails).toEqual(['lee@example.com', 'carl@example.com']);
    expect(chipTexts()).toEqual(['Student', 'Staff']);
  });

  it('shows an error with a try again button when loading fails', () => {
    expectListRequest().flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(textOf('.load-error .alert')).toBe('Could not load the users.');
    expect(textOf('.retry-button')).toBe('Try again');
    expect(pageElement.querySelector('app-user-role-row')).toBeNull();
  });

  it('loads the same page again when try again is pressed', () => {
    expectListRequest().flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    click('.retry-button');

    const request = expectListRequest();
    expect(request.request.params.get('page')).toBe('0');
    request.flush(pageOf([ann]));
    fixture.detectChanges();
    expect(pageElement.querySelector('.load-error')).toBeNull();
  });

  it('shows the empty state with the search text when no user matches', () => {
    loadFirstPage([ann]);
    typeSearch('zzz');
    waitForSearchDelay();

    expectListRequest().flush(pageOf([], 0, 0));
    fixture.detectChanges();

    expect(textOf('.empty-state-title')).toBe('No users match "zzz"');
  });

  it('shows the loaded search text in the empty state, not the text still being typed', () => {
    loadFirstPage([ann]);
    typeSearch('zzz');
    waitForSearchDelay();
    expectListRequest().flush(pageOf([], 0, 0));
    fixture.detectChanges();

    typeSearch('zzzy');
    fixture.detectChanges();

    expect(textOf('.empty-state-title')).toBe('No users match "zzz"');
    waitForSearchDelay();
    expectListRequest().flush(pageOf([], 0, 0));
  });

  it('clears the search when clear search is pressed', () => {
    loadFirstPage([ann]);
    typeSearch('zzz');
    waitForSearchDelay();
    expectListRequest().flush(pageOf([], 0, 0));
    fixture.detectChanges();

    click('.clear-search');
    waitForSearchDelay();

    const request = expectListRequest();
    expect(request.request.params.has('search')).toBe(false);
    expect(pageElement.querySelector<HTMLInputElement>('#user-search')?.value).toBe('');
    request.flush(pageOf([ann]));
  });

  it('waits 300 ms after typing before it searches', () => {
    loadFirstPage([ann]);

    typeSearch('ann');
    vi.advanceTimersByTime(299);
    expectNoListRequest();
    vi.advanceTimersByTime(1);

    expect(expectListRequest().request.params.get('search')).toBe('ann');
  });

  it('cancels the older request when a new search starts', () => {
    loadFirstPage([ann, carl]);
    typeSearch('a');
    waitForSearchDelay();
    const olderRequest = expectListRequest();

    typeSearch('carl');
    waitForSearchDelay();
    const newerRequest = expectListRequest();

    expect(olderRequest.cancelled).toBe(true);
    newerRequest.flush(pageOf([carl]));
    fixture.detectChanges();
    expect(chipTexts()).toEqual(['Staff']);
  });

  it('sends no request when the search text did not change', () => {
    loadFirstPage([ann]);

    typeSearch('   ');
    waitForSearchDelay();
    expectNoListRequest();

    typeSearch('ann');
    waitForSearchDelay();
    expectListRequest().flush(pageOf([ann]));
    typeSearch(' ann ');
    waitForSearchDelay();
    expectNoListRequest();
  });

  it('goes back to the first page on a new search', () => {
    loadFirstPage([ann], 2);
    click('.next-page');
    expectListRequest().flush(pageOf([carl], 1, 2));
    fixture.detectChanges();

    typeSearch('ann');
    waitForSearchDelay();

    const request = expectListRequest();
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('search')).toBe('ann');
    request.flush(pageOf([ann]));
  });

  it('loads the next page when next is pressed', () => {
    loadFirstPage([ann], 2);

    click('.next-page');

    const request = expectListRequest();
    expect(request.request.params.get('page')).toBe('1');
    request.flush(pageOf([carl], 1, 2));
    fixture.detectChanges();
    expect(chipTexts()).toEqual(['Staff']);
  });

  it('cancels the older page request when a newer request starts', () => {
    loadFirstPage([ann], 2);
    click('.next-page');
    const pageRequest = expectListRequest();

    typeSearch('carl');
    waitForSearchDelay();
    const searchRequest = expectListRequest();

    expect(pageRequest.cancelled).toBe(true);
    searchRequest.flush(pageOf([carl]));
    fixture.detectChanges();
    expect(textOf('.page-number')).toBe('Page 1 of 1');
  });

  it('shows the page number counted from one', () => {
    loadFirstPage([ann], 3);

    expect(textOf('.page-number')).toBe('Page 1 of 3');
  });

  it('disables previous on the first page and next on the last page', () => {
    loadFirstPage([ann], 2);

    expect(pageElement.querySelector<HTMLButtonElement>('.previous-page')?.disabled).toBe(true);
    expect(pageElement.querySelector<HTMLButtonElement>('.next-page')?.disabled).toBe(false);

    click('.next-page');
    expectListRequest().flush(pageOf([carl], 1, 2));
    fixture.detectChanges();

    expect(pageElement.querySelector<HTMLButtonElement>('.previous-page')?.disabled).toBe(false);
    expect(pageElement.querySelector<HTMLButtonElement>('.next-page')?.disabled).toBe(true);
  });

  it('shows you and no role control in the row of the current user', () => {
    loadFirstPage([officeAdmin, ann]);

    expect(roleSelects().length).toBe(1);
    expect(textOf('.own-row-note')).toContain('You');
  });

  it('changes a role between student and staff without a confirmation', () => {
    loadFirstPage([ann]);

    chooseRole(ann, 'STAFF');

    expect(confirmationIsOpen()).toBe(false);
    const request = expectRoleRequest(ann);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ role: 'STAFF' });
    request.flush({ ...ann, role: 'STAFF' });
  });

  it('asks for a confirmation when the new role is admin', () => {
    loadFirstPage([ann]);

    chooseRole(ann, 'ADMIN');

    expect(confirmationIsOpen()).toBe(true);
    expect(textOf('dialog h3')).toBe('Make Ann Lee an admin?');
    expectNoRoleRequest();

    confirmationButton('Make admin').click();
    fixture.detectChanges();

    const request = expectRoleRequest(ann);
    expect(request.request.body).toEqual({ role: 'ADMIN' });
    request.flush({ ...ann, role: 'ADMIN' });
  });

  it('asks for a confirmation when the old role is admin', () => {
    loadFirstPage([bob]);

    chooseRole(bob, 'STAFF');

    expect(confirmationIsOpen()).toBe(true);
    expect(textOf('dialog h3')).toBe('Remove admin rights from Bob Stone?');
    expectNoRoleRequest();
    confirmationButton('Cancel').click();
    fixture.detectChanges();
  });

  it('sends nothing and keeps the old role when the confirmation is cancelled', () => {
    loadFirstPage([ann]);
    chooseRole(ann, 'ADMIN');

    confirmationButton('Cancel').click();
    fixture.detectChanges();

    expectNoRoleRequest();
    expect(confirmationIsOpen()).toBe(false);
    expect(selectOf(ann).value).toBe('STUDENT');
    expect(selectOf(ann).disabled).toBe(false);
    expect(chipTexts()).toEqual(['Student']);
  });

  it('updates the row from the server answer without loading the list again', () => {
    loadFirstPage([ann, carl]);

    chooseRole(ann, 'STAFF');
    expectRoleRequest(ann).flush({ ...ann, role: 'STAFF' });
    fixture.detectChanges();

    expect(chipTexts()).toEqual(['Staff', 'Staff']);
    expect(selectOf(ann).value).toBe('STAFF');
    expectNoListRequest();
  });

  it('shows a success message after a role change', () => {
    loadFirstPage([ann]);

    chooseRole(ann, 'STAFF');
    expectRoleRequest(ann).flush({ ...ann, role: 'STAFF' });
    fixture.detectChanges();

    expect(textOf('.success-message')).toBe('Ann Lee is now Staff. Ann Lee must sign in again.');
  });

  it('shows the server message and keeps the old role on a conflict', () => {
    loadFirstPage([ann]);

    chooseRole(ann, 'STAFF');
    expectRoleRequest(ann).flush(
      { message: "Cancel or finish this student's ticket first." },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(textOf('.change-error')).toBe("Cancel or finish this student's ticket first.");
    expect(selectOf(ann).value).toBe('STUDENT');
    expect(chipTexts()).toEqual(['Student']);
    expectNoListRequest();
  });

  it('reloads the list once when the user was not found', () => {
    loadFirstPage([ann, carl]);

    chooseRole(ann, 'STAFF');
    expectRoleRequest(ann).flush(
      { message: 'User not found.' },
      { status: 404, statusText: 'Not Found' },
    );
    fixture.detectChanges();

    const reload = expectListRequest();
    expect(reload.request.params.get('page')).toBe('0');
    reload.flush(pageOf([carl]));
    fixture.detectChanges();
    expectNoListRequest();
    expect(textOf('.change-error')).toBe('This user no longer exists. The list is refreshed.');
    expect(chipTexts()).toEqual(['Staff']);
  });

  it('reloads only once after a not found answer, even when the reload fails', () => {
    loadFirstPage([ann]);

    chooseRole(ann, 'STAFF');
    expectRoleRequest(ann).flush(null, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expectListRequest().flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expectNoListRequest();
    expect(textOf('.load-error .alert')).toBe('Could not load the users.');
  });

  it('shows a message when the role is not valid', () => {
    loadFirstPage([ann]);

    chooseRole(ann, 'STAFF');
    expectRoleRequest(ann).flush(null, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(textOf('.change-error')).toBe('This role is not valid. Choose Student, Staff or Admin.');
    expect(selectOf(ann).value).toBe('STUDENT');
  });

  it('shows a message when access is forbidden', () => {
    loadFirstPage([ann]);

    chooseRole(ann, 'STAFF');
    expectRoleRequest(ann).flush(null, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    expect(textOf('.change-error')).toBe(
      'You do not have access to this page. Please sign in again.',
    );
  });

  it('shows the connection message when the server cannot be reached', () => {
    loadFirstPage([ann]);

    chooseRole(ann, 'STAFF');
    expectRoleRequest(ann).error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(textOf('.change-error')).toBe(NO_CONNECTION_MESSAGE);
  });

  it('leaves an expired session to the session handler without an error message', () => {
    loadFirstPage([ann]);

    chooseRole(ann, 'STAFF');
    expectRoleRequest(ann).flush(null, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    expect(pageElement.querySelector('[role="alert"]')).toBeNull();
    expectNoListRequest();
  });

  it('disables every role control while a role change is saved or the list loads', () => {
    loadFirstPage([ann, carl], 2);

    chooseRole(ann, 'STAFF');
    expect(roleSelects().map((select) => select.disabled)).toEqual([true, true]);
    expectRoleRequest(ann).flush({ ...ann, role: 'STAFF' });
    fixture.detectChanges();
    expect(roleSelects().map((select) => select.disabled)).toEqual([false, false]);

    click('.next-page');
    expect(roleSelects().map((select) => select.disabled)).toEqual([true, true]);
    expectListRequest().flush(pageOf([bob], 1, 2));
    fixture.detectChanges();
    expect(roleSelects().map((select) => select.disabled)).toEqual([false]);
  });

  it('blocks a second role change while one is saving', () => {
    loadFirstPage([ann, carl]);

    chooseRole(ann, 'STAFF');
    chooseRole(carl, 'STUDENT');

    expect(httpTesting.match((request) => request.url.endsWith('/role')).length).toBe(1);
  });
});
