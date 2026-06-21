package iliev.yt.share.backend.friends;

import iliev.yt.share.backend.friends.dto.FriendshipInputDto;
import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FriendshipControllerIT extends AbstractIntegrationTest {

    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private UserRepository userRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-f", "f@example.com");
    }

    private User persistUser(final String suffix) {
        return userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
    }

    private Friendship persistFriendship(final User user, final User friend, final FriendshipStatus status) {
        return friendshipRepository.save(Friendship.builder()
                .user(user)
                .friend(friend)
                .status(status)
                .build());
    }

    @Test
    void createFriendship_persists() throws Exception {
        final User user = persistUser("u1");
        final User friend = persistUser("u2");
        final FriendshipInputDto input = new FriendshipInputDto(user.getId(), friend.getId(), FriendshipStatus.PENDING);

        mockMvc.perform(post("/friendships")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getFriendshipById_found() throws Exception {
        final Friendship f = persistFriendship(persistUser("a"), persistUser("b"), FriendshipStatus.PENDING);

        mockMvc.perform(get("/friendships/{id}", f.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(f.getId().toString()));
    }

    @Test
    void getFriendshipById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/friendships/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFriendshipsByUserId_returnsList() throws Exception {
        final User user = persistUser("byu");
        persistFriendship(user, persistUser("byf"), FriendshipStatus.PENDING);

        mockMvc.perform(get("/friendships/user/{userId}", user.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getFriendshipsByUserIdAndStatus_returnsList() throws Exception {
        final User user = persistUser("su");
        persistFriendship(user, persistUser("sf"), FriendshipStatus.ACCEPTED);

        mockMvc.perform(get("/friendships/user/{userId}/status", user.getId())
                        .param("status", "ACCEPTED")
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllFriendships_paged() throws Exception {
        mockMvc.perform(get("/friendships").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllFriendships_list() throws Exception {
        mockMvc.perform(get("/friendships/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateFriendshipStatus_changesStatus() throws Exception {
        final Friendship f = persistFriendship(persistUser("up1"), persistUser("up2"), FriendshipStatus.PENDING);

        mockMvc.perform(patch("/friendships/{id}/status", f.getId())
                        .param("status", "ACCEPTED")
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void updateFriendshipStatus_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/friendships/{id}/status", UUID.randomUUID())
                        .param("status", "ACCEPTED")
                        .header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFriendship_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/friendships/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/friendships/all")).andExpect(status().is4xxClientError());
    }
}
