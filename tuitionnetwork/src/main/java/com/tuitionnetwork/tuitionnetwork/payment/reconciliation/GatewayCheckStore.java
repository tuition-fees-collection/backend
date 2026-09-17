package com.tuitionnetwork.tuitionnetwork.payment.reconciliation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class GatewayCheckStore {
//This SQL creates a new reconciliation run.
    private static final String INSERT_RUN = """
            INSERT INTO reporting.gateway_check_run (id, run_date, matched_count, unmatched_count)
            VALUES (?, ?, 0, 0)
            """;
//After the reconciliation finishes, we update the numbers.
    private static final String UPDATE_COUNTS = """
            UPDATE reporting.gateway_check_run
            SET matched_count = ?, unmatched_count = ?
            WHERE id = ?
            """;
//This saves one payment's reconciliation result.
    private static final String INSERT_ITEM = """
            INSERT INTO reporting.gateway_check_item
                (id, report_id, payment_id, gateway_status, status)
            VALUES (?, ?, ?, ?, ?)
            """;
//This gets the most recent reconciliation runs.
    private static final String RECENT_RUNS = """
            SELECT id, run_date, matched_count, unmatched_count
            FROM reporting.gateway_check_run
            ORDER BY run_date DESC
            LIMIT ?
            """;
//Show me all the payments checked during this particular reconciliation run.
    private static final String ITEMS_FOR_RUN = """
            SELECT id, report_id, payment_id, gateway_status, status
            FROM reporting.gateway_check_item
            WHERE report_id = ?
            """;
//Show me all reconciliation records for this particular payment
    private static final String ITEMS_FOR_PAYMENT = """
            SELECT id, report_id, payment_id, gateway_status, status
            FROM reporting.gateway_check_item
            WHERE payment_id = ?
            """;
//convert row to java
    private static final RowMapper<GatewayCheckRecords.Run> RUN_MAPPER = (rs, i) ->
            new GatewayCheckRecords.Run(
                    rs.getObject("id", UUID.class),
                    rs.getTimestamp("run_date").toInstant(),
                    rs.getInt("matched_count"),
                    rs.getInt("unmatched_count"));

    private static final RowMapper<GatewayCheckRecords.Item> ITEM_MAPPER = (rs, i) ->
            new GatewayCheckRecords.Item(
                    rs.getObject("id", UUID.class),
                    rs.getObject("report_id", UUID.class),
                    rs.getObject("payment_id", UUID.class),
                    rs.getString("gateway_status"),
                    GatewayCheckRecords.Outcome.valueOf(rs.getString("status")));

    private final JdbcTemplate jdbc;

    public GatewayCheckStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    @Transactional
    public UUID startRun() {
        UUID id = UUID.randomUUID();
        jdbc.update(INSERT_RUN, id, java.sql.Timestamp.from(Instant.now()));
        return id;
    }

    @Transactional
    public void recordItem(UUID runId, UUID paymentId,
                           String gatewayStatus, GatewayCheckRecords.Outcome outcome) {
        jdbc.update(INSERT_ITEM, UUID.randomUUID(), runId, paymentId, gatewayStatus, outcome.name());
    }

    @Transactional
    public void closeRun(UUID runId, int matched, int unmatched) {
        jdbc.update(UPDATE_COUNTS, matched, unmatched, runId);
    }
//This reads recent reconciliation runs.
    @Transactional(readOnly = true)
    public List<GatewayCheckRecords.Run> recentRuns(int limit) {
        return jdbc.query(RECENT_RUNS, RUN_MAPPER, limit);
    }
//Give me all payment checks belonging to this run.
    @Transactional(readOnly = true)
    public List<GatewayCheckRecords.Item> itemsForRun(UUID runId) {
        return jdbc.query(ITEMS_FOR_RUN, ITEM_MAPPER, runId);
    }
    //Give me all reconciliation records for this payment.
    @Transactional(readOnly = true)
    public List<GatewayCheckRecords.Item> itemsForPayment(UUID paymentId) {
        return jdbc.query(ITEMS_FOR_PAYMENT, ITEM_MAPPER, paymentId);
    }
}