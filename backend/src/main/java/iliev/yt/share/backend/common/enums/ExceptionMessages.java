package iliev.yt.share.backend.common.enums;

import lombok.Getter;

import java.util.UUID;

@Getter
public enum ExceptionMessages {
    USER_NOT_FOUND("User not found with id: %s"),
    USER_NOT_FOUND_BY_EMAIL("User not found with email: %s"),
    FRIEND_NOT_FOUND("Friend not found with id: %s"),
    FIREBASE_USER_NOT_FOUND("User not found with firebaseId: %s"),
    FRIENDSHIP_NOT_FOUND("Friendship not found with id: %s"),
    VIDEO_NOT_FOUND("Video not found with id: %s"),
    CHAT_NOT_FOUND("Chat not found with id: %s"),
    CHAT_PARTICIPANTS_NOT_FOUND("One or more participants not found"),
    MESSAGE_NOT_FOUND("Message not found with id: %s"),
    USER_PREFERENCES_NOT_FOUND("User preferences not found with id: %s"),
    DEVICE_NOT_FOUND("Device not found with id: %s"),
    DEVICE_TOKEN_NOT_FOUND("Device token not found with id: %s");

    private final String message;

    ExceptionMessages(String message) {
        this.message = message;
    }

    public String getMessage(UUID id) {
        return String.format(message, id.toString());
    }

    public String getMessage(String prop) {
        return String.format(message, prop);
    }
}
