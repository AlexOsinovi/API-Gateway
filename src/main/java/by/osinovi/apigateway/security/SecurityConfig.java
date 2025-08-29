package by.osinovi.apigateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtValidationFilter jwtValidationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        http.addFilterAt(jwtValidationFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        http.authorizeExchange(exchange -> {
            exchange
                    .pathMatchers("/auth/login", "/auth/register", "/auth/refresh-token", "/auth/validate-token").permitAll()
                    .anyExchange().authenticated();
        });
        return http.build();
    }
}