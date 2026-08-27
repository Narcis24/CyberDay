package com.neueda.leap.merchantportal;

public class PayoutApprovalService {

    private PayoutRepository payoutRepository;

    public PayoutApprovalService(PayoutRepository payoutRepository) {
        this.payoutRepository = payoutRepository;
    }

    // Segregation of duties enforced below: requester cannot approve their own payout.

    public void approve(Long payoutId, Long approvingUserId) {
        if (approvingUserId == null) {
            throw new IllegalArgumentException("approvingUserId must not be null");
        }

        PayoutRequest payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found"));

        if (approvingUserId.equals(payout.getRequestedByUserId())) {
            throw new IllegalArgumentException("Requester cannot approve their own payout");
        }

        if (!"PENDING".equals(payout.getApprovalStatus())) {
            throw new IllegalStateException("Only pending payouts can be approved");
        }

        payout.setApprovalStatus("APPROVED");
        payout.setApprovedByUserId(approvingUserId);
        payoutRepository.save(payout);
    }
}
