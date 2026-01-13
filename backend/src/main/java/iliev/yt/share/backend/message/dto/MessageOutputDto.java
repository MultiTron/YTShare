package iliev.yt.share.backend.message.dto;

import iliev.yt.share.backend.chat.dto.ChatOutputDto;
import iliev.yt.share.backend.message.enums.DeliveryStatus;
import iliev.yt.share.backend.user.dto.UserOutputDto;
import java.util.UUID;

public record MessageOutputDto(
        UUID id,
        String content,
        DeliveryStatus status,
        ChatOutputDto chat,
        UserOutputDto sender
) {
}
