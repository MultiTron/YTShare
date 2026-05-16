import { inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn, Router } from '@angular/router';
import { filter, map, take } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoading()) {
    return authService.isAuthenticated() || router.createUrlTree(['/login']);
  }

  return toObservable(authService.isLoading).pipe(
    filter(loading => !loading),
    take(1),
    map(() => authService.isAuthenticated() || router.createUrlTree(['/login']))
  );
};

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoading()) {
    return !authService.isAuthenticated() || router.createUrlTree(['/chats']);
  }

  return toObservable(authService.isLoading).pipe(
    filter(loading => !loading),
    take(1),
    map(() => !authService.isAuthenticated() || router.createUrlTree(['/chats']))
  );
};
