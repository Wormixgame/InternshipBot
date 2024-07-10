package tech.reliab.kaiten.Entity;

import lombok.*;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeEntity extends Entity{
    private Date dateOfStartPractice;
}