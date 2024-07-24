package tech.reliab.kaiten.services.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tech.reliab.kaiten.http.KaitenCard;
import tech.reliab.kaiten.http.KaitenSelectValue;
import tech.reliab.kaiten.kaiten.KaitenCardBatch;
import tech.reliab.kaiten.services.KaitenService;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KaitenServiceImpl implements KaitenService {
    private final RestClient kaitenApiClient;

    @Override
    public KaitenCardBatch getCardsFromKaiten() throws HttpClientErrorException {
        return kaitenApiClient
                .get()
                .uri("latest/cards")
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is4xxClientError()){
                        throw new HttpClientErrorException(clientResponse.getStatusCode(), "Ошибка при добавлении карточки: " + clientResponse.getBody());
                    }
                    else{
                        return new KaitenCardBatch(clientResponse.bodyTo(new ParameterizedTypeReference<>() {}));
                    }
                });
    }

    @Override
    public Integer addCardOnKaiten(KaitenCard card) throws HttpClientErrorException {
        return kaitenApiClient
                .post()
                .uri("latest/cards")
                .body(card)
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is4xxClientError()){

                        throw new HttpClientErrorException(clientResponse.getStatusCode(), "Ошибка при добавлении карточки: " + clientResponse.bodyTo(String.class));
                    }
                    else{
                        return clientResponse.bodyTo(new ParameterizedTypeReference<KaitenCard>() {}).getId();
                    }
                });
    }

    @Override
    public List<KaitenSelectValue> getSelectValues(Integer propertyId) throws HttpClientErrorException {
        return kaitenApiClient
                .get()
                .uri("latest/company/custom-properties/{propertyId}/select-values", propertyId)
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is4xxClientError()){
                        throw new HttpClientErrorException(clientResponse.getStatusCode(), "Ошибка при получении select значений: " + clientResponse.bodyTo(String.class));
                    }
                    else{
                        return clientResponse.bodyTo(new ParameterizedTypeReference<>() {});
                    }
                });
    }

    @Override
    public KaitenSelectValue updateSelectValues(Integer propertyId, String value) throws HttpClientErrorException {
        return kaitenApiClient
                .post()
                .uri("latest/company/custom-properties/{propertyId}/select-values", propertyId)
                .body(Map.of("value", value))
                .exchange((clientRequest, clientResponse) -> {
                    if (clientResponse.getStatusCode().is4xxClientError()){
                        throw new HttpClientErrorException(clientResponse.getStatusCode(), "Ошибка при добавлении select значений: " + clientResponse.bodyTo(String.class));
                    }
                    else{
                        return clientResponse.bodyTo(new ParameterizedTypeReference<>() {});
                    }
                });
    }

}
