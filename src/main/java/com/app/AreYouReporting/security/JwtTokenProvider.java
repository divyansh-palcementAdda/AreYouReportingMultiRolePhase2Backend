package com.app.AreYouReporting.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private final Key key;
    private final long jwtExpirationMs;
    private final long jwtRefreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:2x9zV7kQyt8qEktJBSzGeJKaVzMeEVFkh0FcogLPBQzX8p2mW3nY5tR7uC9vA1bD4eF6gH8iJ0kL2mN4oP6qR8sT0uV2wX4yZ6aB8cD0eF2gH4iJ6k}") String jwtSecret,
            @Value("${jwt.expiration:86400000}") long jwtExpirationMs,
            @Value("${jwt.refresh.expiration:604800000}") long jwtRefreshExpirationMs) {

        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtRefreshExpirationMs = jwtRefreshExpirationMs;
    }

    public String generateAccessToken(UserPrincipal principal, UUID activeRoleId, String activeRoleName, UUID activeDeptId, UUID activeSubDeptId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("username", principal.getUsername());
        claims.put("email", principal.getEmail());
        claims.put("fullName", principal.getFullName());

        if (activeRoleId != null) {
            claims.put("activeRoleId", activeRoleId.toString());
        }
        if (activeRoleName != null) {
            claims.put("activeRoleName", activeRoleName);
        }
        if (activeDeptId != null) {
            claims.put("activeDepartmentId", activeDeptId.toString());
        }
        if (activeSubDeptId != null) {
            claims.put("activeSubDepartmentId", activeSubDeptId.toString());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(principal.getId().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .setSubject(principal.getId().toString())
                .claim("type", "REFRESH")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            if (claims == null || claims.getSubject() == null) {
                return null;
            }
            return UUID.fromString(claims.getSubject().trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid UUID subject format in JWT token: {}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Could not extract userId from JWT token: {}", ex.getMessage());
            return null;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
