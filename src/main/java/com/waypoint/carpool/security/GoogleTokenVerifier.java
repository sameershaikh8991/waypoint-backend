package com.waypoint.carpool.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Collections;

// Verifies the ID token (JWT "credential") that Google Identity Services
// hands back to the frontend. This checks the token's signature against
// Google's public keys, its expiry, and that it was issued for OUR OAuth
// client ID — never trust the claims in a token you haven't verified.
@Service
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * @return the verified payload (email, name, sub, etc.)
     * @throws BadCredentialsException if the token is missing, expired, or wasn't issued for this app
     */
    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new BadCredentialsException("Invalid Google sign-in token.");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            if (payload.getEmailVerified() == null || !payload.getEmailVerified()) {
                throw new BadCredentialsException("Google account email is not verified.");
            }
            return payload;
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw new BadCredentialsException("Could not verify Google sign-in token.");
        }
    }
}
