package tech.reliab.kaiten.kaiten;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import tech.reliab.kaiten.config.KaitenConfig;
import tech.reliab.kaiten.config.XMLConfigStructure;


@Slf4j
@Configuration
public class KaitenContextConfiguration {

    @Value("${tech.reliab.kaiten.api.baseUrl}")
    private String kaitenApiBaseUrl;
    @Value("${tech.reliab.kaiten.api.key}")
    private String kaitenApiKey;
    @Value("${tech.reliab.kaiten.api.config}")
    private String kaitenApiConfig;

    @Value("${tech.reliab.db.baseUrl}")
    private String dbMicroServiceBaseUrl;

    @Value("${tech.reliab.getCourse.baseUrl}")
    private String getCourseMicroserviceBaseUrl;


    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    @Scope("singleton")
    public RestClient KAITEN_SERVICE() {
        return
                RestClient
                        .builder()
                        .baseUrl(kaitenApiBaseUrl)
                        .defaultHeader("Accept", "application/json")
                        .defaultHeader("Content-Type", "application/json")
                        .defaultHeader("Authorization", "Bearer " + kaitenApiKey).build();
    }

    @Bean
    @Scope("singleton")
    public WebClient DB_SERVICE(){
        return
                WebClient
                        .builder()
                        .baseUrl(dbMicroServiceBaseUrl)
                        .defaultHeader("Accept", "application/json")
                        .defaultHeader("Content-Type", "application/json")
                        .build();
    }

    @Bean
    @Scope("singleton")
    public WebClient GET_COURSE_SERVICE(){
        return
                WebClient
                        .builder()
                        .baseUrl(getCourseMicroserviceBaseUrl)
                        .build();
    }

    @Bean
    @Scope("singleton")
    public KaitenConfig KAITEN_CONFIG(){
        try{
            JAXBContext jaxbContext = JAXBContext.newInstance(XMLConfigStructure.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            XMLConfigStructure config = (XMLConfigStructure) unmarshaller.unmarshal(resourceLoader.getResource("classpath:" + kaitenApiConfig).getInputStream());
            return new KaitenConfig(config);
        }catch (Exception ex){
            log.error("KaitenConfig error", ex);
            return null;
        }
    }

}
