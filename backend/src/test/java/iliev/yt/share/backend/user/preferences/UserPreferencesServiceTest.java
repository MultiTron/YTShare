package iliev.yt.share.backend.user.preferences;

import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.exception.UserNotFoundException;
import iliev.yt.share.backend.user.preferences.dto.UserPreferencesInputDto;
import iliev.yt.share.backend.user.preferences.dto.UserPreferencesOutputDto;
import iliev.yt.share.backend.user.preferences.exception.UserPreferencesNotFoundException;
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
class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserPreferencesMapper userPreferencesMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserPreferencesService userPreferencesService;

    private UUID id;
    private UUID userId;
    private UserPreferences preferences;
    private UserPreferencesOutputDto outputDto;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        userId = UUID.randomUUID();
        preferences = UserPreferences.builder().darkMode(true).notificationsEnabled(true).build();
        preferences.setId(id);
        outputDto = new UserPreferencesOutputDto(id, true, true, false, userId, List.of());
    }

    private UserPreferencesInputDto inputDto() {
        return new UserPreferencesInputDto(true, true, false, userId);
    }

    @Test
    void getAllUserPreferences_returnsMappedList() {
        when(userPreferencesRepository.findAll()).thenReturn(List.of(preferences));
        when(userPreferencesMapper.toOutputDto(preferences)).thenReturn(outputDto);

        assertThat(userPreferencesService.getAllUserPreferences()).containsExactly(outputDto);
    }

    @Test
    void getAllUserPreferences_paged_returnsMappedPage() {
        final Pageable pageable = PageRequest.of(0, 10);
        when(userPreferencesRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(preferences)));
        when(userPreferencesMapper.toOutputDto(preferences)).thenReturn(outputDto);

        final Page<UserPreferencesOutputDto> result = userPreferencesService.getAllUserPreferences(pageable);

        assertThat(result.getContent()).containsExactly(outputDto);
    }

    @Test
    void getUserPreferencesById_found_returnsDto() {
        when(userPreferencesRepository.findById(id)).thenReturn(Optional.of(preferences));
        when(userPreferencesMapper.toOutputDto(preferences)).thenReturn(outputDto);

        assertThat(userPreferencesService.getUserPreferencesById(id)).isEqualTo(outputDto);
    }

    @Test
    void getUserPreferencesById_notFound_throws() {
        when(userPreferencesRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPreferencesService.getUserPreferencesById(id))
                .isInstanceOf(UserPreferencesNotFoundException.class);
    }

    @Test
    void getUserPreferencesByUserId_found_returnsDto() {
        when(userPreferencesRepository.findByUserId(userId)).thenReturn(Optional.of(preferences));
        when(userPreferencesMapper.toOutputDto(preferences)).thenReturn(outputDto);

        assertThat(userPreferencesService.getUserPreferencesByUserId(userId)).isEqualTo(outputDto);
    }

    @Test
    void getUserPreferencesByUserId_notFound_throws() {
        when(userPreferencesRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPreferencesService.getUserPreferencesByUserId(userId))
                .isInstanceOf(UserPreferencesNotFoundException.class);
    }

    @Test
    void createUserPreferences_userFound_setsUserSavesAndReturnsDto() {
        final UserPreferencesInputDto inputDto = inputDto();
        final User user = User.builder().build();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userPreferencesMapper.toEntity(inputDto)).thenReturn(preferences);
        when(userPreferencesRepository.save(preferences)).thenReturn(preferences);
        when(userPreferencesMapper.toOutputDto(preferences)).thenReturn(outputDto);

        assertThat(userPreferencesService.createUserPreferences(inputDto)).isEqualTo(outputDto);
        assertThat(preferences.getUser()).isEqualTo(user);
        verify(userPreferencesRepository).save(preferences);
    }

    @Test
    void createUserPreferences_userNotFound_throwsAndDoesNotSave() {
        final UserPreferencesInputDto inputDto = inputDto();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPreferencesService.createUserPreferences(inputDto))
                .isInstanceOf(UserNotFoundException.class);
        verify(userPreferencesRepository, never()).save(preferences);
    }

    @Test
    void updateUserPreferences_found_updatesSavesAndReturnsDto() {
        final UserPreferencesInputDto inputDto = inputDto();
        when(userPreferencesRepository.findById(id)).thenReturn(Optional.of(preferences));
        when(userPreferencesRepository.save(preferences)).thenReturn(preferences);
        when(userPreferencesMapper.toOutputDto(preferences)).thenReturn(outputDto);

        assertThat(userPreferencesService.updateUserPreferences(id, inputDto)).isEqualTo(outputDto);
        verify(userPreferencesMapper).updateEntity(inputDto, preferences);
        verify(userPreferencesRepository).save(preferences);
    }

    @Test
    void updateUserPreferences_notFound_throws() {
        final UserPreferencesInputDto inputDto = inputDto();
        when(userPreferencesRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPreferencesService.updateUserPreferences(id, inputDto))
                .isInstanceOf(UserPreferencesNotFoundException.class);
    }

    @Test
    void deleteUserPreferences_exists_deletes() {
        when(userPreferencesRepository.existsById(id)).thenReturn(true);

        userPreferencesService.deleteUserPreferences(id);

        verify(userPreferencesRepository).deleteById(id);
    }

    @Test
    void deleteUserPreferences_notExists_throwsAndDoesNotDelete() {
        when(userPreferencesRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userPreferencesService.deleteUserPreferences(id))
                .isInstanceOf(UserPreferencesNotFoundException.class);
        verify(userPreferencesRepository, never()).deleteById(id);
    }
}
