package tech.reliab.kaiten.kaiten;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class KaitenPipelines {

    @PostConstruct
    public void updateDBFromKaiten(){
    }
}
