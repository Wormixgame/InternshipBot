package tech.reliab.kaiten.services.implementation;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.View;
import reactor.core.publisher.Mono;
import tech.reliab.kaiten.Entity.Entity;
import tech.reliab.kaiten.Entity.InternshipEntity;
import tech.reliab.kaiten.services.GetCourseService;

@Service
@RequiredArgsConstructor
public class GetCourseServiceImpl implements GetCourseService {
    private final WebClient getCourseApiClient;
    private final View error;

    @Value("${tech.reliab.getCourse.templateUrl}")
    private String getCourseTemplateUrl;

    private <T extends Entity> boolean isIntern(Class<T> entityClass){
        return entityClass.equals(InternshipEntity.class);
    }

    @Override
    public <T extends Entity> void signal(Class<T> entityClass, Long id) {
        getCourseApiClient
                .post()
                .uri(getCourseTemplateUrl, id, isIntern(entityClass))
                .exchangeToMono((clientResponse) -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(String.class);
                    } else if (clientResponse.statusCode().is4xxClientError()) {
                        return clientResponse.bodyToMono(String.class);
                    }else {
                        return Mono.just("Микросервис геткурса недоступен.");
                    }
                }).subscribe();

    }
}
