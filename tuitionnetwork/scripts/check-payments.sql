SELECT
  (SELECT count(*) FROM payments.payment)            AS payments,
  (SELECT count(*) FROM payments.payment_allocation) AS allocations,
  (SELECT count(*) FROM payments.outbox_event)       AS events;
  SELECT id, status, amount, currency, created_at FROM payments.payment;