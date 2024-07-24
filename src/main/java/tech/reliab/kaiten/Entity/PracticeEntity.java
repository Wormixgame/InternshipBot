package tech.reliab.kaiten.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PracticeEntity extends Entity{
    private Date dateOfStartPractice = new Date();

    @Override
    public String getEntityName() {
        return "Практикант";
    }
}