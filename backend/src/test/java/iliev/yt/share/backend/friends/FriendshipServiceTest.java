package iliev.yt.share.backend.friends;

import iliev.yt.share.backend.friends.dto.FriendshipInputDto;
import iliev.yt.share.backend.friends.dto.FriendshipOutputDto;
import iliev.yt.share.backend.friends.dto.FriendshipStatusUpdateDto;
import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import iliev.yt.share.backend.friends.exception.FriendshipNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendshipMapper friendshipMapper;

    @InjectMocks
    private FriendshipService friendshipService;

    private UUID id;
    private Friendship friendship;
    private FriendshipOutputDto outputDto;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        friendship = Friendship.builder().status(FriendshipStatus.PENDING).build();
        friendship.setId(id);
        outputDto = new FriendshipOutputDto(id, null, null, FriendshipStatus.PENDING);
    }

    @Test
    void getAllFriendships_returnsMappedList() {
        when(friendshipRepository.findAll()).thenReturn(List.of(friendship));
        when(friendshipMapper.toOutputDto(friendship)).thenReturn(outputDto);

        assertThat(friendshipService.getAllFriendships()).containsExactly(outputDto);
    }

    @Test
    void getAllFriendships_paged_returnsMappedPage() {
        final Pageable pageable = PageRequest.of(0, 10);
        when(friendshipRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(friendship)));
        when(friendshipMapper.toOutputDto(friendship)).thenReturn(outputDto);

        final Page<FriendshipOutputDto> result = friendshipService.getAllFriendships(pageable);

        assertThat(result.getContent()).containsExactly(outputDto);
    }

    @Test
    void getFriendshipById_found_returnsDto() {
        when(friendshipRepository.findById(id)).thenReturn(Optional.of(friendship));
        when(friendshipMapper.toOutputDto(friendship)).thenReturn(outputDto);

        assertThat(friendshipService.getFriendshipById(id)).isEqualTo(outputDto);
    }

    @Test
    void getFriendshipById_notFound_throws() {
        when(friendshipRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.getFriendshipById(id))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    void getFriendshipsByUserId_returnsMappedList() {
        final UUID userId = UUID.randomUUID();
        when(friendshipRepository.findByUserId(userId)).thenReturn(List.of(friendship));
        when(friendshipMapper.toOutputDto(friendship)).thenReturn(outputDto);

        assertThat(friendshipService.getFriendshipsByUserId(userId)).containsExactly(outputDto);
    }

    @Test
    void getFriendshipsByUserIdAndStatus_returnsMappedList() {
        final UUID userId = UUID.randomUUID();
        when(friendshipRepository.findByUserIdOrFriendIdAndStatus(userId, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of(friendship));
        when(friendshipMapper.toOutputDto(friendship)).thenReturn(outputDto);

        assertThat(friendshipService.getFriendshipsByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED))
                .containsExactly(outputDto);
    }

    @Test
    void createFriendship_savesAndReturnsDto() {
        final FriendshipInputDto inputDto =
                new FriendshipInputDto(UUID.randomUUID(), UUID.randomUUID(), FriendshipStatus.PENDING);
        when(friendshipMapper.toEntity(inputDto)).thenReturn(friendship);
        when(friendshipRepository.save(friendship)).thenReturn(friendship);
        when(friendshipMapper.toOutputDto(friendship)).thenReturn(outputDto);

        assertThat(friendshipService.createFriendship(inputDto)).isEqualTo(outputDto);
        verify(friendshipRepository).save(friendship);
    }

    @Test
    void updateFriendshipStatus_found_updatesAndReturnsDto() {
        final FriendshipStatusUpdateDto updateDto =
                FriendshipStatusUpdateDto.builder().id(id).status(FriendshipStatus.ACCEPTED).build();
        when(friendshipRepository.findById(id)).thenReturn(Optional.of(friendship));
        when(friendshipMapper.toOutputDto(friendship)).thenReturn(outputDto);

        assertThat(friendshipService.updateFriendshipStatus(updateDto)).isEqualTo(outputDto);
        verify(friendshipMapper).updateStatus(updateDto, friendship);
    }

    @Test
    void updateFriendshipStatus_notFound_throws() {
        final FriendshipStatusUpdateDto updateDto =
                FriendshipStatusUpdateDto.builder().id(id).status(FriendshipStatus.ACCEPTED).build();
        when(friendshipRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.updateFriendshipStatus(updateDto))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    void deleteFriendship_exists_deletes() {
        when(friendshipRepository.existsById(id)).thenReturn(true);

        friendshipService.deleteFriendship(id);

        verify(friendshipRepository).deleteById(id);
    }

    @Test
    void deleteFriendship_notExists_throwsAndDoesNotDelete() {
        when(friendshipRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> friendshipService.deleteFriendship(id))
                .isInstanceOf(FriendshipNotFoundException.class);
        verify(friendshipRepository, never()).deleteById(id);
    }
}
