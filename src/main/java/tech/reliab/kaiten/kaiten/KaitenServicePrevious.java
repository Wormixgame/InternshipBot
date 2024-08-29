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
    private final tech.reliab.kaiten.services.KaitenService kaitenService;
    private final tech.reliab.kaiten.services.DatabaseService databaseService;
    private final tech.reliab.kaiten.services.GetCourseService getCourseService;
    private final tech.reliab.kaiten.kaiten.KaitenCardMapper kaitenCardMapper;
    private final Map<Class<? extends Entity>, KaitenConfig.BoardInfo> boardInfos;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    public Integer getLaneIDByInternshipDirection(KaitenConfig.BoardInfo boardInfo, String internshipDirection) throws IllegalArgumentException{
        if(boardInfo.lanes.containsKey(internshipDirection)){
            return boardInfo.lanes.get(internshipDirection);
        }else{
            throw new IllegalArgumentException("Lane with specified name (" + internshipDirection + ") not found");
        }
    }

    public String getInternshipDirectionByLaneID(KaitenConfig.BoardInfo boardInfo, Integer laneId){
        for (Map.Entry<String, Integer> lane : boardInfo.lanes.entrySet()){
            if (lane.getValue().equals(laneId)){
                return lane.getKey();
            }
        }
        return null;
    }

    public KaitenCard fillKaitenAddRequestBody(KaitenConfig.BoardInfo boardInfo, Entity entity) throws IllegalArgumentException{
        Integer laneId = getLaneIDByInternshipDirection(boardInfo, entity.getDirection());
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

    private <T extends Entity> void addCardToKaiten(Class<T> entityType){
        List<T> dbEntities;
        KaitenConfig.BoardInfo boardInfo = boardInfos.get(entityType);
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
    @Scheduled(cron = "0 */2 * * * *")
    public void addCardsToKaiten(){
        addCardToKaiten(InternshipEntity.class);
        addCardToKaiten(PracticeEntity.class);
    }


    @PostConstruct
    @Scheduled(cron = "30 */2 * * * *")
    public void updatePractice(){
        List<PracticeEntity> practiceEntities;
        try {
            practiceEntities = databaseService.getEntities(PracticeEntity.class).stream().filter(entity -> entity.getIdCardKaiten() == null).toList();
        }catch (HttpClientErrorException ex){
            log.error(ex.getMessage());
            return;
        }

        KaitenConfig.BoardInfo practiceBoardInfo = boardInfos.get(PracticeEntity.class);
        KaitenCardBatch cardBatch = kaitenService.getCardsFromKaiten(practiceBoardInfo);
        Map<Integer, KaitenCard> practiceCards = cardBatch.getKaitenCardMap(new KaitenFilterByTypeAndAccepted(practiceBoardInfo));
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
    @Scheduled(cron = "0 */5 * * * *")
    public void deleteEntriesWithDeletedKaitenCard(){
        KaitenCardBatch kaitenCards = kaitenService.getCardsFromKaiten(boardInfos.get(PracticeEntity.class));
        Map<Integer, KaitenCard> cardMap = kaitenCards.getKaitenCardMap();
        deleteEntries(PracticeEntity.class, cardMap);

        kaitenCards = kaitenService.getCardsFromKaiten(boardInfos.get(InternshipEntity.class));
        cardMap = kaitenCards.getKaitenCardMap();
        deleteEntries(InternshipEntity.class, cardMap);

    }

    private <T extends Entity> void updateDatabaseEntities(Class<T> entityType, KaitenCardBatch kaitenCardBatch){
        ObjectMapper mapper = new ObjectMapper();
        List<T> dbEntities = databaseService.getEntities(entityType);
        KaitenConfig.BoardInfo boardInfo = boardInfos.get(entityType);
        Set<String> tgUrls = dbEntities.stream().map(Entity::getTgUrl).collect(Collectors.toSet());
        Set<Integer> kaitenIds = dbEntities.stream().map(Entity::getIdCardKaiten).filter(Objects::nonNull).collect(Collectors.toSet());

        List<KaitenCard> entityCards = kaitenCardBatch.getCards(new KaitenFilterByType(boardInfo)).stream().filter(kaitenCard -> !kaitenIds.contains(kaitenCard.getId())).toList();
        List<String> cardsIds = entityCards.stream().map(card -> card.getId().toString()).toList();
        log.info("Карточки ({}), которые будут добавлены в бд: ({})", entityType, String.join(", ", cardsIds));
        for (KaitenCard entityCard : entityCards){
            Map<String, Object> map = kaitenCardMapper.mapValuesForDatabase(entityType, entityCard);
            String tgUrl = (String) map.get("tgUrl");

            // Проверяем, существует ли URL телеграмм в базе данных
            if (tgUrls.contains(tgUrl)) {
                log.info("URL телеграмма ({}) уже существует в базе данных. Запись не будет добавлена.", tgUrl);
                continue;
            }

            map.put("idCardKaiten", entityCard.getId());
            map.put("fullName", entityCard.getTitle());
            String laneName = getInternshipDirectionByLaneID(boardInfo, entityCard.getLaneId());
            if (laneName != null) {
                map.put("internshipDirection", laneName);
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
        KaitenCardBatch kaitenCards = kaitenService.getCardsFromKaiten(boardInfos.get(PracticeEntity.class));
        updateDatabaseEntities(PracticeEntity.class, kaitenCards);

        kaitenCards = kaitenService.getCardsFromKaiten(boardInfos.get(InternshipEntity.class));
        updateDatabaseEntities(InternshipEntity.class, kaitenCards);

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
        KaitenConfig.BoardInfo internBoardInfo = boardInfos.get(InternshipEntity.class);
        String lastInterviewDatePropertyId = "id_" + internBoardInfo.serviceProperties.get("dateOfLastInterviewId");
        String startDatePropertyId = "id_" + internBoardInfo.serviceProperties.get("dateOfStartInternShipId");

        KaitenCardBatch cardBatch = kaitenService.getCardsFromKaiten(boardInfos.get(InternshipEntity.class));
        Map<Integer, KaitenCard> kaitenCards = cardBatch.getKaitenCardMap(new KaitenFilterByTypeAndAccepted(internBoardInfo));


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
