package iliev.yt.share.backend.websocket;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import iliev.yt.share.backend.security.FirebaseAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuth firebaseAuth;

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            final String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                final String token = authHeader.substring(BEARER_PREFIX.length());
                try {
                    final FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(token);
                    final FirebaseAuthenticationToken authentication = new FirebaseAuthenticationToken(
                            firebaseToken,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    accessor.setUser(authentication);
                    log.debug("WebSocket authenticated user: {}", firebaseToken.getUid());
                } catch (Exception e) {
                    log.warn("WebSocket Firebase token verification failed: {}", e.getMessage());
                    throw new IllegalArgumentException("Invalid Firebase token");
                }
            } else {
                throw new IllegalArgumentException("Missing Authorization header");
            }
        }

        return message;
    }
}
