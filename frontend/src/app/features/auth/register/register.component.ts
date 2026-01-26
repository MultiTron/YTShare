import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <h1 class="auth-title">Create Account</h1>
        <p class="auth-subtitle">Sign up to get started</p>

        @if (errorMessage()) {
          <div class="error-banner" role="alert" aria-live="polite">
            {{ errorMessage() }}
          </div>
        }

        <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="auth-form">
          <div class="form-row">
            <div class="form-group">
              <label for="firstName" class="form-label">First Name</label>
              <input
                id="firstName"
                type="text"
                formControlName="firstName"
                class="form-input"
                [class.input-error]="isFieldInvalid('firstName')"
                autocomplete="given-name"
                aria-describedby="firstName-error"
              />
              @if (isFieldInvalid('firstName')) {
                <span id="firstName-error" class="field-error" role="alert">
                  First name is required
                </span>
              }
            </div>

            <div class="form-group">
              <label for="lastName" class="form-label">Last Name</label>
              <input
                id="lastName"
                type="text"
                formControlName="lastName"
                class="form-input"
                [class.input-error]="isFieldInvalid('lastName')"
                autocomplete="family-name"
                aria-describedby="lastName-error"
              />
              @if (isFieldInvalid('lastName')) {
                <span id="lastName-error" class="field-error" role="alert">
                  Last name is required
                </span>
              }
            </div>
          </div>

          <div class="form-group">
            <label for="email" class="form-label">Email</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              class="form-input"
              [class.input-error]="isFieldInvalid('email')"
              autocomplete="email"
              aria-describedby="email-error"
            />
            @if (isFieldInvalid('email')) {
              <span id="email-error" class="field-error" role="alert">
                Please enter a valid email address
              </span>
            }
          </div>

          <div class="form-group">
            <label for="password" class="form-label">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              class="form-input"
              [class.input-error]="isFieldInvalid('password')"
              autocomplete="new-password"
              aria-describedby="password-error password-hint"
            />
            <span id="password-hint" class="field-hint">
              Minimum 6 characters
            </span>
            @if (isFieldInvalid('password')) {
              <span id="password-error" class="field-error" role="alert">
                Password must be at least 6 characters
              </span>
            }
          </div>

          <div class="form-group">
            <label for="confirmPassword" class="form-label">Confirm Password</label>
            <input
              id="confirmPassword"
              type="password"
              formControlName="confirmPassword"
              class="form-input"
              [class.input-error]="isFieldInvalid('confirmPassword') || hasPasswordMismatch()"
              autocomplete="new-password"
              aria-describedby="confirmPassword-error"
            />
            @if (isFieldInvalid('confirmPassword')) {
              <span id="confirmPassword-error" class="field-error" role="alert">
                Please confirm your password
              </span>
            }
            @if (hasPasswordMismatch()) {
              <span id="confirmPassword-error" class="field-error" role="alert">
                Passwords do not match
              </span>
            }
          </div>

          <button
            type="submit"
            class="btn btn-primary"
            [disabled]="isSubmitting()"
            [attr.aria-busy]="isSubmitting()"
          >
            @if (isSubmitting()) {
              <span class="spinner" aria-hidden="true"></span>
              Creating account...
            } @else {
              Create Account
            }
          </button>
        </form>

        <p class="auth-footer">
          Already have an account?
          <a routerLink="/login">Sign in</a>
        </p>
      </div>
    </div>
  `,
  styles: `
    .auth-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }

    .auth-card {
      width: 100%;
      max-width: 400px;
      background: var(--color-surface);
      border-radius: var(--border-radius);
      box-shadow: var(--shadow-lg);
      padding: 2rem;
    }

    .auth-title {
      margin: 0 0 0.5rem;
      font-size: 1.5rem;
      font-weight: 700;
      text-align: center;
      color: var(--color-text);
    }

    .auth-subtitle {
      margin: 0 0 1.5rem;
      text-align: center;
      color: var(--color-text-muted);
    }

    .error-banner {
      background: var(--color-error-light);
      color: var(--color-error);
      padding: 0.75rem 1rem;
      border-radius: var(--border-radius);
      margin-bottom: 1rem;
      font-size: 0.875rem;
    }

    .auth-form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.375rem;
    }

    .form-label {
      font-size: 0.875rem;
      font-weight: 500;
      color: var(--color-text);
    }

    .form-input {
      padding: 0.75rem 1rem;
      border: 1px solid var(--color-border);
      border-radius: var(--border-radius);
      font-size: 1rem;
      transition: border-color 0.15s, box-shadow 0.15s;

      &:focus {
        outline: none;
        border-color: var(--color-primary);
        box-shadow: 0 0 0 3px var(--color-primary-light);
      }

      &.input-error {
        border-color: var(--color-error);
      }
    }

    .field-hint {
      font-size: 0.75rem;
      color: var(--color-text-muted);
    }

    .field-error {
      font-size: 0.75rem;
      color: var(--color-error);
    }

    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: var(--border-radius);
      font-size: 1rem;
      font-weight: 500;
      cursor: pointer;
      transition: background-color 0.15s;

      &:disabled {
        opacity: 0.7;
        cursor: not-allowed;
      }
    }

    .btn-primary {
      background: var(--color-primary);
      color: white;

      &:hover:not(:disabled) {
        background: var(--color-primary-hover);
      }

      &:focus-visible {
        outline: 2px solid var(--color-primary);
        outline-offset: 2px;
      }
    }

    .spinner {
      width: 1rem;
      height: 1rem;
      border: 2px solid transparent;
      border-top-color: currentColor;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .auth-footer {
      margin: 1.5rem 0 0;
      text-align: center;
      color: var(--color-text-muted);
      font-size: 0.875rem;
    }
  `
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly registerForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(1)]],
    lastName: ['', [Validators.required, Validators.minLength(1)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  }, {
    validators: [this.passwordMatchValidator]
  });

  isFieldInvalid(fieldName: 'firstName' | 'lastName' | 'email' | 'password' | 'confirmPassword'): boolean {
    const control = this.registerForm.controls[fieldName];
    return control.invalid && control.touched;
  }

  hasPasswordMismatch(): boolean {
    return (
      this.registerForm.hasError('passwordMismatch') &&
      this.registerForm.controls.confirmPassword.touched
    );
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { firstName, lastName, email, password } = this.registerForm.getRawValue();

    this.authService.register({ email, password, firstName, lastName }).pipe(
      switchMap((credential) => {
        return this.userService.createUser({
          firebaseUid: credential.user.uid,
          email: credential.user.email!,
          firstName,
          lastName
        });
      })
    ).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(this.getErrorMessage(err.code));
      }
    });
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const confirmPassword = control.get('confirmPassword');

    if (password && confirmPassword && password.value !== confirmPassword.value) {
      return { passwordMismatch: true };
    }
    return null;
  }

  private getErrorMessage(code: string): string {
    switch (code) {
      case 'auth/email-already-in-use':
        return 'An account with this email already exists';
      case 'auth/invalid-email':
        return 'Please enter a valid email address';
      case 'auth/weak-password':
        return 'Password is too weak. Please choose a stronger password.';
      default:
        return 'An error occurred. Please try again.';
    }
  }
}
