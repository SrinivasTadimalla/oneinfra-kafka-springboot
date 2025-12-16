package com.srikar.kafka.controller;

import com.srikar.kafka.dto.AuthRequest;
import com.srikar.kafka.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationConfiguration authConfig) throws Exception {
        this.authenticationManager = authConfig.getAuthenticationManager();
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req, HttpServletRequest request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        // Create session -> Spring Session writes it to Redis
        request.getSession(true);

        return new AuthResponse("LOGIN_OK", auth.getName());
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpServletRequest request) {

        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        // After logout there may be no authenticated principal; return request username if you want,
        // but safest is null (or "anonymous")
        return new AuthResponse("LOGOUT_OK", null);
    }
}
