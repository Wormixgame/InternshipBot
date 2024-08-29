package tech.reliab.kaiten.kaiten;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import tech.reliab.kaiten.Entity.Entity;
import tech.reliab.kaiten.config.KaitenConfig;
import tech.reliab.kaiten.http.KaitenCard;
import tech.reliab.kaiten.http.KaitenSelectValue;
import tech.reliab.kaiten.services.KaitenService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@RequiredArgsConstructor
public class KaitenCardMapper {
    private final KaitenConfig KAITEN_CONFIG;
    private final KaitenService kaitenService;
    private final Map<Class<? extends Entity>, KaitenConfig.BoardInfo> boardInfos;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_OF_BIRTH_FORMATTER = new SimpleDateFormat("dd.MM.yyyy");
    private static final Logger log = LoggerFactory.getLogger(KaitenCardMapper.class);
    private static final HashMap<Integer, HashMap<String, Integer>> kaitenIdsOfSelectValues = new HashMap<>();
    private static final HashMap<Integer, HashMap<Integer, String>> kaitenSelectValuesById = new HashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Map<String, Object> getKaitenDateFromTimeStamp(Long dateValue){
        if (dateValue == null){
            return null;
        }
        HashMap<String, Object> dateMap = new HashMap<>();
        dateMap.put("date", DATE_FORMATTER.format(new Date(dateValue)));
        dateMap.put("time", null);
        dateMap.put("tzOffset", null);
        return dateMap;
    }

    public String getStringDateFromKaitenDate(Object dateMap){
        if (dateMap == null){
            return null;
        }
        LinkedHashMap<String, Object> dateMapLinkedHashMap = (LinkedHashMap<String, Object>) dateMap;
        return (String) dateMapLinkedHashMap.get("date");
    }

    public Date getDateFromKaitenDate(Object dateMap){
        if (dateMap == null){
            return null;
        }

        LinkedHashMap<String, Object> dateMapLinkedHashMap = (LinkedHashMap<String, Object>) dateMap;
        try{
            return DATE_FORMATTER.parse((String) dateMapLinkedHashMap.get("date"));
        }catch (ParseException ex){
            log.error("Не удалось преобразовать дату {} в нужный формат", dateMapLinkedHashMap.get("date"));
            return null;
        }
    }

    private String getPhoneNumber(String phoneNumber){
        if (phoneNumber == null){
            return null;
        }

        if (phoneNumber.startsWith("+") || phoneNumber.startsWith("8")){
            return phoneNumber;
        }
        return "+" + phoneNumber;
    }

    public Object getPropertyValueForKaiten(Integer propertyId, String propertyType, Object propertyValue) {
        if (propertyValue == null){
            return null;
        }
        return switch (propertyType) {
            case "string", "link", "email" -> propertyValue;
            case "phone" ->
                    getPhoneNumber((String) propertyValue);
            case "select" ->
                // Кайтену нужен массив для Select поля.
                    getIdFromSelectValue(propertyId, (String) propertyValue);
            case "date" ->
                // Кайтену нужен JSON объект с такими полями.
                    getKaitenDateFromTimeStamp((Long) propertyValue);
            default -> {
                log.warn("Unsupported type: {}", propertyType);
                yield null;
            }
        };
    }

    public Object getPropertyValueForDatabase(Integer propertyId, String propertyType, Object propertyValue) {
        if (propertyValue == null){
            return null;
        }

        return switch (propertyType) {
            case "string", "link", "email" -> propertyValue;
            case "phone" ->
                    getPhoneNumber((String) propertyValue);
            case "select" ->
                // Кайтену нужен массив для Select поля.
                    ((ArrayList<Integer>)propertyValue).size() != 0 ?getSelectValueFromId(propertyId, ((ArrayList<Integer>) propertyValue).getFirst()) : null;
            case "date" ->
                // Кайтену нужен JSON объект с такими полями.
                    getStringDateFromKaitenDate(propertyValue);
            default -> {
                log.warn("Unsupported type: {}", propertyType);
                yield null;
            }
        };
    }

    private synchronized String getSelectValueFromId(Integer propertyId, Integer valueId){
        if (!kaitenSelectValuesById.containsKey(propertyId) || !kaitenSelectValuesById.get(propertyId).containsKey(valueId)){
            updateSelectValuesByPropertyId(propertyId);
        }

        if (!kaitenSelectValuesById.get(propertyId).containsKey(valueId)){
            log.error("Неизвестная ошибка, возможно значение было удалено в момент получения карточки.");
            return "Другое";
        }

        return kaitenSelectValuesById.get(propertyId).get(valueId);
    }

    private synchronized List<Integer> getIdFromSelectValue(Integer propertyId, String selectValue){
        String originalSelectValue = selectValue;
        String lowerCaseSelectValue = selectValue.toLowerCase();
        // Маппинг значения
        if (KAITEN_CONFIG.selectPropertiesConfig.get(propertyId).getMapping().containsKey(lowerCaseSelectValue)) {
            originalSelectValue = (KAITEN_CONFIG.selectPropertiesConfig.get(propertyId).getMapping().get(lowerCaseSelectValue));
            lowerCaseSelectValue = originalSelectValue.toLowerCase();
        }

        // Не добавлены select values для данной property
        if (!kaitenIdsOfSelectValues.containsKey(propertyId) || !kaitenIdsOfSelectValues.get(propertyId).containsKey(lowerCaseSelectValue)){
            updateSelectValuesByPropertyId(propertyId);
        }

        // Если нет нужного select value
        if (!kaitenIdsOfSelectValues.get(propertyId).containsKey(lowerCaseSelectValue)){
            if (!KAITEN_CONFIG.selectPropertiesConfig.get(propertyId).getAllowCreation()){
                return null;
            }
            createSelectValue(propertyId, originalSelectValue);
        }

        if (!kaitenIdsOfSelectValues.get(propertyId).containsKey(lowerCaseSelectValue)){
            return null;
        }

        List<Integer> ids = new ArrayList<>();
        ids.add(kaitenIdsOfSelectValues.get(propertyId).get(lowerCaseSelectValue));
        return ids;
    }

