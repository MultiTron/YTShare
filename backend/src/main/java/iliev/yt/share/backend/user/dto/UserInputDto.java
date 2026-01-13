package iliev.yt.share.backend.user.dto;

public record UserInputDto(
        String firebaseUid,
        String email,
        String firstName,
        String lastName
) {
}
