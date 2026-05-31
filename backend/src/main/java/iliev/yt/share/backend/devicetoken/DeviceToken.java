package iliev.yt.share.backend.devicetoken;

import iliev.yt.share.backend.common.entity.BaseEntity;
import iliev.yt.share.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "device_tokens")
public class DeviceToken extends BaseEntity {
    @Column(name = "fcm_token", nullable = false, unique = true)
    private String fcmToken;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
