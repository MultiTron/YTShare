package iliev.yt.share.backend.message.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import java.util.UUID;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.MESSAGE_NOT_FOUND;

public class MessageNotFoundException extends GenericNotFoundException {
    public MessageNotFoundException(UUID id) {
        super(MESSAGE_NOT_FOUND.getMessage(id));
    }
}
