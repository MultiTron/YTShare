package iliev.yt.share.backend.user;

import iliev.yt.share.backend.common.entity.BaseEntity;
import iliev.yt.share.backend.user.preferences.UserPreferences;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
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
@Table(name = "users")
public class User extends BaseEntity {
    @Column(unique = true, nullable = false, length = 128)
    private String firebaseUid;

    @Column(unique = true, nullable = false, length = 128)
    private String email;

    @Column(nullable = false, length = 128)
    private String firstName;

    @Column(nullable = false, length = 128)
    private String lastName;

    @OneToOne(mappedBy = "user", cascade = jakarta.persistence.CascadeType.ALL)
    private UserPreferences userPreferences;
}
