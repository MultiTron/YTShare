package iliev.yt.share.backend.websocket;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import iliev.yt.share.backend.security.FirebaseAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private final MessageChannel channel = mock(MessageChannel.class);
    private FirebaseAuth firebaseAuth;
    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        firebaseAuth = mock(FirebaseAuth.class);
        interceptor = new WebSocketAuthInterceptor(firebaseAuth);
    }

    private Message<byte[]> connectMessage(final String authHeader) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        // Keep headers mutable: on spring-messaging 7.x getMessageHeaders() freezes them,
        // which would make the interceptor's setUser(...) throw. Do not remove.
        accessor.setLeaveMutable(true);
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void preSend_validToken_setsAuthenticatedUser() throws Exception {
        final FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("uid-1");
        when(firebaseAuth.verifyIdToken("good-token")).thenReturn(token);

        final Message<?> result = interceptor.preSend(connectMessage("Bearer good-token"), channel);

        final StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isInstanceOf(FirebaseAuthenticationToken.class);
        assertThat(((FirebaseAuthenticationToken) accessor.getUser()).getUid()).isEqualTo("uid-1");
    }

    @Test
    void preSend_invalidToken_throws() throws Exception {
        when(firebaseAuth.verifyIdToken("bad-token")).thenThrow(new RuntimeException("verification failed"));

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer bad-token"), channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Firebase token");
    }

    @Test
    void preSend_missingHeader_throws() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing Authorization header");
    }

    @Test
    void preSend_nonConnectCommand_passesThrough() {
        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        final Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
    }
}
