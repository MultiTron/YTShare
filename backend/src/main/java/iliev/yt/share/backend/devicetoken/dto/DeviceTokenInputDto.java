package iliev.yt.share.backend.devicetoken.dto;

public record DeviceTokenInputDto(
        String fcmToken,
        String platform
) {
}
