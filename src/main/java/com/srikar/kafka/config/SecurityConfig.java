package com.srikar.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String KEYCLOAK_CLIENT_ID = "secauth-api";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // ----------------------------
                        // Public endpoints
                        // ----------------------------
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ----------------------------
                        // Kafka Cluster inventory (DB metadata)
                        // ----------------------------

                        // ✅ Snapshot: allow non-admin roles to view (meta + live health)
                        .requestMatchers(HttpMethod.GET, "/api/kafka/clusters/*/snapshot").hasAnyRole(
                                "KAFKA_ADMIN", "KAFKA_DEV", "KAFKA_SUPP", "KAFKA_TEST"
                        )

                        // ✅ Inventory CRUD remains ADMIN-only
                        .requestMatchers(HttpMethod.POST, "/api/kafka/clusters/upsert").hasRole("KAFKA_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/kafka/clusters/**").hasRole("KAFKA_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/kafka/clusters/**").hasRole("KAFKA_ADMIN")

                        // ----------------------------
                        // Kafka Cluster runtime health (AdminClient probe)
                        // ----------------------------
                        .requestMatchers(HttpMethod.GET, "/api/kafka/cluster/status").hasAnyRole(
                                "KAFKA_ADMIN", "KAFKA_DEV", "KAFKA_SUPP", "KAFKA_TEST"
                        )

                        // ----------------------------
                        // Topics
                        // ----------------------------
                        .requestMatchers(HttpMethod.GET, "/api/kafka/topics").hasAnyRole(
                                "KAFKA_ADMIN", "KAFKA_DEV", "KAFKA_SUPP", "KAFKA_TEST"
                        )
                        .requestMatchers("/api/kafka/topics/**").hasRole("KAFKA_ADMIN")

                        // ----------------------------
                        // Connectors / Process
                        // ----------------------------
                        .requestMatchers("/api/kafka/connectors/**").hasRole("KAFKA_ADMIN")
                        .requestMatchers("/api/kafka/process/**").hasRole("KAFKA_ADMIN")

                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess == null) return Collections.emptyList();

            Object clientAccessObj = resourceAccess.get(KEYCLOAK_CLIENT_ID);
            if (!(clientAccessObj instanceof Map<?, ?> clientAccess)) return Collections.emptyList();

            Object rolesObj = clientAccess.get("roles");
            if (!(rolesObj instanceof Collection<?> roles)) return Collections.emptyList();

            return roles.stream()
                    .map(String::valueOf)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet());
        });

        return converter;
    }
}
