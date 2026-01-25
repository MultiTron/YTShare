package iliev.yt.share.backend.user.preferences.dto;

import iliev.yt.share.backend.device.dto.DeviceOutputDto;
import java.util.List;
import java.util.UUID;

public record UserPreferencesOutputDto(
        UUID id,
        boolean darkMode,
        boolean notificationsEnabled,
        boolean trackingEnabled,
        UUID userId,
        List<DeviceOutputDto> devices
) {
}
