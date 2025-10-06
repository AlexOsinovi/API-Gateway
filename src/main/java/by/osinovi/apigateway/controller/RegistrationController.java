package by.osinovi.apigateway.controller;

import by.osinovi.apigateway.dto.gateway.GatewayRegistrationRequest;
import by.osinovi.apigateway.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(GatewayRegistrationRequest.class)
                .flatMap(registrationService::register)
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }
}