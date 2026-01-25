package iliev.yt.share.backend.device.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceOutputDto(
        UUID id,
        String hostName,
        String ipAddress,
        String port,
        LocalDateTime lastConnectedTo,
        UUID userPreferencesId
) {
}
