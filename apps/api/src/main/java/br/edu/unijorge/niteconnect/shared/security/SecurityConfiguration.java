package br.edu.unijorge.niteconnect.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectProvider<JwtDecoder> decoders) throws Exception {
        JwtDecoder decoder = decoders.getIfAvailable();
        AuthorizationManager<RequestAuthorizationContext> authenticatedJwt = (authentication, context) ->
                new AuthorizationDecision(decoder != null && authentication.get().isAuthenticated()
                        && authentication.get() instanceof JwtAuthenticationToken);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects", "/api/v1/projects/*", "/api/v1/me/projects",
                                "/api/v1/me/projects/history", "/api/v1/me/projects/leadership/history", "/api/v1/projects/*/history", "/api/v1/projects/*/members",
                                "/api/v1/projects/*/members/history", "/api/v1/projects/*/leadership", "/api/v1/projects/*/leadership/history",
                                "/api/v1/projects/*/member-candidates",
                                "/api/v1/proposals", "/api/v1/proposals/*").access(authenticatedJwt)
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects", "/api/v1/projects/*/members",
                                "/api/v1/projects/*/leadership-transfers", "/api/v1/proposals",
                                "/api/v1/proposals/*/decisions", "/api/v1/proposals/*/convert").access(authenticatedJwt)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/projects/*", "/api/v1/projects/*/status",
                                "/api/v1/proposals/*").access(authenticatedJwt)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/projects/*/members/*").access(authenticatedJwt)
                        .requestMatchers(HttpMethod.GET, "/api/v1/me", "/api/v1/members", "/api/v1/members/*",
                                "/api/v1/members/*/history", "/api/v1/invitations", "/api/v1/audit-events")
                            .access(authenticatedJwt)
                        .requestMatchers(HttpMethod.POST, "/api/v1/members", "/api/v1/invitations",
                                "/api/v1/invitations/*/accept", "/api/v1/members/*/roles")
                            .access(authenticatedJwt)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/members/*", "/api/v1/members/*/status")
                            .access(authenticatedJwt)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/members/*/roles/*", "/api/v1/invitations/*")
                            .access(authenticatedJwt)
                        .anyRequest().denyAll());
        if (decoder != null) {
            http.oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder)));
        }
        return http.build();
    }
}
