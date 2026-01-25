package iliev.yt.share.backend.message;

import iliev.yt.share.backend.chat.ChatMapper;
import iliev.yt.share.backend.message.dto.MessageInputDto;
import iliev.yt.share.backend.message.dto.MessageOutputDto;
import iliev.yt.share.backend.user.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {UserMapper.class, ChatMapper.class})
public interface MessageMapper {
    MessageOutputDto toOutputDto(final Message message);

    @Mapping(target = "chat.id", source = "chatId")
    @Mapping(target = "sender.id", source = "senderId")
    Message toEntity(final MessageInputDto inputDto);
}
