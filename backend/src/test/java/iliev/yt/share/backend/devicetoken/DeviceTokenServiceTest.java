package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.devicetoken.dto.DeviceTokenInputDto;
import iliev.yt.share.backend.devicetoken.dto.DeviceTokenOutputDto;
import iliev.yt.share.backend.security.SecurityUtils;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import iliev.yt.share.backend.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private DeviceTokenMapper deviceTokenMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    private UUID userId;
    private User user;
    private DeviceTokenOutputDto outputDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().firebaseUid("uid-123").firstName("Alice").build();
        user.setId(userId);
        outputDto = new DeviceTokenOutputDto(UUID.randomUUID(), "fcm-1", "android");
    }

    @Test
    void registerToken_existingToken_updatesUserAndPlatform() {
        final DeviceTokenInputDto inputDto = new DeviceTokenInputDto("fcm-1", "ios");
        final DeviceToken existing = DeviceToken.builder().fcmToken("fcm-1").platform("android").build();

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserUid).thenReturn("uid-123");
            when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.of(user));
            when(deviceTokenRepository.findByFcmToken("fcm-1")).thenReturn(Optional.of(existing));
            when(deviceTokenRepository.save(existing)).thenReturn(existing);
            when(deviceTokenMapper.toOutputDto(existing)).thenReturn(outputDto);

            assertThat(deviceTokenService.registerToken(inputDto)).isEqualTo(outputDto);
            assertThat(existing.getUser()).isEqualTo(user);
            assertThat(existing.getPlatform()).isEqualTo("ios");
            verify(deviceTokenRepository).save(existing);
        }
    }

    @Test
    void registerToken_newToken_createsToken() {
        final DeviceTokenInputDto inputDto = new DeviceTokenInputDto("fcm-new", "android");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserUid).thenReturn("uid-123");
            when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.of(user));
            when(deviceTokenRepository.findByFcmToken("fcm-new")).thenReturn(Optional.empty());
            when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deviceTokenMapper.toOutputDto(any(DeviceToken.class))).thenReturn(outputDto);

            assertThat(deviceTokenService.registerToken(inputDto)).isEqualTo(outputDto);

            final ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
            verify(deviceTokenRepository).save(captor.capture());
            final DeviceToken saved = captor.getValue();
            assertThat(saved.getFcmToken()).isEqualTo("fcm-new");
            assertThat(saved.getPlatform()).isEqualTo("android");
            assertThat(saved.getUser()).isEqualTo(user);
        }
    }

    @Test
    void registerToken_userNotFound_throws() {
        final DeviceTokenInputDto inputDto = new DeviceTokenInputDto("fcm-1", "android");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserUid).thenReturn("uid-123");
            when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceTokenService.registerToken(inputDto))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Test
    void removeTokensForCurrentUser_found_deletesByUserId() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserUid).thenReturn("uid-123");
            when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.of(user));

            deviceTokenService.removeTokensForCurrentUser();

            verify(deviceTokenRepository).deleteByUserId(userId);
        }
    }

    @Test
    void removeTokensForCurrentUser_userNotFound_throws() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::requireCurrentUserUid).thenReturn("uid-123");
            when(userRepository.findByFirebaseUid("uid-123")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceTokenService.removeTokensForCurrentUser())
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Test
    void getTokensByUserIds_flattensTokensAcrossUsers() {
        final UUID userId2 = UUID.randomUUID();
        final DeviceToken t1 = DeviceToken.builder().fcmToken("a").build();
        final DeviceToken t2 = DeviceToken.builder().fcmToken("b").build();
        when(deviceTokenRepository.findByUserId(userId)).thenReturn(List.of(t1));
        when(deviceTokenRepository.findByUserId(userId2)).thenReturn(List.of(t2));

        assertThat(deviceTokenService.getTokensByUserIds(List.of(userId, userId2)))
                .containsExactly(t1, t2);
    }

    @Test
    void getTokensByUserIds_emptyList_returnsEmpty() {
        assertThat(deviceTokenService.getTokensByUserIds(List.of())).isEmpty();
    }
}
