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
                        .uri("http://localhost:8081"))
                .route("user-service", r -> r.path("/api/users/**").or().path("/api/cards/**")
                        .uri("http://localhost:8080"))
                .route("order-service", r -> r.path("/api/orders/**").or().path("/api/items/**").or().path("/api/order-items/**")
                        .uri("http://localhost:8082"))
                .build();
    }

    @Bean
    public Validator validator() {
        return jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
    }
}