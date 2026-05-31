package iliev.yt.share.backend.devicetoken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    Optional<DeviceToken> findByFcmToken(String fcmToken);
    List<DeviceToken> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
