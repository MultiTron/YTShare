package iliev.yt.share.backend.user;

import iliev.yt.share.backend.user.dto.UserInputDto;
import iliev.yt.share.backend.user.dto.UserOutputDto;
import iliev.yt.share.backend.user.preferences.UserPreferencesMapper;
import org.mapstruct.Mapper;

@Mapper(uses = {UserPreferencesMapper.class})
public interface UserMapper {
    UserOutputDto toOutputDto(final User user);

    User toEntity(final UserInputDto inputDto);
}
