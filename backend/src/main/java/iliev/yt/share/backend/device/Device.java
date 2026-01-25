package iliev.yt.share.backend.device;

import iliev.yt.share.backend.common.entity.BaseEntity;
import iliev.yt.share.backend.user.preferences.UserPreferences;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "devices")
public class Device extends BaseEntity {
    @Column(name = "host_name")
    private String hostName;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "port")
    private String port;

    @Column(name = "last_connected_to")
    private LocalDateTime lastConnectedTo;

    @ManyToOne
    private UserPreferences userPreferences;
}
