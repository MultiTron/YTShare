package iliev.yt.share.backend.user;

import iliev.yt.share.backend.user.dto.UserInputDto;
import iliev.yt.share.backend.user.dto.UserOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public Page<UserOutputDto> getAllUsers(final Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    @GetMapping("/all")
    public List<UserOutputDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserOutputDto getUserById(@PathVariable final UUID id) {
        return userService.getUserById(id);
    }

    @GetMapping("/by-firebase-uid")
    public UserOutputDto getUserByFirebaseUid(@RequestParam final String firebaseUid) {
        return userService.getUserByFirebaseUid(firebaseUid);
    }

    @GetMapping("/by-email")
    public UserOutputDto getUserByEmail(@RequestParam final String email) {
        return userService.getUserByEmail(email);
    }

    @PostMapping
    public UserOutputDto createUser(@RequestBody final UserInputDto inputDto) {
        return userService.createUser(inputDto);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable final UUID id) {
        userService.deleteUser(id);
    }
}
