import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="home-container">
      <div class="home-card">
        <h1 class="home-title">Welcome to YTShare</h1>
        
        @if (authService.currentUser(); as user) {
          <div class="user-info">
            <p class="greeting">Hello, <strong>{{ user.displayName || user.email }}</strong>!</p>
            <p class="email">{{ user.email }}</p>
          </div>
          
          <p class="placeholder-text">
            Your dashboard content will appear here. Add more features as needed.
          </p>

          <button 
            type="button" 
            class="btn btn-secondary" 
            (click)="logout()"
            [disabled]="isLoggingOut"
          >
            @if (isLoggingOut) {
              Signing out...
            } @else {
              Sign Out
            }
          </button>
        } @else {
          <p class="placeholder-text">Loading...</p>
        }
      </div>
    </div>
  `,
  styles: `
    .home-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }

    .home-card {
      width: 100%;
      max-width: 500px;
      background: var(--color-surface);
      border-radius: var(--border-radius);
      box-shadow: var(--shadow-lg);
      padding: 2rem;
      text-align: center;
    }

    .home-title {
      margin: 0 0 1.5rem;
      font-size: 1.75rem;
      font-weight: 700;
      color: var(--color-text);
    }

    .user-info {
      margin-bottom: 1.5rem;
      padding: 1rem;
      background: var(--color-background);
      border-radius: var(--border-radius);
    }

    .greeting {
      margin: 0 0 0.25rem;
      font-size: 1.125rem;
      color: var(--color-text);
    }

    .email {
      margin: 0;
      font-size: 0.875rem;
      color: var(--color-text-muted);
    }

    .placeholder-text {
      margin: 0 0 1.5rem;
      color: var(--color-text-muted);
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

    .btn-secondary {
      background: var(--color-border);
      color: var(--color-text);

      &:hover:not(:disabled) {
        background: #d1d5db;
      }

      &:focus-visible {
        outline: 2px solid var(--color-primary);
        outline-offset: 2px;
      }
    }
  `
})
export class HomeComponent {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  isLoggingOut = false;

  logout(): void {
    this.isLoggingOut = true;
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: () => {
        this.isLoggingOut = false;
      }
    });
  }
}
