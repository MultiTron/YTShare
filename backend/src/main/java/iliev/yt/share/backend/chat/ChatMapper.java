package iliev.yt.share.backend.chat;

import iliev.yt.share.backend.chat.dto.ChatInputDto;
import iliev.yt.share.backend.chat.dto.ChatOutputDto;
import iliev.yt.share.backend.user.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class})
public interface ChatMapper {
    @Mapping(target = "participants", source = "participants")
    ChatOutputDto toOutputDto(final Chat chat);

    @Mapping(target = "participants", ignore = true)
    Chat toEntity(final ChatInputDto inputDto);
}
