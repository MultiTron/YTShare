package iliev.yt.share.backend.user;

import iliev.yt.share.backend.user.dto.UserInputDto;
import iliev.yt.share.backend.user.dto.UserOutputDto;
import iliev.yt.share.backend.user.exception.UserNotFoundByEmailException;
import iliev.yt.share.backend.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserOutputDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toOutputDto).toList();
    }

    public Page<UserOutputDto> getAllUsers(final Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toOutputDto);
    }

    public UserOutputDto getUserById(final UUID id) {
        final User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return userMapper.toOutputDto(user);
    }

    public UserOutputDto getUserByFirebaseUid(final String firebaseUid) {
        final User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new UserNotFoundException(firebaseUid));

        return userMapper.toOutputDto(user);
    }

    public UserOutputDto getUserByEmail(final String email) {
        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundByEmailException(email));

        return userMapper.toOutputDto(user);
    }

    @Transactional
    public UserOutputDto createUser(final UserInputDto inputDto) {
        final User user = userMapper.toEntity(inputDto);

        final User savedUser = userRepository.save(user);

        return userMapper.toOutputDto(savedUser);
    }

    @Transactional
    public void deleteUser(final UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }
}
