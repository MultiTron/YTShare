package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.devicetoken.dto.DeviceTokenOutputDto;
import org.mapstruct.Mapper;

@Mapper
public interface DeviceTokenMapper {
    DeviceTokenOutputDto toOutputDto(final DeviceToken deviceToken);
}
