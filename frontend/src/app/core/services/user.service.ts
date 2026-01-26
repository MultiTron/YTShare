import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserInput {
  firebaseUid: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface UserOutput {
  id: string;
  firebaseUid: string;
  email: string;
  firstName: string;
  lastName: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/users`;

  createUser(user: UserInput): Observable<UserOutput> {
    return this.http.post<UserOutput>(this.apiUrl, user);
  }

  getUserByFirebaseUid(firebaseUid: string): Observable<UserOutput> {
    return this.http.get<UserOutput>(`${this.apiUrl}/by-firebase-uid`, {
      params: { firebaseUid }
    });
  }

  getUserByEmail(email: string): Observable<UserOutput> {
    return this.http.get<UserOutput>(`${this.apiUrl}/by-email`, {
      params: { email }
    });
  }
}
