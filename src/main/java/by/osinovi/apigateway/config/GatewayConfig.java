package by.osinovi.apigateway.config;

import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/auth/**")
                        .uri(authServiceUrl))
                .route("user-service", r -> r.path("/api/users/**").or().path("/api/cards/**")
                        .uri(userServiceUrl))
                .route("order-service", r -> r.path("/api/orders/**").or().path("/api/items/**").or().path("/api/order-items/**")
                        .uri(orderServiceUrl))
                .build();
    }

    @Bean
    public Validator validator() {
        return jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
    }
}