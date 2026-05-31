import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface VideoOutput {
  id: string;
  title: string;
  url: string;
  thumbnailUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class VideoService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/videos`;

  getAllVideos(): Observable<VideoOutput[]> {
    return this.http.get<VideoOutput[]>(`${this.apiUrl}/all`);
  }

  getVideoById(id: string): Observable<VideoOutput> {
    return this.http.get<VideoOutput>(`${this.apiUrl}/${id}`);
  }

  deleteVideo(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
