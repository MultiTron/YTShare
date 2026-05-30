import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserService, UserOutput, DeviceOutput } from '../../core/services/user.service';
import { DeviceService, DeviceInput } from '../../core/services/device.service';

@Component({
  selector: 'app-devices-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink],
  templateUrl: './devices-page.component.html',
  styleUrl: './devices-page.component.scss'
})
export class DevicesPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly deviceService = inject(DeviceService);
  private readonly router = inject(Router);

  readonly currentUser = signal<UserOutput | null>(null);
  readonly devices = signal<DeviceOutput[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly editingDevice = signal<DeviceOutput | null>(null);
  readonly saving = signal(false);
  readonly formError = signal('');

  readonly formHostName = signal('');
  readonly formIpAddress = signal('');
  readonly formPort = signal('');

  ngOnInit(): void {
    const firebaseUser = this.authService.currentUser();
    if (!firebaseUser) return;

    this.userService.getUserByFirebaseUid(firebaseUser.uid).subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.loadDevices();
      },
      error: () => this.loading.set(false)
    });
  }

  loadDevices(): void {
    const user = this.currentUser();
    if (!user?.userPreferences) {
      this.loading.set(false);
      return;
    }

    this.deviceService.getDevicesByUserPreferencesId(user.userPreferences.id).subscribe({
      next: (devices) => {
        this.devices.set(devices);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openAddForm(): void {
    this.editingDevice.set(null);
    this.formHostName.set('');
    this.formIpAddress.set('');
    this.formPort.set('');
    this.formError.set('');
    this.showForm.set(true);
  }

  openEditForm(device: DeviceOutput): void {
    this.editingDevice.set(device);
    this.formHostName.set(device.hostName);
    this.formIpAddress.set(device.ipAddress);
    this.formPort.set(device.port);
    this.formError.set('');
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingDevice.set(null);
  }

  saveDevice(): void {
    const hostName = this.formHostName().trim();
    const ipAddress = this.formIpAddress().trim();
    const port = this.formPort().trim();
    const user = this.currentUser();

    if (!hostName || !ipAddress || !port) {
      this.formError.set('All fields are required.');
      return;
    }

    if (!user?.userPreferences) {
      this.formError.set('User preferences not found.');
      return;
    }

    this.saving.set(true);
    this.formError.set('');

    const input: DeviceInput = {
      hostName,
      ipAddress,
      port,
      lastConnectedTo: this.editingDevice()?.lastConnectedTo ?? null,
      userPreferencesId: user.userPreferences.id
    };

    const editing = this.editingDevice();
    const request = editing
      ? this.deviceService.updateDevice(editing.id, input)
      : this.deviceService.createDevice(input);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.editingDevice.set(null);
        this.loadDevices();
      },
      error: () => {
        this.saving.set(false);
        this.formError.set('Failed to save device.');
      }
    });
  }

  deleteDevice(device: DeviceOutput): void {
    this.deviceService.deleteDevice(device.id).subscribe({
      next: () => this.loadDevices()
    });
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return 'Never';
    const d = new Date(dateStr);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/'])
    });
  }
}
