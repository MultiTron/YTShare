package iliev.yt.share.backend.friends.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import java.util.UUID;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.FRIENDSHIP_NOT_FOUND;

public class FriendshipNotFoundException extends GenericNotFoundException {
    public FriendshipNotFoundException(UUID id) {
        super(FRIENDSHIP_NOT_FOUND.getMessage(id));
    }
}
