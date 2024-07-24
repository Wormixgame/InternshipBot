package tech.reliab.kaiten.kaiten;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import tech.reliab.kaiten.Entity.*;
import tech.reliab.kaiten.config.KaitenConfig;
import tech.reliab.kaiten.http.*;

import tech.reliab.kaiten.util.KaitenFilterByType;
import tech.reliab.kaiten.util.KaitenFilterByTypeAndAccepted;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@EnableScheduling
@Service
@RequiredArgsConstructor
public class KaitenServicePrevious {
    private final KaitenConfig KAITEN_CONFIG;
    private final tech.reliab.kaiten.services.KaitenService kaitenService;
    private final tech.reliab.kaiten.services.DatabaseService databaseService;
    private final tech.reliab.kaiten.services.GetCourseService getCourseService;
    private final tech.reliab.kaiten.kaiten.KaitenCardMapper kaitenCardMapper;
    private static KaitenConfig.BoardInfo INTERN_BOARD_INFO;
    private static KaitenConfig.BoardInfo PRACTICE_BOARD_INFO;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @PostConstruct
    public void init(){
        INTERN_BOARD_INFO =  KAITEN_CONFIG.boardInfos.get("intern");
        PRACTICE_BOARD_INFO = KAITEN_CONFIG.boardInfos.get("practice");
    }


    public Integer getLaneIDByInternshipDirection(KaitenConfig.BoardInfo boardInfo, String internshipDirection) throws IllegalArgumentException{
        if(boardInfo.lanes.containsKey(internshipDirection)){
            return boardInfo.lanes.get(internshipDirection);
        }else{
            throw new IllegalArgumentException("Lane with specified name (" + internshipDirection + ") not found");
        }
    }

    public KaitenCard fillKaitenAddRequestBody(KaitenConfig.BoardInfo boardInfo, Entity entity) throws IllegalArgumentException{
        Integer laneId =getLaneIDByInternshipDirection(boardInfo, entity.getDirection());
        return new KaitenCard(
                null,
                boardInfo.getBoardId(),
                boardInfo.getCandidatesColumnId(),
                boardInfo.getCardTypeId(),
                laneId,
                entity.getTitle() != null ? entity.getTitle() : "Заглушка",
                entity.getDescription(),
                kaitenCardMapper.mapValuesForKaiten(entity)
        );
    }

    private <T extends Entity> void addCardToKaiten(Class<T> entityType, KaitenConfig.BoardInfo boardInfo){
        List<T> dbEntities;

        try{
            dbEntities = databaseService.getEntities(entityType).stream().filter(entity -> entity.getIdCardKaiten() == null).toList();
        }catch (HttpClientErrorException ex){
            log.error(ex.getMessage());
            return;
        }

        for (T dbEntity : dbEntities) {
            KaitenCard kaitenAddRequestBody;
            try{
                kaitenAddRequestBody = fillKaitenAddRequestBody(boardInfo, dbEntity);
            }catch (IllegalArgumentException ex){
                log.warn("{} for entity ({}) with id ({})", ex.getMessage(), dbEntity.getEntityName(), dbEntity.getId());
                continue;
            }

            try {
                Integer cardId = kaitenService.addCardOnKaiten(kaitenAddRequestBody);
                databaseService.update(entityType, Map.of( "id", dbEntity.getId(), "idCardKaiten", cardId), dbEntity.getId());
            }catch (Exception ex){
                log.error("Mapping exception:", ex);
                return;
            }
        }
    }

    @PostConstruct
    @Scheduled(cron = "0 */5 * * * *")
    public void addCardsToKaiten(){
        addCardToKaiten(InternshipEntity.class, INTERN_BOARD_INFO);
        addCardToKaiten(PracticeEntity.class, PRACTICE_BOARD_INFO);
    }


    @PostConstruct
    @Scheduled(cron = "30 */5 * * * *")
    public void updatePractice(){
        List<PracticeEntity> practiceEntities;
        try {
            practiceEntities = databaseService.getEntities(PracticeEntity.class).stream().filter(entity -> entity.getIdCardKaiten() == null).toList();
        }catch (HttpClientErrorException ex){
            log.error(ex.getMessage());
            return;
        }

        KaitenCardBatch cardBatch = kaitenService.getCardsFromKaiten();
        Map<Integer, KaitenCard> practiceCards = cardBatch.getKaitenCardMap(new KaitenFilterByTypeAndAccepted(PRACTICE_BOARD_INFO));
        for (PracticeEntity entity : practiceEntities) {
            Integer kaitenId = entity.getIdCardKaiten();
            if (!practiceCards.containsKey(kaitenId) || entity.getIdGetCourse() != null){
                continue;
            }

            getCourseService.signal(PracticeEntity.class, entity.getId());
        }
    }


