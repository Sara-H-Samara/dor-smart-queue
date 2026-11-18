import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { Login } from './login';

describe('Login', () => {
  it('shows the hero title and the Sign in heading, with no repeated sign-in prompt', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: { login: vi.fn() } }],
    });
    const fixture = TestBed.createComponent(Login);

    fixture.detectChanges();

    const screenElement = fixture.nativeElement as HTMLElement;
    expect(screenElement.querySelector('.hero-title')?.textContent).toContain('A digital queue');
    expect(screenElement.textContent).not.toContain('Sign in or create an account to continue');
    expect(screenElement.querySelector('h1')?.textContent?.trim()).toBe('Sign in');
  });
});
