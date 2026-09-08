-- Refund and void are out of scope for this project — the mock supports them,
-- but nothing in our system will ever call those endpoints. captured_amount stays
-- (capture is still handled); refunded_amount would just always be zero.
ALTER TABLE payments.payment DROP COLUMN refunded_amount;
