import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { VideoService, VideoOutput } from '../../core/services/video.service';

@Component({
  selector: 'app-history-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, FormsModule],
  templateUrl: './history-page.component.html',
  styleUrl: './history-page.component.scss'
})
export class HistoryPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly videoService = inject(VideoService);
  private readonly router = inject(Router);

  readonly videos = signal<VideoOutput[]>([]);
  readonly loading = signal(true);
  readonly searchQuery = signal('');

  readonly filteredVideos = signal<VideoOutput[]>([]);

  ngOnInit(): void {
    this.loadVideos();
  }

  loadVideos(): void {
    this.loading.set(true);
    this.videoService.getAllVideos().subscribe({
      next: (videos) => {
        this.videos.set(videos);
        this.filteredVideos.set(videos);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onSearch(): void {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) {
      this.filteredVideos.set(this.videos());
      return;
    }
    this.filteredVideos.set(
      this.videos().filter(v =>
        v.title.toLowerCase().includes(q)
      )
    );
  }

  openVideo(video: VideoOutput): void {
    window.open(video.url, '_blank');
  }

  deleteVideo(video: VideoOutput, event: Event): void {
    event.stopPropagation();
    this.videoService.deleteVideo(video.id).subscribe({
      next: () => {
        this.videos.update(vids => vids.filter(v => v.id !== video.id));
        this.onSearch();
      }
    });
  }

  getYoutubeId(url: string): string | null {
    const match = new RegExp(/(?:youtu\.be\/|v=|\/embed\/|\/v\/)([a-zA-Z0-9_-]{11})/).exec(url);
    return match ? match[1] : null;
  }

  getThumbnail(video: VideoOutput): string {
    if (video.thumbnailUrl) return video.thumbnailUrl;
    const id = this.getYoutubeId(video.url);
    if (id) return `https://img.youtube.com/vi/${id}/mqdefault.jpg`;
    return '';
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/'])
    });
  }
}
