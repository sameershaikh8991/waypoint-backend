package com.waypoint.carpool.controller;

import com.waypoint.carpool.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Razorpay webhook receiver. Not authenticated with a JWT — Razorpay calls
 * this directly, so trust comes from the X-Razorpay-Signature HMAC instead
 * (verified inside PaymentService against app.razorpay.webhook-secret).
 * Must be listed under permitAll() in SecurityConfig.
 *
 * Configure in the Razorpay dashboard: Settings > Webhooks > Add New
 * Webhook, URL = https://your-domain/api/payments/webhook/razorpay,
 * active event = payment.captured.
 */
@RestController
public class PaymentWebhookController {

    private final PaymentService paymentService;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments/webhook/razorpay")
    public ResponseEntity<Void> razorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
