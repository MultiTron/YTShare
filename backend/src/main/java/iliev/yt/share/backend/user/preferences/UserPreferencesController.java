package iliev.yt.share.backend.user.preferences;

import iliev.yt.share.backend.user.preferences.dto.UserPreferencesInputDto;
import iliev.yt.share.backend.user.preferences.dto.UserPreferencesOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user-preferences")
@RequiredArgsConstructor
public class UserPreferencesController {
    private final UserPreferencesService userPreferencesService;

    @GetMapping
    public Page<UserPreferencesOutputDto> getAllUserPreferences(final Pageable pageable) {
        return userPreferencesService.getAllUserPreferences(pageable);
    }

    @GetMapping("/all")
    public List<UserPreferencesOutputDto> getAllUserPreferences() {
        return userPreferencesService.getAllUserPreferences();
    }

    @GetMapping("/{id}")
    public UserPreferencesOutputDto getUserPreferencesById(@PathVariable final UUID id) {
        return userPreferencesService.getUserPreferencesById(id);
    }

    @GetMapping("/user/{userId}")
    public UserPreferencesOutputDto getUserPreferencesByUserId(@PathVariable final UUID userId) {
        return userPreferencesService.getUserPreferencesByUserId(userId);
    }

    @PostMapping
    public UserPreferencesOutputDto createUserPreferences(@RequestBody final UserPreferencesInputDto inputDto) {
        return userPreferencesService.createUserPreferences(inputDto);
    }

    @PutMapping("/{id}")
    public UserPreferencesOutputDto updateUserPreferences(
            @PathVariable final UUID id,
            @RequestBody final UserPreferencesInputDto inputDto) {
        return userPreferencesService.updateUserPreferences(id, inputDto);
    }

    @DeleteMapping("/{id}")
    public void deleteUserPreferences(@PathVariable final UUID id) {
        userPreferencesService.deleteUserPreferences(id);
    }
}
