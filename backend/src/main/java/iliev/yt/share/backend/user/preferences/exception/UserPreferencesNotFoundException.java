package iliev.yt.share.backend.user.preferences.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import java.util.UUID;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.USER_PREFERENCES_NOT_FOUND;

public class UserPreferencesNotFoundException extends GenericNotFoundException {
    public UserPreferencesNotFoundException(UUID id) {
        super(USER_PREFERENCES_NOT_FOUND.getMessage(id));
    }
}
