package tech.reliab.kaiten.config;

import lombok.AllArgsConstructor;
import lombok.Data;


import java.util.HashMap;


@AllArgsConstructor
public class KaitenConfig {

    @Data
    @AllArgsConstructor
    public static class BoardInfo{
        public final Integer boardId;
        public final Integer cardTypeId;
        public final Integer candidatesColumnId;
        public final HashMap<String, Integer> lanes;
        public final HashMap<String, CustomProperty> customProperties;
        public final HashMap<String, Integer> serviceProperties;
    }
    public KaitenConfig(XMLConfigStructure configStructure){
        selectPropertiesConfig = new HashMap<>();
        for (XMLConfigStructure.XMLSelectCustomPropertyConfig selectCustomPropertyConfig :configStructure.getSelectCustomPropertiesConfig()){
            selectPropertiesConfig.put(selectCustomPropertyConfig.getId(), new SelectProperty(selectCustomPropertyConfig.isAllowCreation()));
        }

        boardInfos = new HashMap<>();
        for (XMLConfigStructure.XMLBoardConfig boardConfig :configStructure.getBoardConfigs()){
            HashMap<String, Integer> lanes = new HashMap<>();
            HashMap<String, CustomProperty> customProperties = new HashMap<>();
            HashMap<String, Integer> serviceProperties = new HashMap<>();

            for (XMLConfigStructure.XMLBoardLane boardLane : boardConfig.getBoardLanes()){
                lanes.put(boardLane.getSiteValue(), boardLane.getId());
            }

            for (XMLConfigStructure.XMLBoardCustomProperty boardCustomProperty :boardConfig.getBoardCustomProperties()){
                customProperties.put(boardCustomProperty.getDatabaseName(), new CustomProperty(boardCustomProperty.getId(), boardCustomProperty.getType()));
            }

            for (XMLConfigStructure.XMLBoardServiceProperty boardServiceProperty :boardConfig.getBoardServiceProperties()){
                serviceProperties.put(boardServiceProperty.getName(), boardServiceProperty.getId());
            }

            XMLConfigStructure.XMLBoardInfo xmlBoardInfo = boardConfig.getBoardInfo();
            boardInfos.put(
                    xmlBoardInfo.getBoardName(),
                    new BoardInfo(
                            xmlBoardInfo.getBoardId(),
                            xmlBoardInfo.getCardTypeId(),
                            xmlBoardInfo.getCandidateColumnId(),
                            lanes,
                            customProperties,
                            serviceProperties
                    )
            );
        }
    }

    @Data
    @AllArgsConstructor
    public static class CustomProperty{
        public Integer id;
        public String type;
    }

    public HashMap<String, BoardInfo> boardInfos;

    public HashMap<Integer, SelectProperty> selectPropertiesConfig;

    @Data
    @AllArgsConstructor
    public static class SelectProperty{
        private Boolean allowCreation;
    }
}
