package com.waypoint.carpool.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.waypoint.carpool.dto.auth.AuthResponse;
import com.waypoint.carpool.dto.auth.LoginRequest;
import com.waypoint.carpool.dto.auth.RegisterRequest;
import com.waypoint.carpool.dto.auth.UserResponse;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.exception.BadRequestException;
import com.waypoint.carpool.repository.UserRepository;
import com.waypoint.carpool.security.GoogleTokenVerifier;
import com.waypoint.carpool.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email().toLowerCase())) {
            throw new BadRequestException("An account with this email already exists.");
        }

        User user = new User(req.fullName(), req.email().toLowerCase(), passwordEncoder.encode(req.password()));
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getPassword() == null) {
            // Account was created via Google Sign-In and has no local password set.
            throw new BadCredentialsException("This account signs in with Google. Use \"Continue with Google\" instead.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse loginWithGoogle(String idToken) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idToken);

        String email = payload.getEmail().toLowerCase();
        String googleId = payload.getSubject();
        String fullName = (String) payload.get("name");
        if (fullName == null || fullName.isBlank()) {
            fullName = email.substring(0, email.indexOf('@'));
        }

        String finalFullName = fullName;
        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> User.googleUser(finalFullName, email, googleId));

        // Link a pre-existing local (email/password) account the first time
        // it signs in with Google, instead of creating a duplicate.
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
        }

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponse(token, UserResponse.from(user));
    }

    public UserResponse updateUpiId(User user, String upiId) {
        user.setUpiId(upiId);
        user = userRepository.save(user);
        return UserResponse.from(user);
    }
}
