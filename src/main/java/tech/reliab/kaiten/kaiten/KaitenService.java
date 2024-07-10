package tech.reliab.kaiten.kaiten;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tech.reliab.kaiten.Entity.*;

import tech.reliab.kaiten.config.KaitenConfig;
import tech.reliab.kaiten.http.*;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@EnableScheduling
@Service
@RequiredArgsConstructor
public class KaitenService {
    private final RestClient KAITEN_SERVICE;
    private final WebClient DB_SERVICE;
    private final WebClient GET_COURSE_SERVICE;

    private final KaitenConfig KAITEN_CONFIG;
    private static KaitenConfig.BoardInfo INTERN_BOARD_INFO;
    private static KaitenConfig.BoardInfo PRACTICE_BOARD_INFO;
    private static final HashMap<Integer, HashMap<String, Integer>> KAITEN_SELECT_VALUES = new HashMap<>();

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DB_INTERN_LINK = "/internship_entity";
    private static final String DB_PRACTICE_LINK = "/practice_entity";

    @PostConstruct
    public void init(){
        INTERN_BOARD_INFO =  KAITEN_CONFIG.boardInfos.get("intern");
        PRACTICE_BOARD_INFO = KAITEN_CONFIG.boardInfos.get("practice");
    }

    public  ResponseEntity<String> sendSyncRequest(HttpMethod method, Object body, String uri){
        return KAITEN_SERVICE.method(method).uri(uri).body(body).retrieve().toEntity(String.class);
    }


    public Integer getLaneIDByInternshipDirection(KaitenConfig.BoardInfo boardInfo, String internshipDirection){
        if(boardInfo.lanes.containsKey(internshipDirection)){
            return boardInfo.lanes.get(internshipDirection);
        }else{
            log.warn("Lane with specified name not found: {}", internshipDirection);
            return boardInfo.unspecifiedDirectionLaneId;
        }
    }

    public Object transformDBValueToPropertyValue(Integer propertyId, String type, Object value){
        if (value == null){
            return null;
        }
        switch (type) {
            case "string", "link", "email", "phone":
                return value;
            case "select":
                Integer id = getSelectValueId(propertyId, (String) value);
                if (id == null){
                    return null;
                }
                // Кайтену нужен массив для Select поля.
                List<Integer> ids = new ArrayList<>();
                ids.add(id);
                return ids;
            case "date":
                HashMap<String, Object> dateMap = new HashMap<>();
                // Кайтену нужен JSON объект с такими полями.
                dateMap.put("date", DATE_FORMATTER.format(new Date((Long)value)));
                dateMap.put("time", null);
                dateMap.put("tzOffset", null);
                return dateMap;
            default:
                log.warn("Unsupported type: {}", type);
                return null;
        }
    }

    public HashMap<String, Object> getEntityPropertiesForHTTPRequest(KaitenConfig.BoardInfo boardInfo, Entity entity){
        Map entityProperties = new HashMap<>();
        try {
            entityProperties = MAPPER.readValue(MAPPER.writeValueAsString(entity), Map.class);
        }catch (Exception ex){
            log.error("Mapping exception:", ex);
            return null;
        }
        HashMap<String, Object> customProperties = new HashMap<>();
        for (Map.Entry<String, KaitenConfig.CustomProperty> entry: boardInfo.customProperties.entrySet()){
            if(entityProperties.containsKey(entry.getKey())){
                Object value = transformDBValueToPropertyValue(entry.getValue().getId(), entry.getValue().getType(), entityProperties.get(entry.getKey()));
                customProperties.put("id_" + entry.getValue().getId(), value);
            }
        }
        return customProperties;
    }

    public KaitenAddRequestBody fillKaitenAddRequestBody(KaitenConfig.BoardInfo boardInfo, Entity entity){
        return new KaitenAddRequestBody(
                boardInfo.getBoardId(),
                boardInfo.getCandidatesColumnId(),
                boardInfo.getCardTypeId(),
                getLaneIDByInternshipDirection(boardInfo, entity.getDirection()),
                entity.getTitle() != null ? entity.getTitle() : "Заглушка",
                entity.getDescription(),
                getEntityPropertiesForHTTPRequest(boardInfo, entity)
        );
    }

    private <T extends Entity> void addCardToKaiten(Class<T> entityType, KaitenConfig.BoardInfo boardInfo, String url){
        List<T> dbEntities = getEntitiesFromDB(entityType, url,true);
        if (dbEntities == null){
            log.warn("Can't reach DB service when adding card.");
            return;
        }
        for (T dbEntity : dbEntities) {
            KaitenAddRequestBody kaitenAddRequestBody = fillKaitenAddRequestBody(boardInfo, dbEntity);
            String responseBody = sendSyncRequest(HttpMethod.POST, kaitenAddRequestBody, url).getBody();
            TypeReference<KaitenGetResponseBody> typeReference = new TypeReference<>(){};
            try {
                KaitenGetResponseBody card = MAPPER.readValue(responseBody, typeReference);
                sendRequestToMicroService(DB_SERVICE, HttpMethod.PUT, url + "/" + dbEntity.getId(),
                        Map.of( "id", dbEntity.getId(), "idCardKaiten", card.getId()));
            }catch (Exception ex){
                log.error("Mapping exception:", ex);
                return;
            }
        }
    }

