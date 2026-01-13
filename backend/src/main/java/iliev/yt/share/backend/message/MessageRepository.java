package iliev.yt.share.backend.message;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByChatId(UUID chatId);
    List<Message> findBySenderId(UUID senderId);
}
