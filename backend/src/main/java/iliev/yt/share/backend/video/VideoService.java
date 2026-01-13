package iliev.yt.share.backend.video;

import iliev.yt.share.backend.video.dto.VideoInputDto;
import iliev.yt.share.backend.video.dto.VideoOutputDto;
import iliev.yt.share.backend.video.exception.VideoNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;

    public List<VideoOutputDto> getAllVideos() {
        return videoRepository.findAll().stream()
                .map(videoMapper::toOutputDto).toList();
    }

    public Page<VideoOutputDto> getAllVideos(final Pageable pageable) {
        return videoRepository.findAll(pageable)
                .map(videoMapper::toOutputDto);
    }

    public VideoOutputDto getVideoById(final UUID id) {
        final Video video = videoRepository.findById(id)
                .orElseThrow(() -> new VideoNotFoundException(id));
        return videoMapper.toOutputDto(video);
    }

    @Transactional
    public VideoOutputDto createVideo(final VideoInputDto inputDto) {
        final Video video = videoMapper.toEntity(inputDto);
        final Video savedVideo = videoRepository.save(video);
        return videoMapper.toOutputDto(savedVideo);
    }

    @Transactional
    public void deleteVideo(final UUID id) {
        if (!videoRepository.existsById(id)) {
            throw new VideoNotFoundException(id);
        }
        videoRepository.deleteById(id);
    }
}
