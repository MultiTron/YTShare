package iliev.yt.share.backend.friends.dto;

import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record FriendshipStatusUpdateDto(UUID id, FriendshipStatus status) {
}
