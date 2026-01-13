package iliev.yt.share.backend.video.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import java.util.UUID;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.VIDEO_NOT_FOUND;

public class VideoNotFoundException extends GenericNotFoundException {
    public VideoNotFoundException(UUID id) {
        super(VIDEO_NOT_FOUND.getMessage(id));
    }
}
