package iliev.yt.share.backend.friends;

import iliev.yt.share.backend.common.entity.BaseEntity;
import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import iliev.yt.share.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "friendships")
public class Friendship extends BaseEntity {
    @ManyToOne
    private User user;

    @ManyToOne
    private User friend;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;
}
