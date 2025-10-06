package by.osinovi.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Bean
    public WebClient userWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(userServiceUrl).build();
    }

    @Bean
    public WebClient authWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl(authServiceUrl).build();
    }
}