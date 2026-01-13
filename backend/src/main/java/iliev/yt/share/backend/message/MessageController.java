package iliev.yt.share.backend.message;

import iliev.yt.share.backend.message.dto.MessageInputDto;
import iliev.yt.share.backend.message.dto.MessageOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @GetMapping
    public Page<MessageOutputDto> getAllMessages(final Pageable pageable) {
        return messageService.getAllMessages(pageable);
    }

    @GetMapping("/all")
    public List<MessageOutputDto> getAllMessages() {
        return messageService.getAllMessages();
    }

    @GetMapping("/{id}")
    public MessageOutputDto getMessageById(@PathVariable final UUID id) {
        return messageService.getMessageById(id);
    }

    @GetMapping("/chat/{chatId}")
    public List<MessageOutputDto> getMessagesByChatId(@PathVariable final UUID chatId) {
        return messageService.getMessagesByChatId(chatId);
    }

    @GetMapping("/sender/{senderId}")
    public List<MessageOutputDto> getMessagesBySenderId(@PathVariable final UUID senderId) {
        return messageService.getMessagesBySenderId(senderId);
    }

    @PostMapping
    public MessageOutputDto createMessage(@RequestBody final MessageInputDto inputDto) {
        return messageService.createMessage(inputDto);
    }

    @DeleteMapping("/{id}")
    public void deleteMessage(@PathVariable final UUID id) {
        messageService.deleteMessage(id);
    }
}
