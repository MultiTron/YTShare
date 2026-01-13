package iliev.yt.share.backend.chat.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import java.util.UUID;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.CHAT_NOT_FOUND;

public class ChatNotFoundException extends GenericNotFoundException {
    public ChatNotFoundException(UUID id) {
        super(CHAT_NOT_FOUND.getMessage(id));
    }
}
