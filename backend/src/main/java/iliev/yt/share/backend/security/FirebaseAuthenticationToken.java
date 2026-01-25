package iliev.yt.share.backend.security;

import com.google.firebase.auth.FirebaseToken;
import java.util.Collection;
import java.util.Objects;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {

    @Getter
    private final transient FirebaseToken firebaseToken;
    private final transient Object principal;

    public FirebaseAuthenticationToken(final FirebaseToken firebaseToken, final Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.firebaseToken = firebaseToken;
        this.principal = firebaseToken.getUid();
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return firebaseToken;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public String getUid() {
        return firebaseToken.getUid();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final FirebaseAuthenticationToken that = (FirebaseAuthenticationToken) o;
        return Objects.equals(firebaseToken, that.firebaseToken) && Objects.equals(principal, that.principal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), firebaseToken, principal);
    }

    public String getEmail() {
        return firebaseToken.getEmail();
    }
}
