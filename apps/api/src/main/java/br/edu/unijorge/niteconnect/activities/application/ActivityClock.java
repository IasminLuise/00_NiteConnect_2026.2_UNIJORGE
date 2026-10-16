package br.edu.unijorge.niteconnect.activities.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ActivityClock {
    @Bean @ConditionalOnMissingBean(Clock.class)
    Clock domainClock(@Value("${nite.activities.zone:America/Bahia}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
