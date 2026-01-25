package iliev.yt.share.backend.device.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceInputDto(
        String hostName,
        String ipAddress,
        String port,
        LocalDateTime lastConnectedTo,
        UUID userPreferencesId
) {
}
