package iliev.yt.share.backend.user;

import iliev.yt.share.backend.user.dto.UserInputDto;
import iliev.yt.share.backend.user.dto.UserOutputDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserOutputDto toOutputDto(final User user);

    User toEntity(final UserInputDto inputDto);
}
