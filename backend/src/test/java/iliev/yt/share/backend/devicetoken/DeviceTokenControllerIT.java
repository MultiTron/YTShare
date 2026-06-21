package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.devicetoken.dto.DeviceTokenInputDto;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceTokenControllerIT extends AbstractIntegrationTest {

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    @Autowired
    private UserRepository userRepository;

    private User persistUser(final String uid, final String email) {
        return userRepository.save(User.builder()
                .firebaseUid(uid)
                .email(email)
                .firstName("F")
                .lastName("L")
                .build());
    }

    @Test
    void registerToken_createsNewToken() throws Exception {
        persistUser("uid-reg", "reg@example.com");
        final DeviceTokenInputDto input = new DeviceTokenInputDto("fcm-abc", "ANDROID");

        mockMvc.perform(post("/device-tokens")
                        .header("Authorization", authHeaderFor("uid-reg", "reg@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fcmToken").value("fcm-abc"))
                .andExpect(jsonPath("$.platform").value("ANDROID"));

        assertThat(deviceTokenRepository.findByFcmToken("fcm-abc")).isPresent();
    }

    @Test
    void registerToken_existingToken_isReassignedNotDuplicated() throws Exception {
        final User user = persistUser("uid-up", "up@example.com");
        deviceTokenRepository.save(DeviceToken.builder()
                .fcmToken("fcm-shared")
                .platform("IOS")
                .user(user)
                .build());
        final DeviceTokenInputDto input = new DeviceTokenInputDto("fcm-shared", "ANDROID");

        mockMvc.perform(post("/device-tokens")
                        .header("Authorization", authHeaderFor("uid-up", "up@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("ANDROID"));

        assertThat(deviceTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void registerToken_noUserForUid_returns404() throws Exception {
        final DeviceTokenInputDto input = new DeviceTokenInputDto("fcm-x", "ANDROID");

        mockMvc.perform(post("/device-tokens")
                        .header("Authorization", authHeaderFor("uid-missing", "missing@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeToken_deletesCurrentUsersTokens() throws Exception {
        final User user = persistUser("uid-del", "del@example.com");
        deviceTokenRepository.save(DeviceToken.builder()
                .fcmToken("fcm-del")
                .platform("ANDROID")
                .user(user)
                .build());

        mockMvc.perform(delete("/device-tokens")
                        .header("Authorization", authHeaderFor("uid-del", "del@example.com")))
                .andExpect(status().isOk());

        assertThat(deviceTokenRepository.findByFcmToken("fcm-del")).isEmpty();
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(delete("/device-tokens")).andExpect(status().is4xxClientError());
    }
}
