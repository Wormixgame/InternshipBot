package tech.reliab.kaiten.kaiten;

import lombok.Data;
import tech.reliab.kaiten.http.KaitenCard;
import tech.reliab.kaiten.util.KaitenFilter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class KaitenCardBatch {
    private final List<KaitenCard> kaitenCardList;
    public Map<Integer, KaitenCard> getKaitenCardMap(KaitenFilter kaitenFilter) {
        return kaitenCardList
                .stream()
                .filter(kaitenFilter::apply)
                .collect(Collectors.toMap(KaitenCard::getId, kaitenCard -> kaitenCard));
    }

    public Map<Integer, KaitenCard> getKaitenCardMap() {
       return kaitenCardList
               .stream()
               .collect(Collectors.toMap(KaitenCard::getId, kaitenCard -> kaitenCard));
    }
    public List<KaitenCard> getKaitenCardList(KaitenFilter kaitenFilter) {
        return kaitenCardList
                .stream()
                .filter(kaitenFilter::apply)
                .toList();
    }
}
