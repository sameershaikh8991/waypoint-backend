package com.waypoint.carpool.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    // Null for accounts created via Google Sign-In that have never set a
    // local password.
    private String passwordHash;

    // "LOCAL" (email + password) or "GOOGLE" (Google Sign-In). A LOCAL
    // account that later signs in with Google gets linked (googleId set)
    // rather than duplicated, but keeps provider = LOCAL so its password
    // still works too.
    @Column(nullable = false)
    private String provider = "LOCAL";

    // Google's stable per-user identifier ("sub" claim). Null unless the
    // account has signed in with Google at least once.
    @Column(unique = true)
    private String googleId;

    private String phone;

    // UPI ID (VPA) drivers set so riders can pay them directly, e.g. "name@okhdfcbank".
    // Not verified against any bank/UPI provider — self-reported, display-only.
    private String upiId;

    @Column(nullable = false)
    private boolean isDriver = false;

    @Column(nullable = false)
    private double avgRating = 5.0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public User() {}

    public User(String fullName, String email, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.provider = "LOCAL";
    }

    public static User googleUser(String fullName, String email, String googleId) {
        User user = new User();
        user.fullName = fullName;
        user.email = email;
        user.provider = "GOOGLE";
        user.googleId = googleId;
        return user;
    }

    // ---- UserDetails ----
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }


}
