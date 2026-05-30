import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DeviceOutput } from './user.service';

export interface DeviceInput {
  hostName: string;
  ipAddress: string;
  port: string;
  lastConnectedTo: string | null;
  userPreferencesId: string;
}

@Injectable({
  providedIn: 'root'
})
export class DeviceService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/devices`;

  getDevicesByUserPreferencesId(userPreferencesId: string): Observable<DeviceOutput[]> {
    return this.http.get<DeviceOutput[]>(`${this.apiUrl}/user-preferences/${userPreferencesId}`);
  }

  createDevice(input: DeviceInput): Observable<DeviceOutput> {
    return this.http.post<DeviceOutput>(this.apiUrl, input);
  }

  updateDevice(id: string, input: DeviceInput): Observable<DeviceOutput> {
    return this.http.put<DeviceOutput>(`${this.apiUrl}/${id}`, input);
  }

  deleteDevice(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
