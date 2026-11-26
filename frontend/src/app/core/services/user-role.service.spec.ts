import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { UserRoleService } from './user-role.service';

const USERS_URL = `${environment.apiUrl}/api/admin/users`;

describe('UserRoleService', () => {
  let userRoleService: UserRoleService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    userRoleService = TestBed.inject(UserRoleService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('sends a GET request with the search, page and size when listing users', () => {
    userRoleService.listUsers('ann', 2, 20).subscribe();

    const request = httpTesting.expectOne((sent) => sent.url === USERS_URL);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('ann');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');
  });

  it('sends the search without the spaces around it', () => {
    userRoleService.listUsers('  ann  ', 0, 20).subscribe();

    const request = httpTesting.expectOne((sent) => sent.url === USERS_URL);
    expect(request.request.params.get('search')).toBe('ann');
  });

  it('sends no search parameter when the search is empty', () => {
    userRoleService.listUsers('', 0, 20).subscribe();

    const request = httpTesting.expectOne((sent) => sent.url === USERS_URL);
    expect(request.request.params.has('search')).toBe(false);
    expect(request.request.params.get('page')).toBe('0');
  });

  it('sends no search parameter when the search is only spaces', () => {
    userRoleService.listUsers('   ', 0, 20).subscribe();

    const request = httpTesting.expectOne((sent) => sent.url === USERS_URL);
    expect(request.request.params.has('search')).toBe(false);
  });

  it('returns the page from the server when listing users', () => {
    const page = {
      users: [
        {
          id: 3,
          fullName: 'Ann Lee',
          email: 'ann@example.com',
          role: 'STUDENT',
          currentUser: false,
        },
      ],
      page: 0,
      size: 20,
      totalUsers: 1,
      totalPages: 1,
    };
    let receivedPage: unknown;
    userRoleService.listUsers('', 0, 20).subscribe((userPage) => (receivedPage = userPage));

    httpTesting.expectOne((sent) => sent.url === USERS_URL).flush(page);

    expect(receivedPage).toEqual(page);
  });

  it('sends a PATCH request with the new role when changing a role', () => {
    userRoleService.changeRole(7, 'STAFF').subscribe();

    const request = httpTesting.expectOne(`${USERS_URL}/7/role`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ role: 'STAFF' });
  });

  it('passes the server error on when changing a role fails', () => {
    let receivedError: unknown;
    userRoleService
      .changeRole(7, 'STAFF')
      .subscribe({ error: (error: unknown) => (receivedError = error) });

    httpTesting
      .expectOne(`${USERS_URL}/7/role`)
      .flush(
        { message: "Cancel or finish this student's ticket first." },
        { status: 409, statusText: 'Conflict' },
      );

    expect(receivedError).toBeInstanceOf(HttpErrorResponse);
    expect((receivedError as HttpErrorResponse).status).toBe(409);
    expect((receivedError as HttpErrorResponse).error).toEqual({
      message: "Cancel or finish this student's ticket first.",
    });
  });
});
