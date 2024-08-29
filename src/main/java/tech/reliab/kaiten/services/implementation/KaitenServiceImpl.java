package tech.reliab.kaiten.services.implementation;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tech.reliab.kaiten.config.KaitenConfig;
import tech.reliab.kaiten.http.KaitenCard;
import tech.reliab.kaiten.http.KaitenSelectValue;
import tech.reliab.kaiten.kaiten.KaitenCardBatch;
import tech.reliab.kaiten.services.KaitenService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KaitenServiceImpl implements KaitenService {
    private static final Logger log = LoggerFactory.getLogger(KaitenServiceImpl.class);
    private final RestClient kaitenApiClient;

    @Override
    public KaitenCardBatch getCardsFromKaiten(KaitenConfig.BoardInfo boardInfo) throws HttpClientErrorException {
        KaitenCardBatch cardBatch = null;
        do {
            try {
                cardBatch = kaitenApiClient
                        .get()
                        .uri("latest/boards/{board_id}", boardInfo.getBoardId())
                        .exchange((clientRequest, clientResponse) -> {
                            if (clientResponse.getStatusCode().is4xxClientError()) {
                                throw new HttpClientErrorException(clientResponse.getStatusCode(), "Ошибка при получении карточки: " + clientResponse.getBody());
                            } else {
                                return clientResponse.bodyTo(KaitenCardBatch.class);
                            }
                        });
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() != 429) {
                    throw ex;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        log.error("Interrupted exception:", ie);
                    }
                }
            }
        } while (cardBatch == null);
        return cardBatch;
    }

    @Override
    public Integer addCardOnKaiten(KaitenCard card) throws HttpClientErrorException {
        Integer kaitenId = null;
        do {
            try {
                kaitenId = kaitenApiClient
                        .post()
                        .uri("latest/cards")
                        .body(card)
                        .exchange((clientRequest, clientResponse) -> {
                            if (clientResponse.getStatusCode().is4xxClientError()) {
                                throw new HttpClientErrorException(clientResponse.getStatusCode(), "Ошибка при добавлении карточки: " + clientResponse.bodyTo(String.class));
                            } else {
                                return clientResponse.bodyTo(new ParameterizedTypeReference<KaitenCard>() {
                                }).getId();
                            }
                        });
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() != 429) {
                    throw ex;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        log.error("Interrupted exception:", ie);
                    }
                }
            }
        } while (kaitenId == null);
        return kaitenId;
    }

    @Override
    public List<KaitenSelectValue> getSelectValues(Integer propertyId) throws HttpClientErrorException {
        List<KaitenSelectValue> selectValues = null;
        do {
            try {
                selectValues = kaitenApiClient
                        .get()
                        .uri("latest/company/custom-properties/{propertyId}/select-values", propertyId)
                        .exchange((clientRequest, clientResponse) -> {
                            if (clientResponse.getStatusCode().is4xxClientError()) {
                                throw new HttpClientErrorException(clientResponse.getStatusCode(), "Ошибка при получении select значений: " + clientResponse.bodyTo(String.class));
                            } else {
                                return clientResponse.bodyTo(new ParameterizedTypeReference<>() {
                                });
                            }
                        });
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() != 429) {
                    throw ex;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        log.error("Interrupted exception:", ie);
                    }
                }
            }

        }while (selectValues == null) ;
        return selectValues;
    }

        @Override
        public KaitenSelectValue updateSelectValues (Integer propertyId, String value) throws HttpClientErrorException {
            KaitenSelectValue selectValue = null;
            do {
                try {
                    selectValue = kaitenApiClient
                            .post()
                            .uri("latest/company/custom-properties/{propertyId}/select-values", propertyId)
                            .body(Map.of("value", value))
                            .exchange((clientRequest, clientResponse) -> {
                                if (clientResponse.getStatusCode().is4xxClientError()) {
                                    throw new HttpClientErrorException(clientResponse.getStatusCode(), "Ошибка при добавлении select значений: " + clientResponse.bodyTo(String.class));
                                } else {
                                    return clientResponse.bodyTo(new ParameterizedTypeReference<>() {});
                                }
                            });
                } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() != 429) {
                    throw ex;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        log.error("Interrupted exception:", ie);
                    }
                }
            }
            } while (selectValue == null);
            return selectValue;
        }

    }
