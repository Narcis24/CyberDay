package com.neueda.leap.merchantportal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class MerchantController {

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private CurrentMerchantProvider currentMerchantProvider;

    // FIX (A01 - Broken Access Control): verify the payout belongs to the
    // caller before returning it. Returns 404 rather than 403 so an attacker
    // can't use the response to tell apart "not yours" from "doesn't exist".
    @GetMapping("/api/payouts/{payoutId}")
    public PayoutRequest getPayout(@PathVariable Long payoutId) {
        PayoutRequest payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new NotFoundException("Payout not found"));

        if (!payout.getMerchantId().equals(currentMerchantProvider.currentMerchantId())) {
            throw new NotFoundException("Payout not found");
        }

        return payout;
    }
}
