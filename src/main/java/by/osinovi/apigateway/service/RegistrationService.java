package by.osinovi.apigateway.service;

import by.osinovi.apigateway.dto.auth.AuthRequest;
import by.osinovi.apigateway.dto.gateway.GatewayRegistrationRequest;
import by.osinovi.apigateway.dto.user.UserRequest;
import by.osinovi.apigateway.dto.user.UserResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final WebClient userWebClient;
    private final WebClient authWebClient;
    private final Validator validator;

    public Mono<ServerResponse> register(GatewayRegistrationRequest req) {
        Set<ConstraintViolation<GatewayRegistrationRequest>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            return ServerResponse.badRequest().bodyValue(violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList()));
        }

        UserRequest userDto = new UserRequest(req.getName(), req.getSurname(), req.getBirthDate(), req.getEmail());
        return userWebClient.post()
                .bodyValue(userDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> Mono.error(new RuntimeException("User Service failed: " + resp.statusCode())))
                .bodyToMono(UserResponse.class)
                .flatMap(userResponse -> {
                    Long userId = userResponse.getId();

                    AuthRequest authDto = new AuthRequest(req.getEmail(), req.getPassword());
                    return authWebClient.post()
                            .uri("/register")
                            .body(BodyInserters.fromValue(authDto))
                            .retrieve()
                            .onStatus(HttpStatusCode::isError, resp -> Mono.error(new RuntimeException("Auth Service failed: " + resp.statusCode())))
                            .bodyToMono(String.class)
                            .then(ServerResponse.ok().bodyValue("Registration successful"))
                            .onErrorResume(e -> userWebClient.delete()
                                    .uri("/{id}", userId)
                                    .retrieve()
                                    .bodyToMono(Void.class)
                                    .then(Mono.error(new RuntimeException("Registration failed with rollback: " + e.getMessage()))));
                });
    }
}