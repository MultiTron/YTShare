package iliev.yt.share.backend.message;

import iliev.yt.share.backend.chat.Chat;
import iliev.yt.share.backend.chat.ChatRepository;
import iliev.yt.share.backend.chat.exception.ChatNotFoundException;
import iliev.yt.share.backend.devicetoken.DeviceToken;
import iliev.yt.share.backend.devicetoken.DeviceTokenService;
import iliev.yt.share.backend.message.dto.MessageInputDto;
import iliev.yt.share.backend.message.dto.MessageOutputDto;
import iliev.yt.share.backend.message.enums.DeliveryStatus;
import iliev.yt.share.backend.message.exception.MessageNotFoundException;
import iliev.yt.share.backend.notification.FcmService;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private DeviceTokenService deviceTokenService;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private MessageService messageService;

    private UUID id;
    private Message message;
    private MessageOutputDto outputDto;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        message = Message.builder().content("hi").status(DeliveryStatus.SENT).build();
        message.setId(id);
        outputDto = new MessageOutputDto(id, "hi", DeliveryStatus.SENT, null, null, null);
    }

    private User userWithId(final UUID userId, final String firstName) {
        final User user = User.builder().firstName(firstName).build();
        user.setId(userId);
        return user;
    }

    @Test
    void getAllMessages_returnsMappedList() {
        when(messageRepository.findAll()).thenReturn(List.of(message));
        when(messageMapper.toOutputDto(message)).thenReturn(outputDto);

        assertThat(messageService.getAllMessages()).containsExactly(outputDto);
    }

    @Test
    void getAllMessages_paged_returnsMappedPage() {
        final Pageable pageable = PageRequest.of(0, 10);
        when(messageRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(message)));
        when(messageMapper.toOutputDto(message)).thenReturn(outputDto);

        final Page<MessageOutputDto> result = messageService.getAllMessages(pageable);

        assertThat(result.getContent()).containsExactly(outputDto);
    }

    @Test
    void getMessageById_found_returnsDto() {
        when(messageRepository.findById(id)).thenReturn(Optional.of(message));
        when(messageMapper.toOutputDto(message)).thenReturn(outputDto);

        assertThat(messageService.getMessageById(id)).isEqualTo(outputDto);
    }

    @Test
    void getMessageById_notFound_throws() {
        when(messageRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.getMessageById(id))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void getMessagesByChatId_returnsMappedList() {
        final UUID chatId = UUID.randomUUID();
        when(messageRepository.findByChatId(chatId)).thenReturn(List.of(message));
        when(messageMapper.toOutputDto(message)).thenReturn(outputDto);

        assertThat(messageService.getMessagesByChatId(chatId)).containsExactly(outputDto);
    }

    @Test
    void getMessagesBySenderId_returnsMappedList() {
        final UUID senderId = UUID.randomUUID();
        when(messageRepository.findBySenderId(senderId)).thenReturn(List.of(message));
        when(messageMapper.toOutputDto(message)).thenReturn(outputDto);

        assertThat(messageService.getMessagesBySenderId(senderId)).containsExactly(outputDto);
    }

    @Test
    void createMessage_savesBroadcastsAndNotifiesRecipients() {
        final UUID chatId = UUID.randomUUID();
        final UUID senderId = UUID.randomUUID();
        final UUID recipientId = UUID.randomUUID();
        final User sender = userWithId(senderId, "Alice");
        final User recipient = userWithId(recipientId, "Bob");

        final Chat chat = Chat.builder().participants(new ArrayList<>(List.of(sender, recipient))).build();
        chat.setId(chatId);

        final MessageInputDto inputDto = new MessageInputDto("hello", DeliveryStatus.SENT, chatId, senderId);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(messageMapper.toOutputDto(message)).thenReturn(outputDto);

        final DeviceToken token = DeviceToken.builder().fcmToken("fcm-1").build();
        when(deviceTokenService.getTokensByUserIds(List.of(recipientId))).thenReturn(List.of(token));

        final MessageOutputDto result = messageService.createMessage(inputDto);

        assertThat(result).isEqualTo(outputDto);
        verify(messagingTemplate).convertAndSend("/topic/chat/" + chatId, outputDto);
        verify(fcmService).sendPushNotification(
                eq("fcm-1"), eq("Alice"), eq("hello"),
                eq(chatId.toString()), eq(senderId.toString()), eq("Alice"));
    }

    @Test
    void createMessage_chatNotFound_throws() {
        final UUID chatId = UUID.randomUUID();
        final MessageInputDto inputDto = new MessageInputDto("hi", DeliveryStatus.SENT, chatId, UUID.randomUUID());
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.createMessage(inputDto))
                .isInstanceOf(ChatNotFoundException.class);
        verifyNoInteractions(messagingTemplate, fcmService);
    }

    @Test
    void createMessage_senderNotFound_throws() {
        final UUID chatId = UUID.randomUUID();
        final UUID senderId = UUID.randomUUID();
        final Chat chat = Chat.builder().participants(new ArrayList<>()).build();
        chat.setId(chatId);
        final MessageInputDto inputDto = new MessageInputDto("hi", DeliveryStatus.SENT, chatId, senderId);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findById(senderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.createMessage(inputDto))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(messagingTemplate, fcmService);
    }

    @Test
    void deleteMessage_exists_deletes() {
        when(messageRepository.existsById(id)).thenReturn(true);

        messageService.deleteMessage(id);

        verify(messageRepository).deleteById(id);
    }

    @Test
    void deleteMessage_notExists_throwsAndDoesNotDelete() {
        when(messageRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> messageService.deleteMessage(id))
                .isInstanceOf(MessageNotFoundException.class);
        verify(messageRepository, never()).deleteById(id);
    }
}
