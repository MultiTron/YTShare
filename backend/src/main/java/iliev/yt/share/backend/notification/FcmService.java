package iliev.yt.share.backend.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void sendPushNotification(final String fcmToken, final String title, final String body,
                                     final String chatId, final String senderId, final String senderName) {
        final Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body.length() > 100 ? body.substring(0, 100) + "..." : body)
                        .build())
                .putData("chatId", chatId)
                .putData("senderId", senderId)
                .putData("senderName", senderName)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("Failed to send FCM notification to token {}: {}", fcmToken, e.getMessage());
        }
    }
}
