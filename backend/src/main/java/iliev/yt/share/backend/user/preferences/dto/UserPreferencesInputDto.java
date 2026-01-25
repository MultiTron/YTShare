package iliev.yt.share.backend.user.preferences.dto;

import java.util.UUID;

public record UserPreferencesInputDto(
        boolean darkMode,
        boolean notificationsEnabled,
        boolean trackingEnabled,
        UUID userId
) {
}
