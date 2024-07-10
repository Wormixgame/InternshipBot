package tech.reliab.kaiten.config;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;


@Data
@XmlRootElement(name = "config")
@XmlAccessorType(XmlAccessType.FIELD)
public class XMLConfigStructure {
    @XmlElement(name = "board-config")
    private List<XMLBoardConfig> boardConfigs;
    @XmlElementWrapper(name = "custom-properties")
    @XmlElement(name = "custom-property")
    private List<XMLCustomProperty> customProperties;
    @XmlElementWrapper(name = "select-custom-properties-config")
    @XmlElement(name = "select-custom-property-config")
    private List<XMLSelectCustomPropertyConfig> selectCustomPropertiesConfig;


    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XMLBoardConfig {
        @XmlElement(name = "board-info")
        private XMLBoardInfo boardInfo;

        @XmlElementWrapper(name = "board-lanes")
        @XmlElement(name = "board-lane")
        private List<XMLBoardLane> boardLanes;

        @XmlElementWrapper(name = "board-custom-properties")
        @XmlElement(name = "board-custom-property")
        private List<XMLBoardCustomProperty> boardCustomProperties;

        @XmlElementWrapper(name = "board-service-properties")
        @XmlElement(name = "board-service-property")
        private List<XMLBoardServiceProperty> boardServiceProperties;
    }


    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XMLBoardInfo {
        @XmlElement(name = "board-name")
        private String boardName;
        @XmlElement(name = "board-id")
        private int boardId;
        @XmlElement(name = "card-type-id")
        private int cardTypeId;
        @XmlElement(name = "candidate-column-id")
        private int candidateColumnId;
        @XmlElement(name = "unspecified-lane-id")
        private int unspecifiedLaneId;
    }


    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XMLBoardLane {
        private int id;
        @XmlElement(name = "site-value")
        private String siteValue;
    }


    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XMLBoardCustomProperty {
        private int id;
        @XmlElement(name = "database-name")
        private String databaseName;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XMLCustomProperty {
        private int id;
        private String type;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XMLBoardServiceProperty {
        private int id;
        private String name;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XMLSelectCustomPropertyConfig {
        private int id;
        @XmlElement(name = "allow-creation")
        private boolean allowCreation;
    }
}
