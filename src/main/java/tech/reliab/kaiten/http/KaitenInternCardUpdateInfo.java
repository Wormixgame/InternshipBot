package tech.reliab.kaiten.http;

import lombok.Data;


import java.util.Date;

@Data
public class KaitenInternCardUpdateInfo {
    private Integer id;
    private Date startDate;
    private Date lastInterviewDate;
}
