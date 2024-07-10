package tech.reliab.kaiten.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class KaitenGetResponseBody {
    private Integer id;
    private Integer type_id;
    private Integer column_id;
    private Map<String, Object> properties;
}
