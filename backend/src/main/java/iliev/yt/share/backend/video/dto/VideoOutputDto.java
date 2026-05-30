package iliev.yt.share.backend.video.dto;

import java.util.UUID;

public record VideoOutputDto(
        UUID id,
        String title,
        String url,
        String thumbnailUrl
) {
}
