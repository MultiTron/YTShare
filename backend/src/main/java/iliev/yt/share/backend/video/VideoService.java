package iliev.yt.share.backend.video;

import iliev.yt.share.backend.video.dto.VideoInputDto;
import iliev.yt.share.backend.video.dto.VideoOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;

    public Page<VideoOutputDto> getAllVideos(final Pageable pageable) {
        return videoRepository.findAll(pageable)
                .map(videoMapper::toOutputDto);
    }

    public VideoOutputDto getVideoById(final UUID id) {
        final Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
        return videoMapper.toOutputDto(video);
    }

    public VideoOutputDto createVideo(final VideoInputDto inputDto) {
        final Video video = videoMapper.toEntity(inputDto);
        final Video savedVideo = videoRepository.save(video);
        return videoMapper.toOutputDto(savedVideo);
    }

    public void deleteVideo(final UUID id) {
        if (!videoRepository.existsById(id)) {
            throw new RuntimeException("Video not found with id: " + id);
        }
        videoRepository.deleteById(id);
    }
}
