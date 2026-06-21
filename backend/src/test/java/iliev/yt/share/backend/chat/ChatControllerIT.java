package iliev.yt.share.backend.chat;

import iliev.yt.share.backend.chat.dto.ChatInputDto;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerIT extends AbstractIntegrationTest {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-c", "c@example.com");
    }

    private User persistUser(final String suffix) {
        return userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
    }

    private Chat persistChatWith(final User... participants) {
        // Use a mutable list: addParticipants() mutates the collection in place, and
        // because the test runs in a single transaction findById returns this same
        // instance. In production the chat is loaded in a separate transaction as a
        // mutable Hibernate PersistentBag, so this matches real behavior.
        return chatRepository.save(Chat.builder()
                .participants(new ArrayList<>(List.of(participants)))
                .build());
    }

    @Test
    void createChat_withExistingParticipants_persistsJoinRows() throws Exception {
        final User u1 = persistUser("p1");
        final User u2 = persistUser("p2");
        final ChatInputDto input = new ChatInputDto(List.of(u1.getId(), u2.getId()));

        mockMvc.perform(post("/chats")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2));
    }

    @Test
    void createChat_withMissingParticipant_returns404() throws Exception {
        final User u1 = persistUser("p1");
        final ChatInputDto input = new ChatInputDto(List.of(u1.getId(), UUID.randomUUID()));

        mockMvc.perform(post("/chats")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getChatById_found() throws Exception {
        final Chat chat = persistChatWith(persistUser("g1"));

        mockMvc.perform(get("/chats/{id}", chat.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(chat.getId().toString()));
    }

    @Test
    void getChatById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/chats/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllChats_paged() throws Exception {
        mockMvc.perform(get("/chats").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllChats_list() throws Exception {
        persistChatWith(persistUser("l1"));

        mockMvc.perform(get("/chats/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void addParticipants_appendsToChat() throws Exception {
        final Chat chat = persistChatWith(persistUser("a1"));
        final User added = persistUser("a2");

        mockMvc.perform(patch("/chats/{id}/participants", chat.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(added.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2));
    }

    @Test
    void deleteChat_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/chats/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/chats/all")).andExpect(status().is4xxClientError());
    }
}
