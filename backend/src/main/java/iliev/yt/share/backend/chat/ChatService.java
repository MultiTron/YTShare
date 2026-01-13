package iliev.yt.share.backend.chat;

import iliev.yt.share.backend.chat.dto.ChatInputDto;
import iliev.yt.share.backend.chat.dto.ChatOutputDto;
import iliev.yt.share.backend.chat.exception.ChatNotFoundException;
import iliev.yt.share.backend.chat.exception.ChatParticipantsNotFoundException;
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
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMapper chatMapper;
    private final UserRepository userRepository;

    public List<ChatOutputDto> getAllChats() {
        return chatRepository.findAll().stream()
                .map(chatMapper::toOutputDto).toList();
    }

    public Page<ChatOutputDto> getAllChats(final Pageable pageable) {
        return chatRepository.findAll(pageable)
                .map(chatMapper::toOutputDto);
    }

    public ChatOutputDto getChatById(final UUID id) {
        final Chat chat = chatRepository.findById(id)
                .orElseThrow(() -> new ChatNotFoundException(id));
        return chatMapper.toOutputDto(chat);
    }

    @Transactional
    public ChatOutputDto createChat(final ChatInputDto inputDto) {
        final List<User> participants = userRepository.findAllById(inputDto.participantIds());
        
        if (participants.size() != inputDto.participantIds().size()) {
            throw new ChatParticipantsNotFoundException();
        }

        final Chat chat = Chat.builder()
                .participants(participants)
                .build();

        final Chat savedChat = chatRepository.save(chat);
        return chatMapper.toOutputDto(savedChat);
    }

    @Transactional
    public ChatOutputDto addParticipants(final UUID chatId, final List<UUID> participantIds) {
        final Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException(chatId));
        
        final List<User> newParticipants = userRepository.findAllById(participantIds);
        
        if (newParticipants.size() != participantIds.size()) {
            throw new ChatParticipantsNotFoundException();
        }

        chat.getParticipants().addAll(newParticipants);
        return chatMapper.toOutputDto(chat);
    }

    @Transactional
    public void deleteChat(final UUID id) {
        if (!chatRepository.existsById(id)) {
            throw new ChatNotFoundException(id);
        }
        chatRepository.deleteById(id);
    }
}
