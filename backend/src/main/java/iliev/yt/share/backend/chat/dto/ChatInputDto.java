package iliev.yt.share.backend.chat.dto;

import java.util.List;
import java.util.UUID;

public record ChatInputDto(
        List<UUID> participantIds
) {
}
