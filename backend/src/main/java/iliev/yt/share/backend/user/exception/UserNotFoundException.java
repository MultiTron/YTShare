package iliev.yt.share.backend.user.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import java.util.UUID;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.FIREBASE_USER_NOT_FOUND;
import static iliev.yt.share.backend.common.enums.ExceptionMessages.USER_NOT_FOUND;

public class UserNotFoundException extends GenericNotFoundException {
    public UserNotFoundException(UUID id) {
        super(USER_NOT_FOUND.getMessage(id));
    }

    public UserNotFoundException(String text) {
        super(FIREBASE_USER_NOT_FOUND.getMessage(text));
    }
}

