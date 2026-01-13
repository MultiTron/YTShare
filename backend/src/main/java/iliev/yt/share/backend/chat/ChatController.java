package iliev.yt.share.backend.chat;

import iliev.yt.share.backend.chat.dto.ChatInputDto;
import iliev.yt.share.backend.chat.dto.ChatOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @GetMapping
    public Page<ChatOutputDto> getAllChats(final Pageable pageable) {
        return chatService.getAllChats(pageable);
    }

    @GetMapping("/all")
    public List<ChatOutputDto> getAllChats() {
        return chatService.getAllChats();
    }

    @GetMapping("/{id}")
    public ChatOutputDto getChatById(@PathVariable final UUID id) {
        return chatService.getChatById(id);
    }

    @PostMapping
    public ChatOutputDto createChat(@RequestBody final ChatInputDto inputDto) {
        return chatService.createChat(inputDto);
    }

    @PatchMapping("/{id}/participants")
    public ChatOutputDto addParticipants(
            @PathVariable final UUID id,
            @RequestBody final List<UUID> participantIds) {
        return chatService.addParticipants(id, participantIds);
    }

    @DeleteMapping("/{id}")
    public void deleteChat(@PathVariable final UUID id) {
        chatService.deleteChat(id);
    }
}
