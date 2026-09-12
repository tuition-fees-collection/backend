package com.tuitionnetwork.tuitionnetwork.identity.api;

import com.tuitionnetwork.tuitionnetwork.identity.service.AuthService;
import com.tuitionnetwork.tuitionnetwork.identity.service.auth.TokenIssuer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final TokenIssuer tokenIssuer;

    public AuthController(AuthService authService, TokenIssuer tokenIssuer) {
        this.authService = authService;
        this.tokenIssuer = tokenIssuer;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var result = authService.authenticateCredentials(request.username(), request.password());
        if (!result.authenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new AuthenticationErrorResponse("AUTHENTICATION_FAILED", "Invalid credentials."));
        }
        String token = tokenIssuer.issue(result.actor(), result.tokenVersion());
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", 900));
    }
}
