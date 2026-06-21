package iliev.yt.share.backend.user;

import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.dto.UserInputDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User persistUser(final String uid, final String email, final String first, final String last) {
        return userRepository.save(User.builder()
                .firebaseUid(uid)
                .email(email)
                .firstName(first)
                .lastName(last)
                .build());
    }

    @Test
    void getAllUsers_returnsPersistedUsers() throws Exception {
        persistUser("uid-1", "a@example.com", "Alice", "A");

        mockMvc.perform(get("/users/all")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("a@example.com"));
    }

    @Test
    void getAllUsers_paged_returnsContentArray() throws Exception {
        persistUser("uid-1", "a@example.com", "Alice", "A");

        mockMvc.perform(get("/users")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getUserById_found() throws Exception {
        final User user = persistUser("uid-1", "a@example.com", "Alice", "A");

        mockMvc.perform(get("/users/{id}", user.getId())
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/users/{id}", UUID.randomUUID())
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getUserByFirebaseUid_found() throws Exception {
        persistUser("uid-find", "find@example.com", "Bob", "B");

        mockMvc.perform(get("/users/by-firebase-uid")
                        .param("firebaseUid", "uid-find")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("find@example.com"));
    }

    @Test
    void getUserByEmail_found() throws Exception {
        persistUser("uid-mail", "byemail@example.com", "Carol", "C");

        mockMvc.perform(get("/users/by-email")
                        .param("email", "byemail@example.com")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Carol"));
    }

    @Test
    void getUserByEmail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/users/by-email")
                        .param("email", "missing@example.com")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCurrentUser_resolvesAuthenticatedUid() throws Exception {
        persistUser("uid-me", "me@example.com", "Mia", "M");

        mockMvc.perform(get("/users/me")
                        .header("Authorization", authHeaderFor("uid-me", "me@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firebaseUid").value("uid-me"));
    }

    @Test
    void createUser_persistsAndReturnsDto() throws Exception {
        final UserInputDto input = new UserInputDto("uid-new", "new@example.com", "Ned", "N");

        mockMvc.perform(post("/users")
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));

        assertThat(userRepository.findByFirebaseUid("uid-new")).isPresent();
    }

    @Test
    void deleteUser_removesRow() throws Exception {
        final User user = persistUser("uid-del", "del@example.com", "Dan", "D");

        mockMvc.perform(delete("/users/{id}", user.getId())
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isOk());

        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID())
                        .header("Authorization", authHeaderFor("uid-1", "a@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/users/all"))
                .andExpect(status().is4xxClientError());
    }
}
