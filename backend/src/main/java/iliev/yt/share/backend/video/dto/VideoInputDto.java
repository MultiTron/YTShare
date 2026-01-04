package iliev.yt.share.backend.video.dto;

public record VideoInputDto(
        String title,
        String description,
        String url,
        String thumbnailUrl
) {
}
