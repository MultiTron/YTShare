package iliev.yt.share.backend.chat;

import iliev.yt.share.backend.chat.dto.ChatInputDto;
import iliev.yt.share.backend.chat.dto.ChatOutputDto;
import iliev.yt.share.backend.chat.exception.ChatNotFoundException;
import iliev.yt.share.backend.chat.exception.ChatParticipantsNotFoundException;
import iliev.yt.share.backend.user.User;
import iliev.yt.share.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    private UUID chatId;
    private Chat chat;
    private ChatOutputDto outputDto;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        chat = Chat.builder().participants(new ArrayList<>()).build();
        chat.setId(chatId);
        outputDto = new ChatOutputDto(chatId, List.of());
    }

    private User userWithId(final UUID id) {
        final User user = User.builder().build();
        user.setId(id);
        return user;
    }

    @Test
    void getAllChats_returnsMappedList() {
        when(chatRepository.findAll()).thenReturn(List.of(chat));
        when(chatMapper.toOutputDto(chat)).thenReturn(outputDto);

        assertThat(chatService.getAllChats()).containsExactly(outputDto);
    }

    @Test
    void getAllChats_paged_returnsMappedPage() {
        final Pageable pageable = PageRequest.of(0, 10);
        when(chatRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(chat)));
        when(chatMapper.toOutputDto(chat)).thenReturn(outputDto);

        final Page<ChatOutputDto> result = chatService.getAllChats(pageable);

        assertThat(result.getContent()).containsExactly(outputDto);
    }

    @Test
    void getChatById_found_returnsDto() {
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(chatMapper.toOutputDto(chat)).thenReturn(outputDto);

        assertThat(chatService.getChatById(chatId)).isEqualTo(outputDto);
    }

    @Test
    void getChatById_notFound_throws() {
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getChatById(chatId))
                .isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void createChat_allParticipantsFound_savesChat() {
        final UUID p1 = UUID.randomUUID();
        final UUID p2 = UUID.randomUUID();
        final ChatInputDto inputDto = new ChatInputDto(List.of(p1, p2));
        final List<User> participants = List.of(userWithId(p1), userWithId(p2));
        when(userRepository.findAllById(inputDto.participantIds())).thenReturn(participants);
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);
        when(chatMapper.toOutputDto(chat)).thenReturn(outputDto);

        assertThat(chatService.createChat(inputDto)).isEqualTo(outputDto);

        final ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(captor.capture());
        assertThat(captor.getValue().getParticipants()).isEqualTo(participants);
    }

    @Test
    void createChat_missingParticipant_throwsAndDoesNotSave() {
        final UUID p1 = UUID.randomUUID();
        final UUID p2 = UUID.randomUUID();
        final ChatInputDto inputDto = new ChatInputDto(List.of(p1, p2));
        when(userRepository.findAllById(inputDto.participantIds())).thenReturn(List.of(userWithId(p1)));

        assertThatThrownBy(() -> chatService.createChat(inputDto))
                .isInstanceOf(ChatParticipantsNotFoundException.class);
        verify(chatRepository, never()).save(any());
    }

    @Test
    void addParticipants_allFound_addsToChat() {
        final UUID p1 = UUID.randomUUID();
        final List<UUID> ids = List.of(p1);
        final User newParticipant = userWithId(p1);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findAllById(ids)).thenReturn(List.of(newParticipant));
        when(chatMapper.toOutputDto(chat)).thenReturn(outputDto);

        assertThat(chatService.addParticipants(chatId, ids)).isEqualTo(outputDto);
        assertThat(chat.getParticipants()).contains(newParticipant);
    }

    @Test
    void addParticipants_chatNotFound_throws() {
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.addParticipants(chatId, List.of(UUID.randomUUID())))
                .isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void addParticipants_missingParticipant_throws() {
        final UUID p1 = UUID.randomUUID();
        final UUID p2 = UUID.randomUUID();
        final List<UUID> ids = List.of(p1, p2);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findAllById(ids)).thenReturn(List.of(userWithId(p1)));

        assertThatThrownBy(() -> chatService.addParticipants(chatId, ids))
                .isInstanceOf(ChatParticipantsNotFoundException.class);
    }

    @Test
    void deleteChat_exists_deletes() {
        when(chatRepository.existsById(chatId)).thenReturn(true);

        chatService.deleteChat(chatId);

        verify(chatRepository).deleteById(chatId);
    }

    @Test
    void deleteChat_notExists_throwsAndDoesNotDelete() {
        when(chatRepository.existsById(chatId)).thenReturn(false);

        assertThatThrownBy(() -> chatService.deleteChat(chatId))
                .isInstanceOf(ChatNotFoundException.class);
        verify(chatRepository, never()).deleteById(chatId);
    }
}
