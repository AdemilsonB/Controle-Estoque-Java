package com.estoque.controller;

import com.estoque.dto.request.LoginRequest;
import com.estoque.dto.response.LoginResponse;
import com.estoque.security.FuncionarioUserDetails;
import com.estoque.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long expiracaoMs;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                           @Value("${jwt.expiration-ms}") long expiracaoMs) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.expiracaoMs = expiracaoMs;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        var userDetails = (FuncionarioUserDetails) authentication.getPrincipal();
        String token = jwtService.gerarToken(userDetails.getUsername(),
                com.estoque.enums.Role.valueOf(userDetails.getAuthorities().iterator().next()
                        .getAuthority().replace("ROLE_", "")));

        return ResponseEntity.ok(new LoginResponse(token, "Bearer", expiracaoMs / 1000));
    }
}
