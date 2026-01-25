package iliev.yt.share.backend.device.exception;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;

import java.util.UUID;

import static iliev.yt.share.backend.common.enums.ExceptionMessages.DEVICE_NOT_FOUND;

public class DeviceNotFoundException extends GenericNotFoundException {
    public DeviceNotFoundException(UUID id) {
        super(DEVICE_NOT_FOUND.getMessage(id));
    }
}
