import { ChangeDetectionStrategy, Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { UserService, UserOutput } from '../../core/services/user.service';
import { FriendshipService, FriendshipOutput } from '../../core/services/friendship.service';
import { ChatService, ChatOutput, MessageOutput } from '../../core/services/chat.service';

type SidebarTab = 'chats' | 'friends';

@Component({
  selector: 'app-chat-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink],
  templateUrl: './chat-page.component.html',
  styleUrl: './chat-page.component.scss'
})
export class ChatPageComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly friendshipService = inject(FriendshipService);
  private readonly chatService = inject(ChatService);
  private readonly router = inject(Router);
  private pollingInterval: ReturnType<typeof setInterval> | null = null;

  readonly currentUser = signal<UserOutput | null>(null);
  readonly activeTab = signal<SidebarTab>('chats');
  readonly chats = signal<ChatOutput[]>([]);
  readonly friends = signal<FriendshipOutput[]>([]);
  readonly pendingRequests = signal<FriendshipOutput[]>([]);
  readonly activeChat = signal<ChatOutput | null>(null);
  readonly messages = signal<MessageOutput[]>([]);
  readonly messageText = signal('');
  readonly searchEmail = signal('');
  readonly searchError = signal('');
  readonly sendingMessage = signal(false);
  readonly loadingMessages = signal(false);
  readonly showAddFriend = signal(false);
  readonly mobileChatOpen = signal(false);

  readonly chatDisplayList = computed(() => {
    const user = this.currentUser();
    if (!user) return [];
    return this.chats().map(chat => {
      const otherParticipant = chat.participants.find(p => p.id !== user.id);
      return {
        ...chat,
        displayName: otherParticipant
          ? `${otherParticipant.firstName} ${otherParticipant.lastName}`
          : 'Unknown'
      };
    });
  });

  ngOnInit(): void {
    const firebaseUser = this.authService.currentUser();
    if (!firebaseUser) return;

    this.userService.getUserByFirebaseUid(firebaseUser.uid).subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.loadChats();
        this.loadFriends();
      }
    });
  }

  switchTab(tab: SidebarTab): void {
    this.activeTab.set(tab);
  }

  loadChats(): void {
    this.chatService.getAllChats().subscribe({
      next: (chats) => this.chats.set(chats)
    });
  }

  loadFriends(): void {
    const user = this.currentUser();
    if (!user) return;

    this.friendshipService.getFriendshipsByUserAndStatus(user.id, 'ACCEPTED').subscribe({
      next: (friends) => this.friends.set(friends)
    });

    this.friendshipService.getFriendshipsByUserAndStatus(user.id, 'PENDING').subscribe({
      next: (pending) => this.pendingRequests.set(pending)
    });
  }

  openChat(chat: ChatOutput): void {
    this.activeChat.set(chat);
    this.mobileChatOpen.set(true);
    this.loadMessages(chat.id);
    this.startPolling(chat.id);
  }

  loadMessages(chatId: string): void {
    this.loadingMessages.set(true);
    this.chatService.getMessagesByChat(chatId).subscribe({
      next: (msgs) => {
        this.messages.set(msgs);
        this.loadingMessages.set(false);
      },
      error: () => this.loadingMessages.set(false)
    });
  }

  private startPolling(chatId: string): void {
    this.stopPolling();
    this.pollingInterval = setInterval(() => {
      this.chatService.getMessagesByChat(chatId).subscribe({
        next: (msgs) => this.messages.set(msgs)
      });
    }, 3000);
  }

  private stopPolling(): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  sendMessage(): void {
    const text = this.messageText().trim();
    const chat = this.activeChat();
    const user = this.currentUser();
    if (!text || !chat || !user) return;

    this.sendingMessage.set(true);
    this.chatService.sendMessage({
      content: text,
      status: 'SENT',
      chatId: chat.id,
      senderId: user.id
    }).subscribe({
      next: (msg) => {
        this.messages.update(msgs => [...msgs, msg]);
        this.messageText.set('');
        this.sendingMessage.set(false);
      },
      error: () => this.sendingMessage.set(false)
    });
  }

  startChatWithFriend(friendship: FriendshipOutput): void {
    const user = this.currentUser();
    if (!user) return;

    const friendUser = friendship.user.id === user.id ? friendship.friend : friendship.user;

    const existingChat = this.chats().find(c =>
      c.participants.some(p => p.id === friendUser.id)
    );

    if (existingChat) {
      this.openChat(existingChat);
      this.activeTab.set('chats');
      return;
    }

    this.chatService.createChat({ participantIds: [user.id, friendUser.id] }).subscribe({
      next: (chat) => {
        this.chats.update(chats => [chat, ...chats]);
        this.openChat(chat);
        this.activeTab.set('chats');
      }
    });
  }

  sendFriendRequest(): void {
    const email = this.searchEmail().trim();
    const user = this.currentUser();
    if (!email || !user) return;

    this.searchError.set('');
    this.userService.getUserByEmail(email).subscribe({
      next: (foundUser) => {
        if (foundUser.id === user.id) {
          this.searchError.set('You cannot add yourself.');
          return;
        }
        this.friendshipService.createFriendship({
          userId: user.id,
          friendId: foundUser.id,
          status: 'PENDING'
        }).subscribe({
          next: () => {
            this.searchEmail.set('');
            this.showAddFriend.set(false);
            this.loadFriends();
          },
          error: () => this.searchError.set('Failed to send request.')
        });
      },
      error: () => this.searchError.set('User not found.')
    });
  }

  acceptRequest(friendship: FriendshipOutput): void {
    this.friendshipService.updateFriendshipStatus(friendship.id, 'ACCEPTED').subscribe({
      next: () => this.loadFriends()
    });
  }

  rejectRequest(friendship: FriendshipOutput): void {
    this.friendshipService.updateFriendshipStatus(friendship.id, 'REJECTED').subscribe({
      next: () => this.loadFriends()
    });
  }

  removeFriend(friendship: FriendshipOutput): void {
    this.friendshipService.deleteFriendship(friendship.id).subscribe({
      next: () => this.loadFriends()
    });
  }

  getFriendName(friendship: FriendshipOutput): string {
    const user = this.currentUser();
    if (!user) return '';
    const other = friendship.user.id === user.id ? friendship.friend : friendship.user;
    return `${other.firstName} ${other.lastName}`;
  }

  isIncomingRequest(friendship: FriendshipOutput): boolean {
    const user = this.currentUser();
    return !!user && friendship.friend.id === user.id;
  }

  getActiveChatDisplayName(): string {
    const chat = this.activeChat();
    if (!chat) return 'Chat';
    const found = this.chatDisplayList().find(c => c.id === chat.id);
    return found?.displayName ?? 'Chat';
  }

  getActiveChatInitial(): string {
    return this.getActiveChatDisplayName().charAt(0);
  }

  closeMobileChat(): void {
    this.mobileChatOpen.set(false);
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/'])
    });
  }
}
