package com.app.AreYouReporting.service.interfaces;

import com.app.AreYouReporting.payload.request.ContextSwitchRequest;
import com.app.AreYouReporting.payload.request.LoginRequest;
import com.app.AreYouReporting.payload.request.RefreshTokenRequest;
import com.app.AreYouReporting.payload.response.AuthResponse;
import com.app.AreYouReporting.payload.response.UserDto;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse switchContext(ContextSwitchRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    UserDto getCurrentUserProfile();
}
