package com.tuitionnetwork.tuitionnetwork.payment.receipt.service;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.ReceiptPort;
import com.tuitionnetwork.tuitionnetwork.payment.receipt.domain.Receipt;
import com.tuitionnetwork.tuitionnetwork.payment.receipt.infra.ReceiptJpaRepository;
@Service 
public class ReceiptService implements ReceiptPort {
    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);
    private static final int MAX_ATTEMPTS = 5;
    private final ReceiptJpaRepository receipts;
    private final ReceiptNumberGenerator numbers;
    public ReceiptService(ReceiptJpaRepository receipts, ReceiptNumberGenerator numbers) {
        this.receipts = receipts;
        this.numbers = numbers;
    }
     @Override
    @Transactional
    public IssuedReceipt issueFor(UUID paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId is required");
        }
        Optional<Receipt> existing = receipts.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.debug("Reusing existing receipt {} for payment {}",
                    existing.get().getReferenceNumber(), paymentId);
            return toPortType(existing.get());
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String reference = numbers.next();
            try {
                Receipt receipt = receipts.save(Receipt.issue(paymentId, reference));
                log.info("Issued receipt {} for payment {}", reference, paymentId);
                return toPortType(receipt);
            } catch (DataIntegrityViolationException e) {
                Optional<Receipt> raced = receipts.findByPaymentId(paymentId);
                if (raced.isPresent()) {
                    return toPortType(raced.get());
                }
                log.warn("Reference number {} collided, retrying",
                        reference, attempt, MAX_ATTEMPTS);
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique receipt number after " + MAX_ATTEMPTS + " attempts");
    }
    @Override
    @Transactional(readOnly = true)
    //Find the receipt for a specific payment.
    public Optional<IssuedReceipt> findByPaymentId(UUID paymentId) {
        return receipts.findByPaymentId(paymentId).map(this::toPortType);
    }
    //transaltor
    private IssuedReceipt toPortType(Receipt receipt) {
        return new IssuedReceipt(receipt.getId(), receipt.getPaymentId(), receipt.getReferenceNumber());
    }
}
