package com.waypoint.carpool.service;

import com.waypoint.carpool.entity.PasswordResetOtp;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.exception.BadRequestException;
import com.waypoint.carpool.repository.PasswordResetOtpRepository;
import com.waypoint.carpool.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    private final int expiryMinutes;
    private final int resendCooldownSeconds;
    private final int maxAttempts;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetOtpRepository otpRepository,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            @Value("${app.otp.expiry-minutes:10}") int expiryMinutes,
            @Value("${app.otp.resend-cooldown-seconds:60}") int resendCooldownSeconds,
            @Value("${app.otp.max-attempts:5}") int maxAttempts
    ) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.expiryMinutes = expiryMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Always looks the same to the caller whether or not the email is
     * registered — this endpoint must never become a way to check which
     * emails have accounts. Only actually generates/sends a code when the
     * account exists and has a local password to reset.
     */
    @Transactional
    public void requestOtp(String rawEmail) {
        String email = rawEmail.toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getPassword() == null) {
                // Google-only account — nothing to reset locally, and telling
                // the caller that would leak account existence. Silent no-op.
                return;
            }

            otpRepository.findTopByUserOrderByCreatedAtDesc(user).ifPresent(last -> {
                if (last.getCreatedAt().isAfter(Instant.now().minusSeconds(resendCooldownSeconds))) {
                    throw new BadRequestException("Please wait a bit before requesting another code");
                }
            });

            String code = generateCode();

            PasswordResetOtp otp = new PasswordResetOtp();
            otp.setUser(user);
            otp.setCodeHash(passwordEncoder.encode(code));
            otp.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expiryMinutes)));
            otpRepository.save(otp);

            mailService.sendOtp(user.getEmail(), code, expiryMinutes);
        });
    }

    @Transactional
    public void resetPassword(String rawEmail, String code, String newPassword) {
        String email = rawEmail.toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired code"));

        PasswordResetOtp otp = otpRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new BadRequestException("Invalid or expired code"));

        if (otp.isUsed() || otp.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired code — request a new one");
        }
        if (otp.getAttempts() >= maxAttempts) {
            throw new BadRequestException("Too many incorrect attempts — request a new code");
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            throw new BadRequestException("Invalid or expired code");
        }

        otp.setUsed(true);
        otpRepository.save(otp);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
