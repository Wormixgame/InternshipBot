package tech.reliab.kaiten.services;

import tech.reliab.kaiten.http.KaitenCard;
import tech.reliab.kaiten.http.KaitenSelectValue;
import tech.reliab.kaiten.kaiten.KaitenCardBatch;

import java.util.List;


public interface KaitenService {
    KaitenCardBatch getCardsFromKaiten();
    Integer addCardOnKaiten(KaitenCard card);
    List<KaitenSelectValue> getSelectValues(Integer propertyId);
    KaitenSelectValue updateSelectValues(Integer propertyId, String value);
}
