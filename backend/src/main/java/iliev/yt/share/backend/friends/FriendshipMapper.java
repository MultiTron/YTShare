package iliev.yt.share.backend.friends;

import iliev.yt.share.backend.friends.dto.FriendshipInputDto;
import iliev.yt.share.backend.friends.dto.FriendshipOutputDto;
import iliev.yt.share.backend.user.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserMapper.class})
public interface FriendshipMapper {
    FriendshipOutputDto toOutputDto(final Friendship friendship);

    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "friend.id", source = "friendId")
    Friendship toEntity(final FriendshipInputDto inputDto);
}
