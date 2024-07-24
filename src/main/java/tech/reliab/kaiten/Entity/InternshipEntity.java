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
public class InternshipEntity extends Entity{
    private Date dateOfStartInternship = null;
    private Date dateOfLastInterview = null;

    private Long chatId = null;
    private Boolean isNotified = false;

    @Override
    public String getEntityName() {
        return "Стажёр";
    }
}
