package iliev.yt.share.backend.friends;

import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    List<Friendship> findByUserId(UUID userId);
    List<Friendship> findByUserIdAndStatus(UUID userId, FriendshipStatus status);
    List<Friendship> findByFriendId(UUID friendId);
}