    @PostConstruct
    @Scheduled(cron = "0 */1 * * * *")
    public void addCardsToKaiten(){
        addCardToKaiten(InternshipEntity.class, INTERN_BOARD_INFO, DB_INTERN_LINK);
        addCardToKaiten(PracticeEntity.class, PRACTICE_BOARD_INFO, DB_PRACTICE_LINK);

    }

    public <T extends Entity> List<T> getEntitiesFromDB(Class<T> entityType, String url, boolean isKaitenNull) {
        return DB_SERVICE.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(entityType)
                .filter(entity -> entity.getIdCardKaiten() == null && isKaitenNull || entity.getIdCardKaiten() != null && !isKaitenNull)
                .collectList()
                .onErrorResume(e -> Mono.empty())
                .block();
    }


    private Date getDate(Object dateObject){
        if (dateObject == null){
            return null;
        }
        LinkedHashMap<String, Object> dateMap = (LinkedHashMap<String, Object>) dateObject;
        try {
            return DATE_FORMATTER.parse((String) dateMap.get("date"));
        }catch (Exception ex){
            log.warn("Error parsing date: {}", ex.getMessage());
            return null;
        }
    }

    private Map<Integer, KaitenInternCardUpdateInfo> filterInternCards(List<KaitenGetResponseBody> cards){
        String startDateMapKey = "id_" + INTERN_BOARD_INFO.serviceProperties.get("dateOfStartInternShipId");
        String lastInterviewDateMapKey = "id_" + INTERN_BOARD_INFO.serviceProperties.get("dateOfLastInterviewInternShipId");
        List<KaitenGetResponseBody> validCardsByType = cards.stream()
                .filter(card -> card.getType_id().equals(INTERN_BOARD_INFO.getCardTypeId())).toList();

        HashMap<Integer, KaitenInternCardUpdateInfo> validCards = new HashMap<>(validCardsByType.size());
        for(KaitenGetResponseBody validCardByType : validCardsByType){
            KaitenInternCardUpdateInfo validCard = new KaitenInternCardUpdateInfo();
            validCard.setId(validCardByType.getId());
            validCard.setStartDate(
                    getDate(validCardByType.getProperties().get(startDateMapKey))
            );
            validCard.setLastInterviewDate(
                    getDate(validCardByType.getProperties().get(lastInterviewDateMapKey))
            );
            validCards.put(validCard.getId(), validCard);
        }
        return validCards;
    }

    private HashMap<Integer, KaitenGetResponseBody> filterPracticeCards(List<KaitenGetResponseBody> cards){
        Integer columnId = PRACTICE_BOARD_INFO.serviceProperties.get("acceptedColumnId");
        List<KaitenGetResponseBody> filteredCards =  cards.stream().filter(card -> card.getType_id().equals(PRACTICE_BOARD_INFO.getCardTypeId())
                && card.getColumn_id().equals(columnId)).toList();
        HashMap<Integer, KaitenGetResponseBody> validCards = new HashMap<>(filteredCards.size());
        for (KaitenGetResponseBody practiceCard : filteredCards){
            validCards.put(practiceCard.getId(), practiceCard);
        }
        return validCards;
    }

    @PostConstruct
    @Scheduled(cron = "30 */1 * * * *")
    public void updatePracticants(){
        List<PracticeEntity> practiceEntities = getEntitiesFromDB(PracticeEntity.class, DB_PRACTICE_LINK, false);
        if (practiceEntities == null){
            log.warn("Can't reach DB service when updating practicants.");
            return;
        }
        Map<Integer, KaitenGetResponseBody> practiceCards = getPracticeCardsFromKaiten();
        for (PracticeEntity entity : practiceEntities) {
            Integer kaitenId = entity.getIdCardKaiten();
            if (!practiceCards.containsKey(kaitenId) || entity.getIdGetCourse() != null){
                continue;
            }

            sendRequestToMicroService(GET_COURSE_SERVICE,
                    HttpMethod.POST,
                    "/initDatabase" + "?id=" + entity.getId() + "&" + "isInternship=" + false,
                    Map.of());
        }
    }


    private Map<Integer, KaitenGetResponseBody> getPracticeCardsFromKaiten(){
        String responseBody = sendSyncRequest(HttpMethod.GET, "", "latest/cards").getBody();

        TypeReference<List<KaitenGetResponseBody>> typeReference = new TypeReference<>(){};
        try {
            List<KaitenGetResponseBody> kaitenCards = MAPPER.readValue(responseBody, typeReference);
            return filterPracticeCards(kaitenCards);
        }catch (Exception ex) {
            log.error("Mapping exception:", ex);
            return null;
        }
    }
    private Map<Integer, KaitenInternCardUpdateInfo> getInternCardsFromKaiten(){
        String responseBody = sendSyncRequest(HttpMethod.GET, "", "latest/cards").getBody();

        TypeReference<List<KaitenGetResponseBody>> typeReference = new TypeReference<>(){};
        try {
            List<KaitenGetResponseBody> kaitenCards = MAPPER.readValue(responseBody, typeReference);
            return filterInternCards(kaitenCards);
        }catch (Exception ex) {
            log.error("Mapping exception:", ex);
            return null;
        }
    }

