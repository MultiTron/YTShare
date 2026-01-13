package iliev.yt.share.backend.friends.dto;

import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import iliev.yt.share.backend.user.dto.UserOutputDto;
import java.util.UUID;

public record FriendshipOutputDto(
        UUID id,
        UserOutputDto user,
        UserOutputDto friend,
        FriendshipStatus status
) {
}
