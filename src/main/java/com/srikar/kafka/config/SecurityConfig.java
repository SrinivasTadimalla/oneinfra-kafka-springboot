package com.srikar.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
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

    // ✅ Must match your Keycloak client id where roles are created
    private static final String KEYCLOAK_CLIENT_ID = "secauth-api";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // CORS
                .cors(Customizer.withDefaults())

                // JWT APIs are stateless
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ✅ Your new roles
                        .requestMatchers("/topics/**").hasRole("KAFKA_ADMIN")
                        .requestMatchers("/process/**").hasRole("KAFKA_ADMIN")
                        .requestMatchers("/clusters/**").hasRole("KAFKA_ADMIN")
                        .requestMatchers("/connectors/**").hasRole("KAFKA_ADMIN")

                        // example: allow DEV also
                        // .requestMatchers(HttpMethod.GET, "/topics/**").hasAnyRole("KAFKA_ADMIN", "KAFKA_DEV")

                        .anyRequest().authenticated()
                )

                // JWT validation + role extraction from Keycloak token
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    /**
     * ✅ Extract Keycloak CLIENT roles from:
     *   resource_access.<client-id>.roles = ["KAFKA_ADMIN", "KAFKA_DEV", ...]
     *
     * Converts them to Spring authorities:
     *   ROLE_KAFKA_ADMIN, ROLE_KAFKA_DEV, ...
     *
     * So `.hasRole("KAFKA_ADMIN")` works.
     */
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
