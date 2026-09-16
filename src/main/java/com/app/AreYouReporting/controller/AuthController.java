package com.app.AreYouReporting.controller;

import com.app.AreYouReporting.payload.request.ContextSwitchRequest;
import com.app.AreYouReporting.payload.request.LoginRequest;
import com.app.AreYouReporting.payload.request.RefreshTokenRequest;
import com.app.AreYouReporting.payload.response.ApiResponse;
import com.app.AreYouReporting.payload.response.AuthResponse;
import com.app.AreYouReporting.payload.response.UserDto;
import com.app.AreYouReporting.service.interfaces.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Context Management", description = "Endpoints for user login, token refresh, active role/scope switching, and profile retrieval")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT tokens with dynamic grants and available role scopes")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/switch-context")
    @Operation(summary = "Switch active role and departmental scope context for the current user")
    public ResponseEntity<ApiResponse<AuthResponse>> switchContext(@Valid @RequestBody ContextSwitchRequest request) {
        AuthResponse response = authService.switchContext(request);
        return ResponseEntity.ok(ApiResponse.success("Context switched successfully", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using a valid refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Retrieve current authenticated user's profile with assignments and effective grants")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUserProfile() {
        UserDto profile = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
