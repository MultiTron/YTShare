package iliev.yt.share.backend.user.preferences;

import iliev.yt.share.backend.device.DeviceMapper;
import iliev.yt.share.backend.user.preferences.dto.UserPreferencesInputDto;
import iliev.yt.share.backend.user.preferences.dto.UserPreferencesOutputDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = {DeviceMapper.class})
public interface UserPreferencesMapper {
    @Mapping(target = "userId", source = "user.id")
    UserPreferencesOutputDto toOutputDto(final UserPreferences userPreferences);

    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "devices", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserPreferences toEntity(final UserPreferencesInputDto inputDto);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "devices", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(final UserPreferencesInputDto inputDto, @MappingTarget final UserPreferences userPreferences);
}
