package iliev.yt.share.backend.friends;

import iliev.yt.share.backend.friends.dto.FriendshipInputDto;
import iliev.yt.share.backend.friends.dto.FriendshipOutputDto;
import iliev.yt.share.backend.friends.dto.FriendshipStatusUpdateDto;
import iliev.yt.share.backend.friends.enums.FriendshipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/friendships")
@RequiredArgsConstructor
public class FriendshipController {
    private final FriendshipService friendshipService;

    @GetMapping
    public Page<FriendshipOutputDto> getAllFriendships(final Pageable pageable) {
        return friendshipService.getAllFriendships(pageable);
    }

    @GetMapping("/all")
    public List<FriendshipOutputDto> getAllFriendships() {
        return friendshipService.getAllFriendships();
    }

    @GetMapping("/{id}")
    public FriendshipOutputDto getFriendshipById(@PathVariable final UUID id) {
        return friendshipService.getFriendshipById(id);
    }

    @GetMapping("/user/{userId}")
    public List<FriendshipOutputDto> getFriendshipsByUserId(@PathVariable final UUID userId) {
        return friendshipService.getFriendshipsByUserId(userId);
    }

    @GetMapping("/user/{userId}/status")
    public List<FriendshipOutputDto> getFriendshipsByUserIdAndStatus(
            @PathVariable final UUID userId,
            @RequestParam final FriendshipStatus status) {
        return friendshipService.getFriendshipsByUserIdAndStatus(userId, status);
    }

    @PostMapping
    public FriendshipOutputDto createFriendship(@RequestBody final FriendshipInputDto inputDto) {
        return friendshipService.createFriendship(inputDto);
    }

    @PatchMapping("/{id}/status")
    public FriendshipOutputDto updateFriendshipStatus(
            @PathVariable final UUID id,
            @RequestParam final FriendshipStatus status) {
        return friendshipService.updateFriendshipStatus(FriendshipStatusUpdateDto.builder()
                .id(id)
                .status(status)
                .build());
    }

    @DeleteMapping("/{id}")
    public void deleteFriendship(@PathVariable final UUID id) {
        friendshipService.deleteFriendship(id);
    }
}