    private void sendRequestToMicroService(WebClient microService, HttpMethod method, String uri, Map<String, Object> info){
        Mono<Void> updateRequest = microService.method(method).uri(uri).contentType(MediaType.APPLICATION_JSON).body(Mono.just(info), Map.class).retrieve().bodyToMono(Void.class);
        updateRequest.subscribe(
                null, // On Success
                error -> log.warn("Error sending request:", error) // On Error
        );
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


    private synchronized Integer getSelectValueId(Integer propertyId, String selectValue){
        if (!KAITEN_SELECT_VALUES.containsKey(propertyId)){
            updateSelectValuesByPropertyId(propertyId);
        }
        if (KAITEN_SELECT_VALUES.get(propertyId).containsKey(selectValue)){
            return KAITEN_SELECT_VALUES.get(propertyId).get(selectValue);
        }

        if (!KAITEN_CONFIG.selectPropertiesConfig.get(propertyId).getAllowCreation()){
            return null;
        }

        ResponseEntity<String> createRequestResult = sendSyncRequest(HttpMethod.POST, Map.of("value", selectValue),"latest/company/custom-properties/" + propertyId + "/select-values");
        if (createRequestResult.getStatusCode().is2xxSuccessful()){
            try {
                KaitenSelectValuesResponseBody selectValuesResponseBody = MAPPER.readValue(createRequestResult.getBody(), new TypeReference<>() {});
                KAITEN_SELECT_VALUES.get(propertyId).put(selectValuesResponseBody.getValue(), selectValuesResponseBody.getId());
            }catch (Exception ex){
                return null;
            }
        }else{
            updateSelectValuesByPropertyId(propertyId);
        }

        return KAITEN_SELECT_VALUES.get(propertyId).get(selectValue);
    }
    private synchronized void updateSelectValuesByPropertyId(Integer propertyId){
        ResponseEntity<String> response = sendSyncRequest(HttpMethod.GET, "", "/latest/company/custom-properties/" + propertyId + "/select-values");
        String body = response.getBody();
        List<KaitenSelectValuesResponseBody> selectValuesList;
        try {
             selectValuesList = MAPPER.readValue(body, new TypeReference<>() {
            });
        }catch (Exception ex){
            return;
        }

        if (KAITEN_SELECT_VALUES.containsKey(propertyId)){
            KAITEN_SELECT_VALUES.get(propertyId).clear();
        }else{
            KAITEN_SELECT_VALUES.put(propertyId, new HashMap<>());
        }

        Map<String, Integer> selectValues = KAITEN_SELECT_VALUES.get(propertyId);
        for (KaitenSelectValuesResponseBody selectValue : selectValuesList){
            selectValues.put(selectValue.getValue(), selectValue.getId());
        }
    }

    @PostConstruct
    @Scheduled(cron = "0 */1 * * * *")
    public void updateInternDB(){
        List<InternshipEntity> internsWithCards = getEntitiesFromDB(InternshipEntity.class, DB_INTERN_LINK,false);
        if (internsWithCards == null){
            log.warn("Can't reach DB service when updating interns.");
            return;
        }
        Map<Integer, KaitenInternCardUpdateInfo> kaitenCards = getInternCardsFromKaiten();

        for (InternshipEntity intern : internsWithCards) {
            Integer kaitenId = intern.getIdCardKaiten();
            if (!kaitenCards.containsKey(kaitenId)){
                continue;
            }
            HashMap<String, Object> updateFields = new HashMap<>();

            KaitenInternCardUpdateInfo kaitenInfo = kaitenCards.get(kaitenId);

            Date kaitenStartDate = kaitenInfo.getStartDate();
            Date dbStartDate = intern.getDateOfStartInternship();

            if (kaitenStartDate != null && notSameDate(kaitenStartDate, dbStartDate)){
                updateFields.put("dateOfStartInternship", DATE_FORMATTER.format(kaitenStartDate));
                if (dbStartDate == null) {
                    sendRequestToMicroService(GET_COURSE_SERVICE,
                            HttpMethod.POST,
                            "/initDatabase" + "?id=" + intern.getId() + "&" + "isInternship=" + true,
                            Map.of());
                }
            }

            Date kaitenInterviewDate = kaitenInfo.getLastInterviewDate();
            Date dbInterviewDate = intern.getDateOfLastInterview();

            if (kaitenInterviewDate != null && notSameDate(kaitenStartDate, dbInterviewDate)){
                updateFields.put("dateOfLastInterview", DATE_FORMATTER.format(kaitenInterviewDate));
            }

            if (!updateFields.isEmpty()){
                updateFields.put("id", intern.getId());
                updateFields.put("isNotified", false);
                sendRequestToMicroService(DB_SERVICE, HttpMethod.PUT, DB_INTERN_LINK + "/" + intern.getId(), updateFields);
            }
        }
    }
}
