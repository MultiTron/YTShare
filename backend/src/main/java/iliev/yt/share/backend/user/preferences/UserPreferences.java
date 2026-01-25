package iliev.yt.share.backend.user.preferences;

import iliev.yt.share.backend.common.entity.BaseEntity;
import iliev.yt.share.backend.device.Device;
import iliev.yt.share.backend.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "user_prefs")
public class UserPreferences extends BaseEntity {
    @Column(name = "dark_mode")
    private boolean darkMode;

    @Column(name = "notifications")
    private boolean notificationsEnabled;

    @Column(name = "tracking")
    private boolean trackingEnabled;

    @OneToMany(mappedBy = "userPreferences", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Device> devices = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
