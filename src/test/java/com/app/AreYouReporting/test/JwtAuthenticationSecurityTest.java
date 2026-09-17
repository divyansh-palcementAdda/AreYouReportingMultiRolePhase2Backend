package com.app.AreYouReporting.test;

import com.app.AreYouReporting.Entities.User;
import com.app.AreYouReporting.repository.UserRepository;
import com.app.AreYouReporting.security.CustomUserDetailsService;
import com.app.AreYouReporting.security.JwtAuthenticationFilter;
import com.app.AreYouReporting.security.JwtTokenProvider;
import com.app.AreYouReporting.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationSecurityTest {

    private JwtTokenProvider tokenProvider;
    private CustomUserDetailsService customUserDetailsService;
    private JwtAuthenticationFilter filter;

    @Mock
    private UserRepository userRepository;

    private final String jwtSecret = "2x9zV7kQyt8qEktJBSzGeJKaVzMeEVFkh0FcogLPBQzX8p2mW3nY5tR7uC9vA1bD4eF6gH8iJ0kL2mN4oP6qR8sT0uV2wX4yZ6aB8cD0eF2gH4iJ6k";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider(jwtSecret, 86400000, 604800000);
        customUserDetailsService = new CustomUserDetailsService(userRepository);
        filter = new JwtAuthenticationFilter(tokenProvider, customUserDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. Valid token with an existing active user successfully sets authentication in security context")
    void testValidTokenWithExistingActiveUser() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        User activeUser = User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .fullName("Test User")
                .isActive(true)
                .build();
        activeUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UserPrincipal principal = UserPrincipal.create(activeUser);
        String token = tokenProvider.generateAccessToken(principal, null, null, null, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be set for active existing user");
        assertEquals("testuser", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("2. Validly signed token with non-existent user UUID does not set authentication and leaves context clean")
    void testValidlySignedTokenWithNonExistentUser() throws ServletException, IOException {
        UUID nonExistentUserId = UUID.fromString("eeda06ce-05f0-45dd-8d7a-878ee20ff032");

        User tempUser = User.builder()
                .username("ghost")
                .email("ghost@example.com")
                .fullName("Ghost User")
                .isActive(true)
                .build();
        tempUser.setId(nonExistentUserId);

        UserPrincipal principal = UserPrincipal.create(tempUser);
        String token = tokenProvider.generateAccessToken(principal, null, null, null, null);

        // Database does not contain this user
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication must not be set for non-existent user");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("3. Token with malformed UUID subject does not set authentication")
    void testMalformedUuidSubject() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication must not be set for malformed token");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("4. Deleted/stale user token cleanly rejected without error")
    void testStaleDeletedUserToken() throws ServletException, IOException {
        UUID deletedUserId = UUID.randomUUID();

        User deletedUser = User.builder()
                .username("deleteduser")
                .email("deleted@example.com")
                .fullName("Deleted User")
                .isActive(true)
                .build();
        deletedUser.setId(deletedUserId);

        String staleToken = tokenProvider.generateAccessToken(UserPrincipal.create(deletedUser), null, null, null, null);

        when(userRepository.findById(deletedUserId)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + staleToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Stale token must leave SecurityContext empty");
    }

    @Test
    @DisplayName("5. Inactive user's token is rejected and does not set authentication")
    void testInactiveUserTokenRejected() throws ServletException, IOException {
        UUID inactiveUserId = UUID.randomUUID();
        User inactiveUser = User.builder()
                .username("inactiveuser")
                .email("inactive@example.com")
                .fullName("Inactive User")
                .isActive(false)
                .build();
        inactiveUser.setId(inactiveUserId);

        when(userRepository.findById(inactiveUserId)).thenReturn(Optional.of(inactiveUser));

        UserPrincipal principal = UserPrincipal.create(inactiveUser);
        String token = tokenProvider.generateAccessToken(principal, null, null, null, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication must not be set for inactive user");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("6. JWT generation uses persistent User.id as the token subject")
    void testJwtSubjectMatchesPersistentUserId() {
        UUID persistentId = UUID.randomUUID();
        User user = User.builder()
                .username("admin")
                .email("admin@rcef.com")
                .fullName("Administrator")
                .isActive(true)
                .build();
        user.setId(persistentId);

        UserPrincipal principal = UserPrincipal.create(user);
        String token = tokenProvider.generateAccessToken(principal, null, null, null, null);

        UUID extractedId = tokenProvider.getUserIdFromToken(token);
        assertEquals(persistentId, extractedId, "JWT sub must match the persistent User.id");
    }

    @Test
    @DisplayName("7. Fallback to username claim when token UUID differs but user exists in database")
    void testFallbackToUsernameClaimWhenUserExists() throws ServletException, IOException {
        UUID oldTokenUuid = UUID.fromString("eeda06ce-05f0-45dd-8d7a-878ee20ff032");
        UUID newDbUuid = UUID.randomUUID();

        User currentDbUser = User.builder()
                .username("superadmin")
                .email("superadmin@rcef.com")
                .fullName("Global Super Administrator")
                .isActive(true)
                .build();
        currentDbUser.setId(newDbUuid);

        User oldUser = User.builder()
                .username("superadmin")
                .email("superadmin@rcef.com")
                .fullName("Global Super Administrator")
                .isActive(true)
                .build();
        oldUser.setId(oldTokenUuid);

        String token = tokenProvider.generateAccessToken(UserPrincipal.create(oldUser), null, null, null, null);

        // findById fails for the old UUID
        when(userRepository.findById(oldTokenUuid)).thenReturn(Optional.empty());
        // findByUsername succeeds for the active user in current DB
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(currentDbUser));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should succeed via signed username claim fallback");
        assertEquals("superadmin", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}

