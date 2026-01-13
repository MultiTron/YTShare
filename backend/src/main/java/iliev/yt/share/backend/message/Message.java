package iliev.yt.share.backend.message;

import iliev.yt.share.backend.chat.Chat;
import iliev.yt.share.backend.common.entity.BaseEntity;
import iliev.yt.share.backend.message.enums.DeliveryStatus;
import iliev.yt.share.backend.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "messages")
public class Message extends BaseEntity {
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE, CascadeType.DETACH })
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE, CascadeType.DETACH })
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
}
