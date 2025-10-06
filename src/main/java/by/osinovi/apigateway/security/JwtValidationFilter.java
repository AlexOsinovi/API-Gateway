package by.osinovi.apigateway.security;

import by.osinovi.apigateway.dto.token.TokenValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;

public class JwtValidationFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidationFilter.class);
    private final WebClient webClient;

    public JwtValidationFilter(@Qualifier("authWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        logger.info("JwtValidationFilter invoked for request: {}, RequestId: {}",
                exchange.getRequest().getPath(), requestId);

        Object cachedAuth = exchange.getAttribute("cachedAuth");
        if (cachedAuth instanceof Authentication auth) {
            logger.info("Using cached authentication for user: {}, RequestId: {}", auth.getName(), requestId);
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(auth))));
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        logger.info("Processing request: {}, Method: {}, Headers: {}, RequestId: {}",
                path, method, request.getHeaders(), requestId);

        if (path.startsWith("/auth/login") ||
                path.startsWith("/auth/register") ||
                path.startsWith("/auth/refresh-token") ||
                path.startsWith("/auth/validate-token")) {
            logger.info("Skipping JWT validation for path: {}, RequestId: {}", path, requestId);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        logger.info("Authorization header: {}, RequestId: {}", authHeader, requestId);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header, RequestId: {}", requestId);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        return webClient.get()
                .uri("/auth/validate-token")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .doOnNext(errorBody -> logger.error("Token validation failed: {} - {}, RequestId: {}",
                                        response.statusCode(), errorBody, requestId))
                                .flatMap(errorBody -> Mono.error(new RuntimeException("Invalid token: " + errorBody))))
                .bodyToMono(TokenValidationResponse.class)
                .doOnNext(response -> logger.info("Token validation response: isValid={}, email={}, RequestId: {}",
                        response.isValid(), response.getEmail(), requestId))
                .flatMap(response -> {
                    if (!response.isValid() || response.getEmail() == null || response.getEmail().isEmpty()) {
                        logger.warn("Invalid JWT token or email is null/empty, RequestId: {}", requestId);
                        return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                    }
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            response.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("USER")));
                    exchange.getAttributes().put("cachedAuth", auth);
                    SecurityContext context = new SecurityContextImpl(auth);
                    logger.info("Authentication created for user: {}, RequestId: {}", response.getEmail(), requestId);
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
                })
                .onErrorResume(e -> {
                    logger.error("Error during token validation: {}, RequestId: {}", e.getMessage(), requestId);
                    return onError(exchange, e.getMessage(), HttpStatus.UNAUTHORIZED);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        String requestId = UUID.randomUUID().toString();
        logger.error("Error response: {}, Status: {}, RequestId: {}", err, status, requestId);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        DataBuffer buffer = response.bufferFactory().wrap(err.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}