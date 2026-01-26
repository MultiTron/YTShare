import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet],
  template: `
    @if (authService.isLoading()) {
      <div class="loading-screen" role="status" aria-label="Loading application">
        <div class="spinner" aria-hidden="true"></div>
        <span class="sr-only">Loading...</span>
      </div>
    } @else {
      <router-outlet />
    }
  `,
  styles: `
    .loading-screen {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--color-background);
    }

    .spinner {
      width: 2.5rem;
      height: 2.5rem;
      border: 3px solid var(--color-border);
      border-top-color: var(--color-primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
  `
})
export class AppComponent {
  readonly authService = inject(AuthService);
}
