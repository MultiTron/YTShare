package iliev.yt.share.backend.user.dto;

import iliev.yt.share.backend.user.preferences.dto.UserPreferencesOutputDto;
import java.util.UUID;

public record UserOutputDto(
        UUID id,
        String firebaseUid,
        String email,
        String firstName,
        String lastName,
        UserPreferencesOutputDto userPreferences
) {
}
