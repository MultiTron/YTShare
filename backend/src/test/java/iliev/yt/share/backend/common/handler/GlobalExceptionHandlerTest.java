package iliev.yt.share.backend.common.handler;

import iliev.yt.share.backend.common.exception.GenericNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGenericNotFoundException_returns404WithBody() {
        final ResponseEntity<Object> response =
                handler.handleGenericNotFoundException(new GenericNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        @SuppressWarnings("unchecked")
        final Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body)
                .containsEntry("status", 404)
                .containsEntry("error", "Not Found")
                .containsEntry("message", "missing")
                .containsKey("timestamp");
    }

    @Test
    void handleGlobalException_returns500WithBody() {
        final ResponseEntity<Object> response =
                handler.handleGlobalException(new Exception("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        @SuppressWarnings("unchecked")
        final Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body)
                .containsEntry("status", 500)
                .containsEntry("error", "Internal Server Error")
                .containsEntry("message", "boom")
                .containsKey("timestamp");
    }
}
