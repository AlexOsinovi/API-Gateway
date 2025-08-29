package by.osinovi.apigateway.security;

import by.osinovi.apigateway.dto.token.TokenValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBuffer;

import java.util.Collections;

@Component
public class JwtValidationFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidationFilter.class);
    private final WebClient webClient;

    public JwtValidationFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        logger.info("JwtValidationFilter invoked for request: {}", exchange.getRequest().getPath());
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        logger.info("Processing request: {}", path);

        if (path.startsWith("/auth/login") ||
                path.startsWith("/auth/register") ||
                path.startsWith("/auth/refresh-token") ||
                path.startsWith("/auth/validate-token")) {
            logger.info("Skipping JWT validation for path: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        logger.info("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header");
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        return webClient.get()
                .uri("/auth/validate-token")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("Token validation failed: {} - {}", response.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("Invalid token: " + response.statusCode() + " - " + errorBody));
                                }))
                .bodyToMono(TokenValidationResponse.class)
                .flatMap(response -> {
                    logger.info("Token validation response: isValid={}, email={}", response.isValid(), response.getEmail());
                    if (!response.isValid()) {
                        logger.warn("Invalid JWT token");
                        return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                    }
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            response.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("USER")));
                    SecurityContext context = new SecurityContextImpl(auth);
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
                })
                .onErrorResume(e -> {
                    logger.error("Error during token validation: {}", e.getMessage());
                    return onError(exchange, e.getMessage(), HttpStatus.UNAUTHORIZED);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        DataBuffer buffer = response.bufferFactory().wrap(err.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}