package ku.cs.NutritionCalculator.service;

import ku.cs.NutritionCalculator.dto.user.SigninRequest;
import ku.cs.NutritionCalculator.dto.user.SigninResponse;
import ku.cs.NutritionCalculator.utils.JwtUtils;
import ku.cs.NutritionCalculator.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger tokenLogger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public ApiResponse<SigninResponse> signIn(SigninRequest signinRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            signinRequest.getUsername(),
                            signinRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            SigninResponse signinResponse = new SigninResponse();
            signinResponse.setToken(jwt);
            signinResponse.setId(userDetails.getId());
            signinResponse.setUsername(userDetails.getUsername());
            signinResponse.setPhone(userDetails.getPhone());
            signinResponse.setRole(userDetails.getRole());

            tokenLogger.info("User {} signed in successfully", userDetails.getUsername());
            return new ApiResponse<>(true, "Sign-in successful.", signinResponse);
        } catch (Exception e) {
            tokenLogger.error("Failed to sign in user {}: {}", signinRequest.getUsername(), e.getMessage());
            return new ApiResponse<>(false, "Sign-in failed: " + e.getMessage(), null);
        }
    }

    @Override
    public Boolean validateToken(String token) {
        return jwtUtils.validateJwtToken(token);
    }
}
