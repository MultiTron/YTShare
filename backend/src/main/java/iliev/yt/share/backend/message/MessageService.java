package iliev.yt.share.backend.message;

import iliev.yt.share.backend.chat.Chat;
import iliev.yt.share.backend.chat.ChatRepository;
import iliev.yt.share.backend.chat.exception.ChatNotFoundException;
import iliev.yt.share.backend.message.dto.MessageInputDto;
import iliev.yt.share.backend.message.exception.MessageNotFoundException;
import iliev.yt.share.backend.user.exception.UserNotFoundException;
import iliev.yt.share.backend.message.dto.MessageOutputDto;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public List<MessageOutputDto> getAllMessages() {
        return messageRepository.findAll().stream()
                .map(messageMapper::toOutputDto).toList();
    }

    public Page<MessageOutputDto> getAllMessages(final Pageable pageable) {
        return messageRepository.findAll(pageable)
                .map(messageMapper::toOutputDto);
    }

    public MessageOutputDto getMessageById(final UUID id) {
        final Message message = messageRepository.findById(id)
                .orElseThrow(() -> new MessageNotFoundException(id));
        return messageMapper.toOutputDto(message);
    }

    public List<MessageOutputDto> getMessagesByChatId(final UUID chatId) {
        return messageRepository.findByChatId(chatId).stream()
                .map(messageMapper::toOutputDto).toList();
    }

    public List<MessageOutputDto> getMessagesBySenderId(final UUID senderId) {
        return messageRepository.findBySenderId(senderId).stream()
                .map(messageMapper::toOutputDto).toList();
    }

    @Transactional
    public MessageOutputDto createMessage(final MessageInputDto inputDto) {
        final Chat chat = chatRepository.findById(inputDto.chatId())
                .orElseThrow(() -> new ChatNotFoundException(inputDto.chatId()));
        final User sender = userRepository.findById(inputDto.senderId())
                .orElseThrow(() -> new UserNotFoundException(inputDto.senderId()));

        final Message message = Message.builder()
                .content(inputDto.content())
                .status(inputDto.status())
                .chat(chat)
                .sender(sender)
                .build();

        final Message savedMessage = messageRepository.save(message);
        return messageMapper.toOutputDto(savedMessage);
    }

    @Transactional
    public void deleteMessage(final UUID id) {
        if (!messageRepository.existsById(id)) {
            throw new MessageNotFoundException(id);
        }
        messageRepository.deleteById(id);
    }
}
