package iliev.yt.share.backend.devicetoken.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;
import java.util.UUID;
import static iliev.yt.share.backend.common.enums.ExceptionMessages.DEVICE_TOKEN_NOT_FOUND;

public class DeviceTokenNotFoundException extends GenericNotFoundException {
    public DeviceTokenNotFoundException(UUID id) {
        super(DEVICE_TOKEN_NOT_FOUND.getMessage(id));
    }
}
