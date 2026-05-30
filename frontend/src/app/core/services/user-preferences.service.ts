import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserPreferencesOutput } from './user.service';

export interface UserPreferencesInput {
  darkMode: boolean;
  notificationsEnabled: boolean;
  trackingEnabled: boolean;
  userId: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserPreferencesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/user-preferences`;

  getByUserId(userId: string): Observable<UserPreferencesOutput> {
    return this.http.get<UserPreferencesOutput>(`${this.apiUrl}/user/${userId}`);
  }

  create(input: UserPreferencesInput): Observable<UserPreferencesOutput> {
    return this.http.post<UserPreferencesOutput>(this.apiUrl, input);
  }

  update(id: string, input: UserPreferencesInput): Observable<UserPreferencesOutput> {
    return this.http.put<UserPreferencesOutput>(`${this.apiUrl}/${id}`, input);
  }
}
