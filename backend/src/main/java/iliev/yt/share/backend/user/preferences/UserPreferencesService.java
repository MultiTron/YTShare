package iliev.yt.share.backend.user.preferences;

import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.exception.UserNotFoundException;
import iliev.yt.share.backend.user.preferences.dto.UserPreferencesInputDto;
import iliev.yt.share.backend.user.preferences.dto.UserPreferencesOutputDto;
import iliev.yt.share.backend.user.preferences.exception.UserPreferencesNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserPreferencesMapper userPreferencesMapper;
    private final UserRepository userRepository;

    public List<UserPreferencesOutputDto> getAllUserPreferences() {
        return userPreferencesRepository.findAll().stream()
                .map(userPreferencesMapper::toOutputDto).toList();
    }

    public Page<UserPreferencesOutputDto> getAllUserPreferences(final Pageable pageable) {
        return userPreferencesRepository.findAll(pageable)
                .map(userPreferencesMapper::toOutputDto);
    }

    public UserPreferencesOutputDto getUserPreferencesById(final UUID id) {
        final UserPreferences userPreferences = userPreferencesRepository.findById(id)
                .orElseThrow(() -> new UserPreferencesNotFoundException(id));
        return userPreferencesMapper.toOutputDto(userPreferences);
    }

    public UserPreferencesOutputDto getUserPreferencesByUserId(final UUID userId) {
        final UserPreferences userPreferences = userPreferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new UserPreferencesNotFoundException(userId));
        return userPreferencesMapper.toOutputDto(userPreferences);
    }

    @Transactional
    public UserPreferencesOutputDto createUserPreferences(final UserPreferencesInputDto inputDto) {
        final User user = userRepository.findById(inputDto.userId())
                .orElseThrow(() -> new UserNotFoundException(inputDto.userId()));

        final UserPreferences userPreferences = userPreferencesMapper.toEntity(inputDto);
        userPreferences.setUser(user);

        final UserPreferences savedUserPreferences = userPreferencesRepository.save(userPreferences);
        return userPreferencesMapper.toOutputDto(savedUserPreferences);
    }

    @Transactional
    public UserPreferencesOutputDto updateUserPreferences(final UUID id, final UserPreferencesInputDto inputDto) {
        final UserPreferences existingPreferences = userPreferencesRepository.findById(id)
                .orElseThrow(() -> new UserPreferencesNotFoundException(id));

        userPreferencesMapper.updateEntity(inputDto, existingPreferences);

        final UserPreferences updatedPreferences = userPreferencesRepository.save(existingPreferences);
        return userPreferencesMapper.toOutputDto(updatedPreferences);
    }

    @Transactional
    public void deleteUserPreferences(final UUID id) {
        if (!userPreferencesRepository.existsById(id)) {
            throw new UserPreferencesNotFoundException(id);
        }
        userPreferencesRepository.deleteById(id);
    }
}
