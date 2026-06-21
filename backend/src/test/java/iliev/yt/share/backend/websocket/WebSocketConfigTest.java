package iliev.yt.share.backend.websocket;

import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    private WebSocketAuthInterceptor interceptor;
    private WebSocketConfig config;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(mock(FirebaseAuth.class));
        config = new WebSocketConfig(interceptor);
    }

    @Test
    void configureMessageBroker_enablesBrokerAndPrefixes() {
        final MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void registerStompEndpoints_registersWsEndpointWithSockJs() {
        final StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        final StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws");
        verify(registration).setAllowedOriginPatterns("*");
        verify(registration).withSockJS();
    }

    @Test
    void configureClientInboundChannel_registersAuthInterceptor() {
        final ChannelRegistration registration = mock(ChannelRegistration.class);

        config.configureClientInboundChannel(registration);

        verify(registration).interceptors(interceptor);
    }
}
