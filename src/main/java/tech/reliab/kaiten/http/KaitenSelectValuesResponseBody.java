package tech.reliab.kaiten.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KaitenSelectValuesResponseBody {
    private Integer id;
    private String value;
}
