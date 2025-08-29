package by.osinovi.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient userWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl("http://localhost:8080").build();
    }

    @Bean
    public WebClient authWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder.baseUrl("http://localhost:8081").build();
    }
}