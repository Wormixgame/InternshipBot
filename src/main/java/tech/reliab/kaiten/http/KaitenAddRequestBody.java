package tech.reliab.kaiten.http;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KaitenAddRequestBody {
    public Integer boardId;
    public Integer columnId;
    public Integer typeId;
    public Integer laneId;
    public String title;
    public String description;
    public Map<String, Object> properties;
}
