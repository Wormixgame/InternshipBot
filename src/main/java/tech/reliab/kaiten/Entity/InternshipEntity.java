package tech.reliab.kaiten.Entity;


import lombok.*;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternshipEntity extends Entity{
    private Date dateOfStartInternship;

    private Integer stageOfInternship;
    private Date dateOfLastInterview;

    private Long chatId;
    private Boolean isNotified;
}
