package tech.reliab.kaiten.services.implementation;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tech.reliab.kaiten.Entity.Entity;
import tech.reliab.kaiten.services.DatabaseService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatabaseServiceImpl implements DatabaseService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);
    private final WebClient dbApiClient;
    private final Map<Class<? extends Entity>, String> dbUrls;

    @Override
    public <T extends Entity> void create(T entity) {
        dbApiClient
                .post()
                .uri(dbUrls.get(entity.getClass()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(entity), entity.getClass())
                .exchangeToMono((clientResponse) -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(entity.getClass());
                    } else {
                        clientResponse.toEntity(String.class).subscribe(stringResponseEntity -> {log.error(stringResponseEntity.getBody());});
                        throw new HttpClientErrorException(clientResponse.statusCode());
                    }
                }).subscribe();
    }

    @Override
    public <T extends Entity> void update(Class<T> entityClass, Map<String, Object> fields, Long id) {
        dbApiClient
                .put()
                .uri("{uri}/{id}", dbUrls.get(entityClass), id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(fields), Map.class)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(entityClass);
                    } else {
                        throw new HttpClientErrorException(clientResponse.statusCode());
                    }
                }).subscribe();

    }

    @Override
    public <T extends Entity> void delete(Class<T> entityClass, Long id) {
        dbApiClient
                .delete()
                .uri("{uri}/{id}", dbUrls.get(entityClass), id)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(entityClass);
                    }else{
                        throw new HttpClientErrorException(clientResponse.statusCode());
                    }
                }).subscribe();
    }

    @Override
    public <T extends Entity> List<T> getEntities(Class<T> entityClass) {
        return dbApiClient
                .get()
                .uri(dbUrls.get(entityClass))
                .retrieve()
                .bodyToFlux(entityClass)
                .collectList()
                .block();
    }

}
