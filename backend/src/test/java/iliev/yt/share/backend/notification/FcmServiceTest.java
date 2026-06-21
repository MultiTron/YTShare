package iliev.yt.share.backend.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @InjectMocks
    private FcmService fcmService;

    @Test
    void sendPushNotification_sendsMessageViaFirebaseMessaging() throws Exception {
        final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        when(messaging.send(any(Message.class))).thenReturn("message-id");

        try (MockedStatic<FirebaseMessaging> firebase = mockStatic(FirebaseMessaging.class)) {
            firebase.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            fcmService.sendPushNotification("token", "Alice", "hello", "chat-1", "sender-1", "Alice");

            verify(messaging, times(1)).send(any(Message.class));
        }
    }

    @Test
    void sendPushNotification_longBody_stillSends() throws Exception {
        final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        when(messaging.send(any(Message.class))).thenReturn("message-id");
        final String longBody = "x".repeat(250);

        try (MockedStatic<FirebaseMessaging> firebase = mockStatic(FirebaseMessaging.class)) {
            firebase.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            fcmService.sendPushNotification("token", "Alice", longBody, "chat-1", "sender-1", "Alice");

            verify(messaging).send(any(Message.class));
        }
    }

    @Test
    void sendPushNotification_sendThrows_isSwallowed() throws Exception {
        final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        when(messaging.send(any(Message.class))).thenThrow(new RuntimeException("boom"));

        try (MockedStatic<FirebaseMessaging> firebase = mockStatic(FirebaseMessaging.class)) {
            firebase.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            assertThatCode(() ->
                    fcmService.sendPushNotification("token", "Alice", "hello", "chat-1", "sender-1", "Alice"))
                    .doesNotThrowAnyException();
        }
    }
}
