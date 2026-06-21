package iliev.yt.share.backend.video;

import iliev.yt.share.backend.video.dto.VideoInputDto;
import iliev.yt.share.backend.video.dto.VideoOutputDto;
import iliev.yt.share.backend.video.exception.VideoNotFoundException;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoMapper videoMapper;

    @InjectMocks
    private VideoService videoService;

    private UUID id;
    private Video video;
    private VideoOutputDto outputDto;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        video = Video.builder()
                .title("Title")
                .url("https://youtu.be/abc")
                .thumbnailUrl("https://img/abc.jpg")
                .build();
        video.setId(id);
        outputDto = new VideoOutputDto(id, "Title", "https://youtu.be/abc", "https://img/abc.jpg");
    }

    @Test
    void getAllVideos_returnsMappedList() {
        when(videoRepository.findAll()).thenReturn(List.of(video));
        when(videoMapper.toOutputDto(video)).thenReturn(outputDto);

        assertThat(videoService.getAllVideos()).containsExactly(outputDto);
    }

    @Test
    void getAllVideos_paged_returnsMappedPage() {
        final Pageable pageable = PageRequest.of(0, 5);
        when(videoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(video)));
        when(videoMapper.toOutputDto(video)).thenReturn(outputDto);

        final Page<VideoOutputDto> result = videoService.getAllVideos(pageable);

        assertThat(result.getContent()).containsExactly(outputDto);
    }

    @Test
    void getVideoById_found_returnsDto() {
        when(videoRepository.findById(id)).thenReturn(Optional.of(video));
        when(videoMapper.toOutputDto(video)).thenReturn(outputDto);

        assertThat(videoService.getVideoById(id)).isEqualTo(outputDto);
    }

    @Test
    void getVideoById_notFound_throws() {
        when(videoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoService.getVideoById(id))
                .isInstanceOf(VideoNotFoundException.class);
    }

    @Test
    void createVideo_savesAndReturnsDto() {
        final VideoInputDto inputDto = new VideoInputDto("Title", "https://youtu.be/abc", "https://img/abc.jpg");
        when(videoMapper.toEntity(inputDto)).thenReturn(video);
        when(videoRepository.save(video)).thenReturn(video);
        when(videoMapper.toOutputDto(video)).thenReturn(outputDto);

        assertThat(videoService.createVideo(inputDto)).isEqualTo(outputDto);
        verify(videoRepository).save(video);
    }

    @Test
    void deleteVideo_exists_deletes() {
        when(videoRepository.existsById(id)).thenReturn(true);

        videoService.deleteVideo(id);

        verify(videoRepository).deleteById(id);
    }

    @Test
    void deleteVideo_notExists_throwsAndDoesNotDelete() {
        when(videoRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> videoService.deleteVideo(id))
                .isInstanceOf(VideoNotFoundException.class);
        verify(videoRepository, never()).deleteById(id);
    }

    @Test
    void deleteAllVideos_delegatesToRepository() {
        videoService.deleteAllVideos();

        verify(videoRepository).deleteAll();
    }
}
