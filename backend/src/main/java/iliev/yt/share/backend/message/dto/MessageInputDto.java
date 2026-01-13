package iliev.yt.share.backend.message.dto;

import iliev.yt.share.backend.message.enums.DeliveryStatus;
import java.util.UUID;

public record MessageInputDto(
        String content,
        DeliveryStatus status,
        UUID chatId,
        UUID senderId
) {
}
