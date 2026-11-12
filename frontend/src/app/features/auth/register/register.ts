import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, Observable, catchError, switchMap, tap } from 'rxjs';

import { HOME_PATH_BY_ROLE } from '../../../core/role-routes';
import { LoginResponse, RegisterRequest, Role } from '../../../core/models/auth.model';
import { AuthService } from '../../../core/services/auth.service';
import { registrationErrorMessage } from './registration-error-message';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrls: ['../auth-ticket.scss'],
})
export class Register {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly registerForm = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected submit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const { fullName, email, password } = this.registerForm.getRawValue();

    this.registerThenSignIn({ fullName, email, password }).subscribe({
      error: (error: HttpErrorResponse) => this.showRegistrationError(error),
    });
  }

  private registerThenSignIn(registration: RegisterRequest): Observable<LoginResponse> {
    const { email, password } = registration;
    return this.authService
      .register(registration)
      .pipe(switchMap(() => this.signInAfterRegistration(email, password)));
  }

  private signInAfterRegistration(email: string, password: string): Observable<LoginResponse> {
    return this.authService.login({ email, password }).pipe(
      catchError(() => {
        this.redirectToLoginAfterRegistration();
        return EMPTY;
      }),
      tap((response) => this.redirectToRoleHome(response.role)),
    );
  }

  private redirectToRoleHome(role: Role): void {
    this.router.navigateByUrl(HOME_PATH_BY_ROLE[role]);
  }

  private redirectToLoginAfterRegistration(): void {
    this.router.navigate(['/login'], { queryParams: { registered: 1 } });
  }

  private showRegistrationError(error: HttpErrorResponse): void {
    this.isSubmitting.set(false);
    this.errorMessage.set(registrationErrorMessage(error));
  }
}
