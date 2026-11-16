package com.chandra.cardauth.api;

import com.chandra.cardauth.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {

    private final AuthorizationService service;

    public AuthorizationController(AuthorizationService service) {
        this.service = service;
    }

    /** Approved and declined outcomes are both normal results, so both return 200. */
    @PostMapping
    public AuthorizationResponse authorize(@Valid @RequestBody AuthorizationApiRequest request) {
        return AuthorizationResponse.from(service.authorize(request.toDomain()));
    }

    @PostMapping("/{authorizationId}/reversal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reverse(@PathVariable String authorizationId) {
        service.reverse(authorizationId);
    }
}
