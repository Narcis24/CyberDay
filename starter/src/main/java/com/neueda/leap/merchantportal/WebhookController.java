package com.neueda.leap.merchantportal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
public class WebhookController {

    private final PayoutStatusUpdater payoutStatusUpdater;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // resolved from the OS environment variable of the same name, not application.properties
    @Value("${PAYMENT_WEBHOOK_SECRET}")
    private String secret;

    public WebhookController(PayoutStatusUpdater payoutStatusUpdater) {
        this.payoutStatusUpdater = payoutStatusUpdater;
    }

    @PostMapping("/api/webhooks/payment-status")
    public ResponseEntity<Void> handlePaymentStatusWebhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Signature") String signature) throws Exception {

        // signature must be checked against the raw bytes before any JSON parsing/trust
        if (!isValidSignature(rawBody, signature)) {
            return ResponseEntity.status(401).build();
        }

        PaymentStatusEvent event = objectMapper.readValue(rawBody, PaymentStatusEvent.class);
        payoutStatusUpdater.markSettled(event.getPayoutId(), event.getStatus());
        return ResponseEntity.ok().build();
    }

    // constant-time comparison prevents timing attacks on the signature check
    private boolean isValidSignature(String payload, String signature) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String expectedHex = HexFormat.of().formatHex(expected);
        return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
