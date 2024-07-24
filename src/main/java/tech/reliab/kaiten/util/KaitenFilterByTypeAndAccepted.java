package tech.reliab.kaiten.util;

import tech.reliab.kaiten.config.KaitenConfig;
import tech.reliab.kaiten.http.KaitenCard;

public class KaitenFilterByTypeAndAccepted implements KaitenFilter {
    private final Integer cardTypeId;
    private final Integer boardId;
    private final Integer columnId;

    public KaitenFilterByTypeAndAccepted(KaitenConfig.BoardInfo boardInfo){
        cardTypeId = boardInfo.getCardTypeId();
        boardId = boardInfo.getBoardId();
        columnId = boardInfo.getCandidatesColumnId();
    }

    @Override
    public boolean apply(KaitenCard card) {
        return card.getTypeId().equals(cardTypeId) && card.getBoardId().equals(boardId) && !card.getColumnId().equals(columnId);
    }
}
