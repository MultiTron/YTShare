package iliev.yt.share.backend.user.dto;

import java.util.UUID;

public record UserOutputDto(
        UUID id,
        String firebaseUid,
        String email,
        String firstName,
        String lastName
) {
}
