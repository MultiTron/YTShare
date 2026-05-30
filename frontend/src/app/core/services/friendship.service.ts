import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserOutput } from './user.service';

export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface FriendshipInput {
  userId: string;
  friendId: string;
  status: FriendshipStatus;
}

export interface FriendshipOutput {
  id: string;
  user: UserOutput;
  friend: UserOutput;
  status: FriendshipStatus;
}

@Injectable({
  providedIn: 'root'
})
export class FriendshipService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/friendships`;

  getFriendshipsByUser(userId: string): Observable<FriendshipOutput[]> {
    return this.http.get<FriendshipOutput[]>(`${this.apiUrl}/user/${userId}`);
  }

  getFriendshipsByUserAndStatus(userId: string, status: FriendshipStatus): Observable<FriendshipOutput[]> {
    return this.http.get<FriendshipOutput[]>(`${this.apiUrl}/user/${userId}/status`, {
      params: { status }
    });
  }

  createFriendship(input: FriendshipInput): Observable<FriendshipOutput> {
    return this.http.post<FriendshipOutput>(this.apiUrl, input);
  }

  updateFriendshipStatus(id: string, status: FriendshipStatus): Observable<FriendshipOutput> {
    return this.http.patch<FriendshipOutput>(`${this.apiUrl}/${id}/status`, null, {
      params: { status }
    });
  }

  deleteFriendship(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
