package iliev.yt.share.backend.device;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByUserPreferencesId(UUID userPreferencesId);
}
