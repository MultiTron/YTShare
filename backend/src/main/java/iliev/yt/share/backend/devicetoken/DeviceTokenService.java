package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.devicetoken.dto.DeviceTokenInputDto;
import iliev.yt.share.backend.devicetoken.dto.DeviceTokenOutputDto;
import iliev.yt.share.backend.security.SecurityUtils;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {
    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenMapper deviceTokenMapper;
    private final UserRepository userRepository;

    @Transactional
    public DeviceTokenOutputDto registerToken(final DeviceTokenInputDto inputDto) {
        final String firebaseUid = SecurityUtils.requireCurrentUserUid();
        final User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new UserNotFoundException(firebaseUid));

        final DeviceToken deviceToken = deviceTokenRepository.findByFcmToken(inputDto.fcmToken())
                .map(existing -> {
                    existing.setUser(user);
                    existing.setPlatform(inputDto.platform());
                    return existing;
                })
                .orElseGet(() -> DeviceToken.builder()
                        .fcmToken(inputDto.fcmToken())
                        .platform(inputDto.platform())
                        .user(user)
                        .build());

        final DeviceToken saved = deviceTokenRepository.save(deviceToken);
        return deviceTokenMapper.toOutputDto(saved);
    }

    @Transactional
    public void removeTokensForCurrentUser() {
        final String firebaseUid = SecurityUtils.requireCurrentUserUid();
        final User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new UserNotFoundException(firebaseUid));
        deviceTokenRepository.deleteByUserId(user.getId());
    }

    public List<DeviceToken> getTokensByUserIds(final List<UUID> userIds) {
        return userIds.stream()
                .flatMap(id -> deviceTokenRepository.findByUserId(id).stream())
                .toList();
    }
}
