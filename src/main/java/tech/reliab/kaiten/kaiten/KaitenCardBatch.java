package tech.reliab.kaiten.kaiten;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.reliab.kaiten.http.KaitenCard;
import tech.reliab.kaiten.util.KaitenFilter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KaitenCardBatch {
    private List<KaitenCard> cards;
    public Map<Integer, KaitenCard> getKaitenCardMap(KaitenFilter kaitenFilter) {
        return cards
                .stream()
                .filter(kaitenFilter::apply)
                .collect(Collectors.toMap(KaitenCard::getId, kaitenCard -> kaitenCard));
    }

    public Map<Integer, KaitenCard> getKaitenCardMap() {
       return cards
               .stream()
               .collect(Collectors.toMap(KaitenCard::getId, kaitenCard -> kaitenCard));
    }
    public List<KaitenCard> getCards(KaitenFilter kaitenFilter) {
        return cards
                .stream()
                .filter(kaitenFilter::apply)
                .toList();
    }
}
