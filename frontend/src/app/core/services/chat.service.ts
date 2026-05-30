import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserOutput } from './user.service';

export type DeliveryStatus = 'PENDING' | 'SENT' | 'READ';

export interface ChatInput {
  participantIds: string[];
}

export interface ChatOutput {
  id: string;
  participants: UserOutput[];
}

export interface MessageInput {
  content: string;
  status: DeliveryStatus;
  chatId: string;
  senderId: string;
}

export interface MessageOutput {
  id: string;
  content: string;
  status: DeliveryStatus;
  chat: ChatOutput;
  sender: UserOutput;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly chatsUrl = `${environment.apiUrl}/chats`;
  private readonly messagesUrl = `${environment.apiUrl}/messages`;

  getAllChats(): Observable<ChatOutput[]> {
    return this.http.get<ChatOutput[]>(`${this.chatsUrl}/all`);
  }

  getChatById(id: string): Observable<ChatOutput> {
    return this.http.get<ChatOutput>(`${this.chatsUrl}/${id}`);
  }

  createChat(input: ChatInput): Observable<ChatOutput> {
    return this.http.post<ChatOutput>(this.chatsUrl, input);
  }

  getMessagesByChat(chatId: string): Observable<MessageOutput[]> {
    return this.http.get<MessageOutput[]>(`${this.messagesUrl}/chat/${chatId}`);
  }

  sendMessage(input: MessageInput): Observable<MessageOutput> {
    return this.http.post<MessageOutput>(this.messagesUrl, input);
  }
}
