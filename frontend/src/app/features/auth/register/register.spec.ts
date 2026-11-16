import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { Register } from './register';

describe('Register', () => {
  it('render_onRegisterScreen_showsHeroTitleWithoutRepeatedSignInPrompt', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { register: vi.fn(), login: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(Register);

    fixture.detectChanges();

    const screenElement = fixture.nativeElement as HTMLElement;
    expect(screenElement.querySelector('.hero-title')?.textContent).toContain('A digital queue');
    expect(screenElement.textContent).not.toContain('Sign in or create an account to continue');
    expect(screenElement.querySelector('h1')?.textContent?.trim()).toBe('Create an account');
  });
});
