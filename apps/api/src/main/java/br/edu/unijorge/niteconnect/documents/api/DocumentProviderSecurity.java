package br.edu.unijorge.niteconnect.documents.api;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
/** The vendor JWT must be checked by the adapter, never parsed as an Entra user token. */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DocumentProviderSecurity {
    @Bean @Order(0) SecurityFilterChain documentProviderChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/v1/document-provider/**").csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.GET,"/api/v1/document-provider/sources/*","/api/v1/document-provider/inspection-sources/*").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/document-provider/cycles/*/callback","/api/v1/document-provider/inspection-cycles/*/callback").permitAll().anyRequest().denyAll()).build();
    }
}
