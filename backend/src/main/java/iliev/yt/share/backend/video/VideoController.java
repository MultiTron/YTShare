package iliev.yt.share.backend.video;

import iliev.yt.share.backend.video.dto.VideoInputDto;
import iliev.yt.share.backend.video.dto.VideoOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    @GetMapping
    public Page<VideoOutputDto> getAllVideos(final Pageable pageable) {
        return videoService.getAllVideos(pageable);
    }

    @GetMapping("/all")
    public List<VideoOutputDto> getAllVideos() {
        return videoService.getAllVideos();
    }

    @GetMapping("/{id}")
    public VideoOutputDto getVideoById(@PathVariable final UUID id) {
        return videoService.getVideoById(id);
    }

    @PostMapping
    public VideoOutputDto createVideo(@RequestBody final VideoInputDto inputDto) {
        return videoService.createVideo(inputDto);
    }

    @DeleteMapping("/{id}")
    public void deleteVideo(@PathVariable final UUID id) {
        videoService.deleteVideo(id);
    }

    @DeleteMapping
    public void deleteAllVideos() {
        videoService.deleteAllVideos();
    }
}
