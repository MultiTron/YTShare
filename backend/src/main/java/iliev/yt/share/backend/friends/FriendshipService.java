package iliev.yt.share.backend.friends;

import iliev.yt.share.backend.friends.dto.FriendshipInputDto;
import iliev.yt.share.backend.friends.dto.FriendshipOutputDto;
import iliev.yt.share.backend.friends.dto.FriendshipStatusUpdateDto;
import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import iliev.yt.share.backend.friends.exception.FriendshipNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final FriendshipMapper friendshipMapper;

    public List<FriendshipOutputDto> getAllFriendships() {
        return friendshipRepository.findAll().stream()
                .map(friendshipMapper::toOutputDto).toList();
    }

    public Page<FriendshipOutputDto> getAllFriendships(final Pageable pageable) {
        return friendshipRepository.findAll(pageable)
                .map(friendshipMapper::toOutputDto);
    }

    public FriendshipOutputDto getFriendshipById(final UUID id) {
        final Friendship friendship = friendshipRepository.findById(id)
                .orElseThrow(() -> new FriendshipNotFoundException(id));

        return friendshipMapper.toOutputDto(friendship);
    }

    public List<FriendshipOutputDto> getFriendshipsByUserId(final UUID userId) {
        return friendshipRepository.findByUserId(userId).stream()
                .map(friendshipMapper::toOutputDto).toList();
    }

    public List<FriendshipOutputDto> getFriendshipsByUserIdAndStatus(final UUID userId, final FriendshipStatus status) {
        return friendshipRepository.findByUserIdOrFriendIdAndStatus(userId, status).stream()
                .map(friendshipMapper::toOutputDto).toList();
    }

    @Transactional
    public FriendshipOutputDto createFriendship(final FriendshipInputDto inputDto) {
        final var friendship = friendshipMapper.toEntity(inputDto);

        return friendshipMapper.toOutputDto(friendshipRepository.save(friendship));
    }

    @Transactional
    public FriendshipOutputDto updateFriendshipStatus(final FriendshipStatusUpdateDto updateDto) {
        final Friendship friendship = friendshipRepository.findById(updateDto.id())
                .orElseThrow(() -> new FriendshipNotFoundException(updateDto.id()));

        friendshipMapper.updateStatus(updateDto, friendship);

        return friendshipMapper.toOutputDto(friendship);
    }

    @Transactional
    public void deleteFriendship(final UUID id) {
        if (!friendshipRepository.existsById(id)) {
            throw new FriendshipNotFoundException(id);
        }
        friendshipRepository.deleteById(id);
    }
}
