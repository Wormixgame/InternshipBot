package tech.reliab.kaiten.config;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tech.reliab.kaiten.Entity.Entity;
import tech.reliab.kaiten.Entity.InternshipEntity;
import tech.reliab.kaiten.Entity.PracticeEntity;

import java.util.Map;


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

    @Value("${tech.reliab.db.practice.url}")
    private String practiceEntitiesUrl;

    @Value("${tech.reliab.db.intern.url}")
    private String internEntitiesUrl;

    @Bean
    @Scope("singleton")
    public Map<Class<? extends Entity>, String> dbUrls() {
        return Map.of(InternshipEntity.class, internEntitiesUrl, PracticeEntity.class, practiceEntitiesUrl);
    }

    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    @Scope("singleton")
    public RestClient kaitenApiClient() {
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
    public WebClient dbApiClient(){
        return
                WebClient
                        .builder()
                        .baseUrl(dbMicroServiceBaseUrl)
                        .defaultHeader("Accept", "application/json")
                        .defaultHeader("Content-Type", "application/json")
                        .clientConnector(new ReactorClientHttpConnector(HttpClient.newConnection().compress(true)))
                        .build();
    }


    @Bean
    @Scope("singleton")
    public WebClient getCourseApiClient(){
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

    @Bean
    @Scope("singleton")
    public Map<Class<? extends Entity>, KaitenConfig.BoardInfo> boardInfos(){
        return Map.of(InternshipEntity.class,KAITEN_CONFIG().boardInfos.get("intern"),PracticeEntity.class ,KAITEN_CONFIG().boardInfos.get("practice"));
    }
}
