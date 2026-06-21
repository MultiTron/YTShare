package iliev.yt.share.backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class FirebaseConfigTest {

    private FirebaseConfig configWithJson() {
        final FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "firebaseCredentialsJson", "{\"type\":\"service_account\"}");
        return config;
    }

    @Test
    void initialize_noExistingApp_initializesFirebase() throws Exception {
        final FirebaseConfig config = configWithJson();
        final GoogleCredentials credentials = mock(GoogleCredentials.class);

        try (MockedStatic<FirebaseApp> firebaseApp = mockStatic(FirebaseApp.class);
             MockedStatic<GoogleCredentials> googleCreds = mockStatic(GoogleCredentials.class)) {

            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of());
            googleCreds.when(() -> GoogleCredentials.fromStream(any(InputStream.class))).thenReturn(credentials);

            config.initialize();

            firebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), times(1));
        }
    }

    @Test
    void initialize_existingApp_doesNotInitializeAgain() throws Exception {
        final FirebaseConfig config = configWithJson();

        try (MockedStatic<FirebaseApp> firebaseApp = mockStatic(FirebaseApp.class)) {
            firebaseApp.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));

            config.initialize();

            firebaseApp.verify(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)), never());
        }
    }

    @Test
    void firebaseAuth_returnsInstance() {
        final FirebaseConfig config = new FirebaseConfig();
        final FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);

        try (MockedStatic<FirebaseAuth> staticAuth = mockStatic(FirebaseAuth.class)) {
            staticAuth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

            assertThat(config.firebaseAuth()).isSameAs(firebaseAuth);
        }
    }
}