    private synchronized void createSelectValue(Integer propertyId, String selectValue){
        try{
            KaitenSelectValue kaitenSelectValue = kaitenService.updateSelectValues(propertyId, selectValue);
            kaitenIdsOfSelectValues.get(propertyId).put(kaitenSelectValue.getValue().toLowerCase(), kaitenSelectValue.getId());
            kaitenSelectValuesById.get(propertyId).put(kaitenSelectValue.getId(), kaitenSelectValue.getValue().toLowerCase());
        }catch (HttpClientErrorException ex){
            log.error(ex.getMessage());
        }
    }

    private synchronized void updateSelectValuesByPropertyId(Integer propertyId){
        if (kaitenSelectValuesById.containsKey(propertyId)){
            kaitenSelectValuesById.get(propertyId).clear();
            kaitenIdsOfSelectValues.get(propertyId).clear();
        }else{
            kaitenSelectValuesById.put(propertyId, new HashMap<>());
            kaitenIdsOfSelectValues.put(propertyId, new HashMap<>());
        }

        List<KaitenSelectValue> selectValuesList = kaitenService.getSelectValues(propertyId);
        Map<String, Integer> selectIds = kaitenIdsOfSelectValues.get(propertyId);
        Map<Integer, String> selectValues = kaitenSelectValuesById.get(propertyId);
        for (KaitenSelectValue selectValue : selectValuesList.stream().filter(selectValue -> selectValue.getCondition().equals("active")).toList()){
            selectIds.put(selectValue.getValue().toLowerCase(), selectValue.getId());
            selectValues.put(selectValue.getId(), selectValue.getValue());
        }
        for (KaitenSelectValue selectValue : selectValuesList.stream().filter(selectValue -> !selectValue.getCondition().equals("active")).toList()){
            selectValues.put(selectValue.getId(), selectValue.getValue());
        }
    }

    public <T extends Entity> Map<String, Object> mapValuesForKaiten(T entity){
        KaitenConfig.BoardInfo boardInfo  = boardInfos.get(entity.getClass());
        Map<String, Object> entityProperties;
        try {
            entityProperties = MAPPER.readValue(MAPPER.writeValueAsString(entity), new TypeReference<>() {});
        }catch (Exception ex){
            log.error("Mapping exception:", ex);
            return null;
        }
        HashMap<String, Object> customProperties = new HashMap<>();
        for (Map.Entry<String, KaitenConfig.CustomProperty> entry: boardInfo.customProperties.entrySet()){
            if(entityProperties.containsKey(entry.getKey())){
                if (entry.getKey().equals("dateOfBirth")){
                    try {
                        Date dbDate = new Date((Long)entityProperties.get(entry.getKey()));
                        String kaitenDate = DATE_OF_BIRTH_FORMATTER.format(dbDate);
                        customProperties.put("id_" + entry.getValue().getId(), kaitenDate);
                    }catch (ClassCastException ex){
                        log.warn("Кто придумал использовать строковое поле для записи даты в Кайтене???", ex);
                    }
                }else {
                    Object value = getPropertyValueForKaiten(entry.getValue().getId(), entry.getValue().getType(), entityProperties.get(entry.getKey()));
                    customProperties.put("id_" + entry.getValue().getId(), value);
                }
            }
        }
        return customProperties;
    }

    public <T extends Entity> Map<String, Object> mapValuesForDatabase(Class<T> entityType, KaitenCard card) {
        KaitenConfig.BoardInfo boardInfo  = boardInfos.get(entityType);
        HashMap<String, Object> customProperties = new HashMap<>();
        for (Map.Entry<String, KaitenConfig.CustomProperty> entry: boardInfo.customProperties.entrySet()){
            KaitenConfig.CustomProperty property =  entry.getValue();
            Object kaitenPropertyValue = card.getProperties().get("id_" + property.getId());
            if (entry.getKey().equals("dateOfBirth")){
                try{
                    String dbDate;
                    if (kaitenPropertyValue == null){
                        dbDate = DATE_FORMATTER.format(new Date());
                    }else {
                        Date kaitenDate = DATE_OF_BIRTH_FORMATTER.parse((String) kaitenPropertyValue);
                        dbDate = DATE_FORMATTER.format(kaitenDate);
                    }
                    customProperties.put(entry.getKey(), dbDate);
                }catch (ParseException ex){
                    log.warn("Кто придумал использовать строковое поле для записи даты в Кайтене??? Маппинг карточки из Кайтена.", ex);
                }
            }else{
                Object databasePropertyValue = getPropertyValueForDatabase(property.getId(), property.getType(), kaitenPropertyValue);
                if (databasePropertyValue != null) {
                    customProperties.put(entry.getKey(), databasePropertyValue);
                }
            }
        }
        return customProperties;
    }
}
