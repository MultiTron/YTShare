package iliev.yt.share.backend.security;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@NoArgsConstructor(access = PRIVATE)
public final class SecurityUtils {

    public static Optional<FirebaseAuthenticationToken> getCurrentAuthentication() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof final FirebaseAuthenticationToken firebaseAuth) {
            return Optional.of(firebaseAuth);
        }
        return Optional.empty();
    }

    public static Optional<String> getCurrentUserUid() {
        return getCurrentAuthentication().map(FirebaseAuthenticationToken::getUid);
    }

    public static Optional<String> getCurrentUserEmail() {
        return getCurrentAuthentication().map(FirebaseAuthenticationToken::getEmail);
    }

    public static String requireCurrentUserUid() {
        return getCurrentUserUid()
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }
}
