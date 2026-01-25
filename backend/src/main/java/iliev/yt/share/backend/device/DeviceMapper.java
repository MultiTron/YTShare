package iliev.yt.share.backend.device;

import iliev.yt.share.backend.device.dto.DeviceInputDto;
import iliev.yt.share.backend.device.dto.DeviceOutputDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface DeviceMapper {
    @Mapping(target = "userPreferencesId", source = "userPreferences.id")
    DeviceOutputDto toOutputDto(final Device device);

    @Mapping(target = "userPreferences.id", source = "userPreferencesId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Device toEntity(final DeviceInputDto inputDto);

    @Mapping(target = "userPreferences", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(final DeviceInputDto inputDto, @MappingTarget final Device device);
}
