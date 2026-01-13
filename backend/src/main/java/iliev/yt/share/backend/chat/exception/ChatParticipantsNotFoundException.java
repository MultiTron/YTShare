package iliev.yt.share.backend.chat.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.CHAT_PARTICIPANTS_NOT_FOUND;

public class ChatParticipantsNotFoundException extends GenericNotFoundException {
    public ChatParticipantsNotFoundException() {
        super(CHAT_PARTICIPANTS_NOT_FOUND.getMessage());
    }
}
