package iliev.yt.share.backend.message;

import iliev.yt.share.backend.chat.Chat;
import iliev.yt.share.backend.chat.ChatRepository;
import iliev.yt.share.backend.devicetoken.DeviceToken;
import iliev.yt.share.backend.devicetoken.DeviceTokenRepository;
import iliev.yt.share.backend.message.dto.MessageInputDto;
import iliev.yt.share.backend.message.enums.DeliveryStatus;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-m", "m@example.com");
    }

    private User persistUser(final String suffix) {
        return userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
    }

    private Chat persistChat(final User... participants) {
        return chatRepository.save(Chat.builder()
                .participants(List.of(participants))
                .build());
    }

    private Message persistMessage(final Chat chat, final User sender, final String content) {
        return messageRepository.save(Message.builder()
                .content(content)
                .status(DeliveryStatus.SENT)
                .chat(chat)
                .sender(sender)
                .build());
    }

    @Test
    void createMessage_persists_broadcasts_andNotifiesRecipientTokens() throws Exception {
        final User sender = persistUser("sender");
        final User recipient = persistUser("recipient");
        final Chat chat = persistChat(sender, recipient);
        deviceTokenRepository.save(DeviceToken.builder()
                .fcmToken("fcm-recipient")
                .platform("ANDROID")
                .user(recipient)
                .build());

        final MessageInputDto input = new MessageInputDto("hello", DeliveryStatus.SENT, chat.getId(), sender.getId());

        mockMvc.perform(post("/messages")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("hello"));

        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + chat.getId()), (Object) any());
        verify(fcmService).sendPushNotification(
                eq("fcm-recipient"), anyString(), eq("hello"),
                eq(chat.getId().toString()), eq(sender.getId().toString()), anyString());
    }

    @Test
    void createMessage_chatNotFound_returns404() throws Exception {
        final User sender = persistUser("sender2");
        final MessageInputDto input = new MessageInputDto("x", DeliveryStatus.SENT, UUID.randomUUID(), sender.getId());

        mockMvc.perform(post("/messages")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMessage_senderNotFound_returns404() throws Exception {
        final Chat chat = persistChat(persistUser("only"));
        final MessageInputDto input = new MessageInputDto("x", DeliveryStatus.SENT, chat.getId(), UUID.randomUUID());

        mockMvc.perform(post("/messages")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessageById_found() throws Exception {
        final User sender = persistUser("s3");
        final Chat chat = persistChat(sender);
        final Message message = persistMessage(chat, sender, "stored");

        mockMvc.perform(get("/messages/{id}", message.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("stored"));
    }

    @Test
    void getMessageById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/messages/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessagesByChatId_returnsChatMessages() throws Exception {
        final User sender = persistUser("s4");
        final Chat chat = persistChat(sender);
        persistMessage(chat, sender, "c1");

        mockMvc.perform(get("/messages/chat/{chatId}", chat.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("c1"));
    }

    @Test
    void getMessagesBySenderId_returnsSenderMessages() throws Exception {
        final User sender = persistUser("s5");
        final Chat chat = persistChat(sender);
        persistMessage(chat, sender, "s1");

        mockMvc.perform(get("/messages/sender/{senderId}", sender.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("s1"));
    }

    @Test
    void getAllMessages_paged() throws Exception {
        mockMvc.perform(get("/messages").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllMessages_list() throws Exception {
        mockMvc.perform(get("/messages/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteMessage_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/messages/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/messages/all")).andExpect(status().is4xxClientError());
    }
}
