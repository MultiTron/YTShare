package iliev.yt.share.backend;

import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static lombok.AccessLevel.PRIVATE;

@SpringBootApplication
@NoArgsConstructor(access = PRIVATE)
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
