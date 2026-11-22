import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { SessionExpiryHandler } from './session-expiry-handler';

describe('SessionExpiryHandler', () => {
  let logout: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;
  let sessionExpiryHandler: SessionExpiryHandler;

  beforeEach(() => {
    logout = vi.fn();
    navigate = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { logout } },
        { provide: Router, useValue: { navigate } },
      ],
    });
    sessionExpiryHandler = TestBed.inject(SessionExpiryHandler);
  });

  it('logs the user out when the session ends', () => {
    sessionExpiryHandler.endSessionAndShowLogin();

    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('goes to the login page when the session ends', () => {
    sessionExpiryHandler.endSessionAndShowLogin();

    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('logs out before it goes to the login page', () => {
    sessionExpiryHandler.endSessionAndShowLogin();

    expect(logout.mock.invocationCallOrder[0]).toBeLessThan(navigate.mock.invocationCallOrder[0]);
  });
});
