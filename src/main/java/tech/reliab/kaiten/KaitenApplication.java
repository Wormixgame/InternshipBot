package tech.reliab.kaiten;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KaitenApplication {
    public static void main (String[] args) {
        SpringApplication.run(KaitenApplication.class, args);
    }
}
