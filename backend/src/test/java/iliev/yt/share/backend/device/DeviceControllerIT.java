package iliev.yt.share.backend.device;

import iliev.yt.share.backend.device.dto.DeviceInputDto;
import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.preferences.UserPreferences;
import iliev.yt.share.backend.user.preferences.UserPreferencesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceControllerIT extends AbstractIntegrationTest {

    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    @Autowired
    private UserRepository userRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-dev", "dev@example.com");
    }

    private UserPreferences persistPrefs(final String suffix) {
        final User user = userRepository.save(User.builder()
                .firebaseUid("uid-" + suffix)
                .email(suffix + "@example.com")
                .firstName("F" + suffix)
                .lastName("L" + suffix)
                .build());
        return userPreferencesRepository.save(UserPreferences.builder()
                .darkMode(false)
                .notificationsEnabled(true)
                .trackingEnabled(false)
                .user(user)
                .build());
    }

    private Device persistDevice(final UserPreferences prefs, final String host) {
        return deviceRepository.save(Device.builder()
                .hostName(host)
                .ipAddress("10.0.0.1")
                .port("8080")
                .lastConnectedTo(LocalDateTime.now())
                .userPreferences(prefs)
                .build());
    }

    @Test
    void createDevice_persists() throws Exception {
        final UserPreferences prefs = persistPrefs("c1");
        final DeviceInputDto input = new DeviceInputDto("laptop", "10.0.0.5", "9090", LocalDateTime.now(), prefs.getId());

        mockMvc.perform(post("/devices")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostName").value("laptop"))
                .andExpect(jsonPath("$.userPreferencesId").value(prefs.getId().toString()));
    }

    @Test
    void createDevice_prefsNotFound_returns404() throws Exception {
        final DeviceInputDto input = new DeviceInputDto("laptop", "10.0.0.5", "9090", LocalDateTime.now(), UUID.randomUUID());

        mockMvc.perform(post("/devices")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDeviceById_found() throws Exception {
        final Device device = persistDevice(persistPrefs("g1"), "host-g1");

        mockMvc.perform(get("/devices/{id}", device.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostName").value("host-g1"));
    }

    @Test
    void getDeviceById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/devices/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDevicesByUserPreferencesId_returnsList() throws Exception {
        final UserPreferences prefs = persistPrefs("byp");
        persistDevice(prefs, "host-byp");

        mockMvc.perform(get("/devices/user-preferences/{userPreferencesId}", prefs.getId())
                        .header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hostName").value("host-byp"));
    }

    @Test
    void getAllDevices_paged() throws Exception {
        mockMvc.perform(get("/devices").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getAllDevices_list() throws Exception {
        mockMvc.perform(get("/devices/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateDevice_changesValues() throws Exception {
        final UserPreferences prefs = persistPrefs("up");
        final Device device = persistDevice(prefs, "old-host");
        final DeviceInputDto input = new DeviceInputDto("new-host", "10.0.0.9", "7070", LocalDateTime.now(), prefs.getId());

        mockMvc.perform(put("/devices/{id}", device.getId())
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostName").value("new-host"));
    }

    @Test
    void deleteDevice_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/devices/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/devices/all")).andExpect(status().is4xxClientError());
    }
}
