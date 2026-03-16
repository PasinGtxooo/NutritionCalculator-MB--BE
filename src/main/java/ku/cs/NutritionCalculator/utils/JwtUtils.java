package ku.cs.NutritionCalculator.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import ku.cs.NutritionCalculator.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername()) // Set username as the token subject
                .claim("roles", roles) // Store roles in the token
                .setIssuedAt(new Date()) // Token issue time
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Set token expiration
                .signWith(key(), SignatureAlgorithm.HS256) // Sign the token with the secret key
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        if (token.startsWith("Bearer "))
            token = token.substring(7).trim();

        return Jwts.parserBuilder()
                .setSigningKey(key()) // Set signing key for validation
                .build()
                .parseClaimsJws(token) // Parse token claims
                .getBody()
                .getSubject(); // Extract subject (username)
    }

    public Claims getClaims(String token) {
        if (token.startsWith("Bearer "))
            token = token.substring(7).trim();

        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateJwtToken(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            logger.warn("JWT Token is null or empty.");
            return false;
        }

        // Remove surrounding braces and any additional formatting
        authToken = authToken.replaceAll("[{}]", "").trim(); // Remove { and }

        // Check if the token contains the "token:" prefix and extract

        if (authToken.startsWith("token:"))
            authToken = authToken.substring(6).trim(); // Remove the "token:" prefix


        // Ensure the token doesn't contain any additional quotes or whitespace
        authToken = authToken.replaceAll("[\"\\s]", "").trim();
        logger.info("Validating JWT Token: {}", authToken);

        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
            logger.info("JWT Token is valid.");
            return true;
        } catch (Exception e) {
            logger.error("JWT Token validation failed: {}", e.getMessage());
            return false;
        }
    }


    private Key key() {
        byte[] keyBytes = jwtSecret.getBytes();

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes) long.");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("roles", List.class);
    }
}
