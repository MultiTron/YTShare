import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <h1 class="auth-title">Welcome Back</h1>
        <p class="auth-subtitle">Sign in to your account</p>

        @if (errorMessage()) {
          <div class="error-banner" role="alert" aria-live="polite">
            {{ errorMessage() }}
          </div>
        }

        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="auth-form">
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
              autocomplete="current-password"
              aria-describedby="password-error"
            />
            @if (isFieldInvalid('password')) {
              <span id="password-error" class="field-error" role="alert">
                Password is required
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
              Signing in...
            } @else {
              Sign In
            }
          </button>
        </form>

        <p class="auth-footer">
          Don't have an account?
          <a routerLink="/register">Create one</a>
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
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  isFieldInvalid(fieldName: 'email' | 'password'): boolean {
    const control = this.loginForm.controls[fieldName];
    return control.invalid && control.touched;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.loginForm.getRawValue();

    this.authService.login(email, password).subscribe({
      next: () => {
        this.router.navigate(['/chats']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(this.getErrorMessage(err.code));
      }
    });
  }

  private getErrorMessage(code: string): string {
    switch (code) {
      case 'auth/invalid-credential':
      case 'auth/user-not-found':
      case 'auth/wrong-password':
        return 'Invalid email or password';
      case 'auth/too-many-requests':
        return 'Too many failed attempts. Please try again later.';
      case 'auth/user-disabled':
        return 'This account has been disabled';
      default:
        return 'An error occurred. Please try again.';
    }
  }
}
