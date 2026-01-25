package iliev.yt.share.backend.video;

import iliev.yt.share.backend.video.dto.VideoInputDto;
import iliev.yt.share.backend.video.dto.VideoOutputDto;
import org.mapstruct.Mapper;

@Mapper
public interface VideoMapper {
    VideoOutputDto toOutputDto(final Video video);

    Video toEntity(final VideoInputDto inputDto);
}