    private boolean notSameDate(Date first, Date second){
        if (first == null && second == null){
            return false;
        }
        if (first == null || second == null){
            return true;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return !sdf.format(first).equals(sdf.format(second));
    }


    private <T extends Entity> void deleteEntries(Class<T> entityType, Map<Integer, KaitenCard> kaitenCards){
        List<T> dbEntities = databaseService.getEntities(entityType).stream().filter(entity -> entity.getIdCardKaiten() != null).toList();
        List<T> entitiesToDelete = dbEntities.stream().filter(entity -> !kaitenCards.containsKey(entity.getIdCardKaiten())).toList();
        for (T entity : entitiesToDelete){
            databaseService.delete(entityType, entity.getId());
            log.info("({}) with id ({}) was deleted", entity.getEntityName(), entity.getId());
        }
    }

    @PostConstruct
    @Scheduled(cron = "0 */10 * * * *")
    public void deleteEntriesWithDeletedKaitenCard(){
        KaitenCardBatch kaitenCards = kaitenService.getCardsFromKaiten();
        Map<Integer, KaitenCard> cardMap = kaitenCards.getKaitenCardMap();
        deleteEntries(InternshipEntity.class, cardMap);
        deleteEntries(PracticeEntity.class, cardMap);
    }

    private <T extends Entity> void updateDatabaseEntities(Class<T> entityType, KaitenCardBatch kaitenCardBatch, KaitenConfig.BoardInfo boardInfo){
        ObjectMapper mapper = new ObjectMapper();
        List<T> dbEntities = databaseService.getEntities(entityType);

        Set<String> tgUrls = dbEntities.stream().map(Entity::getTgUrl).collect(Collectors.toSet());
        Set<Integer> kaitenIds = dbEntities.stream().map(Entity::getIdCardKaiten).filter(Objects::nonNull).collect(Collectors.toSet());

        List<KaitenCard> entityCards = kaitenCardBatch.getKaitenCardList(new KaitenFilterByType(boardInfo)).stream().filter(kaitenCard -> !kaitenIds.contains(kaitenCard.getId())).toList();

        for (KaitenCard entityCard : entityCards){
            Map<String, Object> map = kaitenCardMapper.mapValuesForDatabase(entityType, entityCard);
            map.put("idCardKaiten", entityCard.getId());
            map.put("fullName", entityCard.getTitle());
            String tgUrl = (String) map.get("tgUrl");

            // Проверяем, существует ли URL телеграмм в базе данных
            if (tgUrls.contains(tgUrl)) {
                log.info("URL телеграмма ({}) уже существует в базе данных. Запись не будет добавлена.", tgUrl);
                continue;
            }
            try {
                T entity = mapper.readValue(mapper.writeValueAsString(map), entityType);
                log.info("Добавление новой записи: {}", mapper.writeValueAsString(entity));
                // Сохраняем новую запись в базе данных
                databaseService.create(entity);
            } catch (Exception ex) {
                log.error("Ошибка при создании записи в базе данных:", ex);
            }
        }
    }


    @Scheduled(initialDelay = 5000)
    public void updateDatabaseFromKaiten(){
        KaitenCardBatch kaitenCards = kaitenService.getCardsFromKaiten();
        updateDatabaseEntities(InternshipEntity.class, kaitenCards, INTERN_BOARD_INFO);
        updateDatabaseEntities(PracticeEntity.class, kaitenCards, PRACTICE_BOARD_INFO);
    }

    @PostConstruct
    @Scheduled(cron = "0 */1 * * * *")
    public void updateInternDB() {
        List<InternshipEntity> internsWithCards;
        try {
            internsWithCards = databaseService.getEntities(InternshipEntity.class).stream().filter(entity -> entity.getIdCardKaiten() != null).toList();
        } catch (HttpClientErrorException ex) {
            log.error(ex.getMessage());
            return;
        }
        String lastInterviewDatePropertyId = "id_" + INTERN_BOARD_INFO.serviceProperties.get("dateOfLastInterviewId");
        String startDatePropertyId = "id_" + INTERN_BOARD_INFO.serviceProperties.get("dateOfStartInternShipId");

        KaitenCardBatch cardBatch = kaitenService.getCardsFromKaiten();
        Map<Integer, KaitenCard> kaitenCards = cardBatch.getKaitenCardMap(new KaitenFilterByTypeAndAccepted(INTERN_BOARD_INFO));


        for (InternshipEntity intern : internsWithCards) {
            Integer kaitenId = intern.getIdCardKaiten();

            if (!kaitenCards.containsKey(kaitenId)) {
                continue;
            }

            HashMap<String, Object> updateFields = new HashMap<>();

            KaitenCard kaitenCard = kaitenCards.get(kaitenId);
            Map<String, Object> cardProperties = kaitenCard.getProperties();

            Date kaitenStartDate = kaitenCardMapper.getDateFromKaitenDate(cardProperties.get(startDatePropertyId));
            Date dbStartDate = intern.getDateOfStartInternship();

            if (kaitenStartDate != null && notSameDate(kaitenStartDate, dbStartDate)) {
                updateFields.put("dateOfStartInternship", DATE_FORMATTER.format(kaitenStartDate));
            }

            if ((dbStartDate != null || kaitenStartDate != null) && intern.getIdGetCourse() == null) {
                getCourseService.signal(InternshipEntity.class, intern.getId());
            }

            Date kaitenInterviewDate = kaitenCardMapper.getDateFromKaitenDate(cardProperties.get(lastInterviewDatePropertyId));
            Date dbInterviewDate = intern.getDateOfLastInterview();

            if (kaitenInterviewDate != null && notSameDate(kaitenStartDate, dbInterviewDate)) {
                updateFields.put("dateOfLastInterview", DATE_FORMATTER.format(kaitenInterviewDate));
            }

            if (!updateFields.isEmpty()) {
                updateFields.put("id", intern.getId());
                updateFields.put("isNotified", false);
                databaseService.update(InternshipEntity.class, updateFields, intern.getId());
            }
        }
    }
}
