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
        public final Integer unspecifiedDirectionLaneId;
        public final HashMap<String, Integer> lanes;
        public final HashMap<String, CustomProperty> customProperties;
        public final HashMap<String, Integer> serviceProperties;
    }
    public KaitenConfig(XMLConfigStructure configStructure){
        HashMap<Integer, CustomProperty> globalCustomProperties = new HashMap<>();
        for (XMLConfigStructure.XMLCustomProperty customProperty: configStructure.getCustomProperties()){
            globalCustomProperties.put(customProperty.getId(), new CustomProperty(customProperty.getId(), customProperty.getType()));
        }
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
                customProperties.put(boardCustomProperty.getDatabaseName(), globalCustomProperties.get(boardCustomProperty.getId()));
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
                            xmlBoardInfo.getUnspecifiedLaneId(),
                            lanes,
                            customProperties,
                            serviceProperties
                    )
            );
        }
        globalCustomProperties.clear();
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
