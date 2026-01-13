package iliev.yt.share.backend.user.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import java.util.UUID;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.FRIEND_NOT_FOUND;

public class FriendNotFoundException extends GenericNotFoundException {
    public FriendNotFoundException(UUID id) {
        super(FRIEND_NOT_FOUND.getMessage(id));
    }
}
