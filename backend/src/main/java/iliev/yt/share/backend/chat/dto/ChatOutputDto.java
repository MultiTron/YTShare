package iliev.yt.share.backend.chat.dto;

import iliev.yt.share.backend.user.dto.UserOutputDto;
import java.util.List;
import java.util.UUID;

public record ChatOutputDto(
        UUID id,
        List<UserOutputDto> participants
) {
}
