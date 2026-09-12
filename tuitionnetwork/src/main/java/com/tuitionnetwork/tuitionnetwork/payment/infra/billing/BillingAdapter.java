package com.tuitionnetwork.tuitionnetwork.payment.infra.billing;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tuitionnetwork.tuitionnetwork.payment.port.portout.BillingPort;
@Component
public class BillingAdapter implements BillingPort {
    private static final Logger log = LoggerFactory.getLogger(BillingAdapter.class);
    private static final String SELECT_FEES = """
            SELECT id, student_id, institution_id, fee_type, collection_period,
                   amount, amount_paid, status, version
            FROM billing.fee
            WHERE id = ANY (?)
            """;
    private static final String SETTLE = """
            UPDATE billing.fee
            SET amount_paid = amount_paid + ?,
                status = CASE
                    WHEN amount_paid + ? >= amount THEN 'SETTLED'
                    ELSE 'PARTIALLY_PAID'
                END,
                version = version + 1
            WHERE id = ? AND version = ?
            """;

    private static final RowMapper<PayableFee> MAPPER = (rs, i) -> new PayableFee(
            rs.getObject("id", UUID.class),
            rs.getObject("student_id", UUID.class),
            rs.getObject("institution_id", UUID.class),
            rs.getString("fee_type"),
            rs.getString("collection_period"),
            rs.getBigDecimal("amount"),
            rs.getBigDecimal("amount_paid"),
            rs.getString("status"),
            rs.getInt("version"));

    private final JdbcTemplate jdbc;

    public BillingAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    @Override
    @Transactional(readOnly = true)
    public List<PayableFee> loadPayable(List<UUID> feeIds) {
        if (feeIds == null || feeIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(SELECT_FEES, MAPPER, (Object) feeIds.toArray(new UUID[0]));
    }
    @Override
    @Transactional
    public void settle(UUID paymentId, List<FeeSettlement> settlements) {
        if (settlements == null || settlements.isEmpty()) {
            return;
        }

        for (FeeSettlement settlement : settlements) {
            BigDecimal applied = settlement.amountApplied();

            int rows = jdbc.update(SETTLE,
                    applied,
                    applied,
                    settlement.feeId(),
                    settlement.expectedVersion());

            if (rows == 0) {
                log.warn("Fee {} changed under payment {} - expected version {}",
                        settlement.feeId(), paymentId, settlement.expectedVersion());
                throw new OptimisticLockingFailureException(
                        "Fee " + settlement.feeId() + " was modified by another payment");
            }
        }

        log.info("Payment {} settled {} fee line(s)", paymentId, settlements.size());
    }
}