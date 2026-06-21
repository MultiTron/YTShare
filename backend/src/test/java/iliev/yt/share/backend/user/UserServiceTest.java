package iliev.yt.share.backend.user;

import iliev.yt.share.backend.security.SecurityUtils;
import iliev.yt.share.backend.user.dto.UserInputDto;
import iliev.yt.share.backend.user.dto.UserOutputDto;
import iliev.yt.share.backend.user.exception.UserNotFoundByEmailException;
import iliev.yt.share.backend.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private UUID id;
    private User user;
    private UserOutputDto outputDto;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        user = User.builder()
                .firebaseUid("uid-123")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
        user.setId(id);
        outputDto = new UserOutputDto(id, "uid-123", "john@example.com", "John", "Doe", null);
    }

    @Test
    void getAllUsers_returnsMappedList() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toOutputDto(user)).thenReturn(outputDto);

        final List<UserOutputDto> result = userService.getAllUsers();

        assertThat(result).containsExactly(outputDto);
    }

    @Test
    void getAllUsers_paged_returnsMappedPage() {
        final Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toOutputDto(user)).thenReturn(outputDto);

        final Page<UserOutputDto> result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).containsExactly(outputDto);
    }

    @Test
    void getUserById_found_returnsDto() {
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toOutputDto(user)).thenReturn(outputDto);

        assertThat(userService.getUserById(id)).isEqualTo(outputDto);
    }

    @Test
    void getUserById_notFound_throws() {
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUserByFirebaseUid_found_returnsDto() {
        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.of(user));
        when(userMapper.toOutputDto(user)).thenReturn(outputDto);

        assertThat(userService.getUserByFirebaseUid("uid-123")).isEqualTo(outputDto);
    }

    @Test
    void getUserByFirebaseUid_notFound_throws() {
        when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByFirebaseUid("uid-123"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUserByEmail_found_returnsDto() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toOutputDto(user)).thenReturn(outputDto);

        assertThat(userService.getUserByEmail("john@example.com")).isEqualTo(outputDto);
    }

    @Test
    void getUserByEmail_notFound_throws() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("john@example.com"))
                .isInstanceOf(UserNotFoundByEmailException.class);
    }

    @Test
    void getCurrentUser_found_returnsDto() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserUid).thenReturn("uid-123");
            when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.of(user));
            when(userMapper.toOutputDto(user)).thenReturn(outputDto);

            assertThat(userService.getCurrentUser()).isEqualTo(outputDto);
        }
    }

    @Test
    void getCurrentUser_notFound_throws() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserUid).thenReturn("uid-123");
            when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getCurrentUser())
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Test
    void createUser_savesAndReturnsDto() {
        final UserInputDto inputDto = new UserInputDto("uid-123", "john@example.com", "John", "Doe");
        when(userMapper.toEntity(inputDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toOutputDto(user)).thenReturn(outputDto);

        assertThat(userService.createUser(inputDto)).isEqualTo(outputDto);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_exists_deletes() {
        when(userRepository.existsById(id)).thenReturn(true);

        userService.deleteUser(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void deleteUser_notExists_throwsAndDoesNotDelete() {
        when(userRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(id))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).deleteById(id);
    }
}
