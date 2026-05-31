package iliev.yt.share.backend.devicetoken.dto;

import java.util.UUID;

public record DeviceTokenOutputDto(
        UUID id,
        String fcmToken,
        String platform
) {
}
