ALTER TABLE purchase_orders DROP CONSTRAINT purchase_orders_status_check;
ALTER TABLE purchase_orders ADD CONSTRAINT purchase_orders_status_check
    CHECK (status IN ('CREATED','CONFIRMED','PROCESSING','DISPATCHED','RECEIVED'));
CREATE TABLE order_events (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    target_status VARCHAR(20) NOT NULL CHECK (target_status IN ('CONFIRMED','PROCESSING','DISPATCHED','RECEIVED')),
    occurred_at TIMESTAMPTZ NOT NULL,
    acting_account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    UNIQUE (order_id,target_status)
);
