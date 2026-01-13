package iliev.yt.share.backend.user.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.USER_NOT_FOUND_BY_EMAIL;

public class UserNotFoundByEmailException extends GenericNotFoundException {
    public UserNotFoundByEmailException(String text) {
        super(USER_NOT_FOUND_BY_EMAIL.getMessage(text));
    }
}
