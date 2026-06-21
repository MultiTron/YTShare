package iliev.yt.share.backend.user.preferences;

import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.preferences.dto.UserPreferencesInputDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserPreferencesControllerIT extends AbstractIntegrationTest {

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    @Autowired
    private UserRepository userRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-pref", "pref@example.com");
    }

    private User persistUser(final String suffix) {
        return userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
    }

    private UserPreferences persistPrefs(final User user, final boolean dark) {
        return userPreferencesRepository.save(UserPreferences.builder()
                .darkMode(dark)
                .notificationsEnabled(true)
                .trackingEnabled(false)
                .user(user)
                .build());
    }

    @Test
    void createUserPreferences_persists() throws Exception {
        final User user = persistUser("c1");
        final UserPreferencesInputDto input = new UserPreferencesInputDto(true, true, false, user.getId());

        mockMvc.perform(post("/user-preferences")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkMode").value(true))
                .andExpect(jsonPath("$.userId").value(user.getId().toString()));
    }

    @Test
    void createUserPreferences_userNotFound_returns404() throws Exception {
        final UserPreferencesInputDto input = new UserPreferencesInputDto(true, true, false, UUID.randomUUID());

        mockMvc.perform(post("/user-preferences")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_found() throws Exception {
        final UserPreferences prefs = persistPrefs(persistUser("g1"), true);

        mockMvc.perform(get("/user-preferences/{id}", prefs.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(prefs.getId().toString()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/user-preferences/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByUserId_found() throws Exception {
        final User user = persistUser("byuser");
        persistPrefs(user, false);

        mockMvc.perform(get("/user-preferences/user/{userId}", user.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()));
    }

    @Test
    void getByUserId_notFound_returns404() throws Exception {
        mockMvc.perform(get("/user-preferences/user/{userId}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_paged() throws Exception {
        mockMvc.perform(get("/user-preferences").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAll_list() throws Exception {
        mockMvc.perform(get("/user-preferences/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updatePreferences_changesValues() throws Exception {
        final User user = persistUser("up");
        final UserPreferences prefs = persistPrefs(user, false);
        final UserPreferencesInputDto input = new UserPreferencesInputDto(true, false, true, user.getId());

        mockMvc.perform(put("/user-preferences/{id}", prefs.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkMode").value(true));
    }

    @Test
    void updatePreferences_notFound_returns404() throws Exception {
        final UserPreferencesInputDto input = new UserPreferencesInputDto(true, false, true, UUID.randomUUID());

        mockMvc.perform(put("/user-preferences/{id}", UUID.randomUUID())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePreferences_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/user-preferences/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/user-preferences/all")).andExpect(status().is4xxClientError());
    }
}
