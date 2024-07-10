package tech.reliab.kaiten.Entity;

import lombok.Data;

import java.util.Date;

@Data
public abstract class Entity {
    protected Long id;

    protected String fullName;

    protected Date dateOfBirth;

    protected String email;
    protected String cityLocation;
    protected String institution;

    protected Date dateOfGraduation;

    protected String internshipDirection;
    protected String phoneNumber;
    protected String tgUrl;
    protected String studyDegree;
    protected String specialization;
    protected String infoAboutHowFindInternship;
    protected String infoAboutWorkExperience;
    protected String infoAboutOpinionOfInternship;
    protected String infoAboutIntern;

    protected Integer idCardKaiten;
    protected Long idGetCourse;

    public String getTitle(){
        return fullName;
    }
    public String getDirection(){
        return internshipDirection;
    }

    public String getDescription(){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("## Опыт работы\n\n");
        String workExperience = infoAboutWorkExperience;
        stringBuilder.append(workExperience != null ? workExperience : ".").append("\n\n");

        stringBuilder.append("## Откуда узнали\n\n");
        String howFind = infoAboutHowFindInternship;
        stringBuilder.append(howFind != null ? howFind : ".").append("\n\n");

        stringBuilder.append("## Откуда заинтересованность\n\n");
        String opinion = infoAboutOpinionOfInternship;
        stringBuilder.append(opinion != null ? opinion : ".").append("\n\n");

        stringBuilder.append("## Немного о себе\n\n");
        String aboutMe = infoAboutIntern;
        stringBuilder.append(aboutMe != null ? aboutMe : ".").append("\n\n");

        return stringBuilder.toString();
    }
}
