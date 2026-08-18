package com.waypoint.carpool.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A one-time 6-digit code emailed to a user for the "forgot password" flow.
 * The code itself is never stored in plain text — only its bcrypt hash —
 * same as we'd never store a real password in plain text.
 */
@Entity
@Table(name = "password_reset_otps")
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    // Wrong-code guesses against this specific OTP — capped so someone can't
    // brute-force a 6-digit code (1M combos) before it expires.
    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PasswordResetOtp() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Instant getCreatedAt() { return createdAt; }
}
