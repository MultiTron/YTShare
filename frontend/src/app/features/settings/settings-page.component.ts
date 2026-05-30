import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserService, UserOutput, UserPreferencesOutput } from '../../core/services/user.service';
import { UserPreferencesService, UserPreferencesInput } from '../../core/services/user-preferences.service';

@Component({
  selector: 'app-settings-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss'
})
export class SettingsPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly preferencesService = inject(UserPreferencesService);
  private readonly router = inject(Router);

  readonly currentUser = signal<UserOutput | null>(null);
  readonly preferences = signal<UserPreferencesOutput | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly saveSuccess = signal(false);

  readonly darkMode = signal(false);
  readonly notificationsEnabled = signal(true);
  readonly trackingEnabled = signal(false);

  ngOnInit(): void {
    const firebaseUser = this.authService.currentUser();
    if (!firebaseUser) return;

    this.userService.getUserByFirebaseUid(firebaseUser.uid).subscribe({
      next: (user) => {
        this.currentUser.set(user);
        if (user.userPreferences) {
          this.preferences.set(user.userPreferences);
          this.darkMode.set(user.userPreferences.darkMode);
          this.notificationsEnabled.set(user.userPreferences.notificationsEnabled);
          this.trackingEnabled.set(user.userPreferences.trackingEnabled);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  toggleDarkMode(): void {
    this.darkMode.update(v => !v);
    this.save();
  }

  toggleNotifications(): void {
    this.notificationsEnabled.update(v => !v);
    this.save();
  }

  toggleTracking(): void {
    this.trackingEnabled.update(v => !v);
    this.save();
  }

  private save(): void {
    const user = this.currentUser();
    if (!user) return;

    this.saving.set(true);
    this.saveSuccess.set(false);

    const input: UserPreferencesInput = {
      darkMode: this.darkMode(),
      notificationsEnabled: this.notificationsEnabled(),
      trackingEnabled: this.trackingEnabled(),
      userId: user.id
    };

    const prefs = this.preferences();
    const request = prefs
      ? this.preferencesService.update(prefs.id, input)
      : this.preferencesService.create(input);

    request.subscribe({
      next: (result) => {
        this.preferences.set(result);
        this.saving.set(false);
        this.saveSuccess.set(true);
        setTimeout(() => this.saveSuccess.set(false), 2000);
      },
      error: () => this.saving.set(false)
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/'])
    });
  }
}
