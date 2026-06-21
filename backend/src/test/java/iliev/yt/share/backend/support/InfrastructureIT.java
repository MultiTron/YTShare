package iliev.yt.share.backend.support;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InfrastructureIT extends AbstractIntegrationTest {

    @Test
    void contextLoads_andContainerIsRunning() {
        org.assertj.core.api.Assertions.assertThat(POSTGRES.isRunning()).isTrue();
    }

    @Test
    void unauthenticatedRequest_isRejected() throws Exception {
        // No Authorization header -> Spring Security rejects before the controller runs.
        mockMvc.perform(get("/users/all"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void authenticatedRequest_reachesController() throws Exception {
        mockMvc.perform(get("/users/all")
                        .header("Authorization", authHeaderFor("uid-smoke", "smoke@example.com")))
                .andExpect(status().isOk());
    }
}
