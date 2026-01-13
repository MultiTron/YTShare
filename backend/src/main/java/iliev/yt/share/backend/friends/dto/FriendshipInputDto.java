package iliev.yt.share.backend.friends.dto;

import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import java.util.UUID;

public record FriendshipInputDto(
        UUID userId,
        UUID friendId,
        FriendshipStatus status
) {
}
