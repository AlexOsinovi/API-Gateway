package by.osinovi.apigateway.config;

import jakarta.validation.Validator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://authentication-service:8081"))
                .route("user-service", r -> r.path("/api/users/**,/api/cards/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://user-service:8080"))
                .route("order-service", r -> r.path("/api/orders/**,/api/items/**,/api/order-items/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://order-service:8082"))
                .build();
    }

    @Bean
    public Validator validator() {
        return jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
    }
}