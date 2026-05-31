import { Injectable, inject, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { MessageOutput } from './chat.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private readonly authService = inject(AuthService);
  private client: Client | null = null;
  private subscriptions = new Map<string, { unsubscribe: () => void; subject: Subject<MessageOutput> }>();

  async connect(): Promise<void> {
    if (this.client?.connected) return;

    const token = await this.authService.getIdToken();
    if (!token) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000
    });

    this.client.activate();
  }

  subscribeToChat(chatId: string): Observable<MessageOutput> {
    const existing = this.subscriptions.get(chatId);
    if (existing) return existing.subject.asObservable();

    const subject = new Subject<MessageOutput>();

    const trySubscribe = () => {
      if (!this.client?.connected) {
        setTimeout(trySubscribe, 500);
        return;
      }

      const sub = this.client.subscribe(`/topic/chat/${chatId}`, (message: IMessage) => {
        const msg: MessageOutput = JSON.parse(message.body);
        subject.next(msg);
      });

      this.subscriptions.set(chatId, { unsubscribe: () => sub.unsubscribe(), subject });
    };

    trySubscribe();
    return subject.asObservable();
  }

  unsubscribeFromChat(chatId: string): void {
    const sub = this.subscriptions.get(chatId);
    if (sub) {
      sub.unsubscribe();
      sub.subject.complete();
      this.subscriptions.delete(chatId);
    }
  }

  disconnect(): void {
    this.subscriptions.forEach(sub => {
      sub.unsubscribe();
      sub.subject.complete();
    });
    this.subscriptions.clear();
    this.client?.deactivate();
    this.client = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
