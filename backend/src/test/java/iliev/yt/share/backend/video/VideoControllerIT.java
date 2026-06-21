package iliev.yt.share.backend.video;

import iliev.yt.share.backend.support.AbstractIntegrationTest;
import iliev.yt.share.backend.video.dto.VideoInputDto;
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

class VideoControllerIT extends AbstractIntegrationTest {

    @Autowired
    private VideoRepository videoRepository;

    private String auth() throws Exception {
        return authHeaderFor("uid-v", "v@example.com");
    }

    private Video persistVideo(final String title) {
        return videoRepository.save(Video.builder()
                .title(title)
                .url("https://youtu.be/" + title)
                .thumbnailUrl("https://img/" + title)
                .build());
    }

    @Test
    void getAllVideos_returnsPersisted() throws Exception {
        persistVideo("clip");

        mockMvc.perform(get("/videos/all").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("clip"));
    }

    @Test
    void getAllVideos_paged() throws Exception {
        persistVideo("clip");

        mockMvc.perform(get("/videos").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getVideoById_found() throws Exception {
        final Video video = persistVideo("clip");

        mockMvc.perform(get("/videos/{id}", video.getId()).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("clip"));
    }

    @Test
    void getVideoById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/videos/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createVideo_persists() throws Exception {
        final VideoInputDto input = new VideoInputDto("new", "https://youtu.be/new", "https://img/new");

        mockMvc.perform(post("/videos")
                        .header("Authorization", auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("new"));

        assertThat(videoRepository.findAll()).extracting(Video::getTitle).contains("new");
    }

    @Test
    void deleteVideo_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/videos/{id}", UUID.randomUUID()).header("Authorization", auth()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllVideos_emptiesTable() throws Exception {
        persistVideo("a");
        persistVideo("b");

        mockMvc.perform(delete("/videos").header("Authorization", auth()))
                .andExpect(status().isOk());

        assertThat(videoRepository.count()).isZero();
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/videos/all")).andExpect(status().is4xxClientError());
    }
}
