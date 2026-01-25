package iliev.yt.share.backend.friends;

import iliev.yt.share.backend.friends.dto.FriendshipInputDto;
import iliev.yt.share.backend.friends.dto.FriendshipOutputDto;
import iliev.yt.share.backend.friends.dto.FriendshipStatusUpdateDto;
import iliev.yt.share.backend.user.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = {UserMapper.class})
public interface FriendshipMapper {
    FriendshipOutputDto toOutputDto(final Friendship friendship);

    @Mapping(target = "user.id", source = "userId")
    @Mapping(target = "friend.id", source = "friendId")
    Friendship toEntity(final FriendshipInputDto inputDto);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "friend", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateStatus(final FriendshipStatusUpdateDto updateDto, @MappingTarget final Friendship friendship);
}
