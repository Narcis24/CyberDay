package com.neueda.leap.merchantportal;

import java.util.List;

public class BatchPayoutJob {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BatchPayoutJob.class);

    private BankTransferClient bankTransferClient;
    private PayoutRepository payoutRepository;

    public BatchPayoutJob(BankTransferClient bankTransferClient, PayoutRepository payoutRepository) {
        this.bankTransferClient = bankTransferClient;
        this.payoutRepository = payoutRepository;
    }

    // FIX (A10 - Mishandling of Exceptional Conditions): a failed transfer
    // must not be recorded as PAID. Mark it FAILED so it's distinguishable
    // from a successful payout and requires manual review before any retry,
    // instead of being silently re-run and risking a double payment.
    public void runNightlyBatch(List<PayoutRequest> approvedPayouts) {
        for (PayoutRequest payout : approvedPayouts) {
            try {
                bankTransferClient.transfer(payout.getMerchantId(), payout.getAmount());
                payout.setApprovalStatus("PAID");
            } catch (BankTransferException e) {
                log.error("Transfer failed for payout {}, marking FAILED for manual review: {}",
                        payout.getId(), e.getMessage());
                payout.setApprovalStatus("FAILED");
            }
            payoutRepository.save(payout);
        }
    }
}
