package tech.reliab.kaiten.util;

import tech.reliab.kaiten.config.KaitenConfig;
import tech.reliab.kaiten.http.KaitenCard;

public class KaitenFilterByType implements KaitenFilter {

    private final Integer cardTypeId;
    private final Integer boardId;


    public KaitenFilterByType(KaitenConfig.BoardInfo boardInfo){
        cardTypeId = boardInfo.getCardTypeId();
        boardId = boardInfo.getBoardId();
    }

    @Override
    public boolean apply(KaitenCard card) {
        return card.getTypeId().equals(cardTypeId) && card.getBoardId().equals(boardId);
    }
}
