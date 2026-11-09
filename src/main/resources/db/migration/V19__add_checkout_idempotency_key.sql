ALTER TABLE customer_order ADD COLUMN checkout_key VARCHAR(128);

ALTER TABLE customer_order ADD CONSTRAINT uq_order_consumer_checkout_key
    UNIQUE (consumer_id, checkout_key);

ALTER TABLE customer_order ADD CONSTRAINT chk_order_checkout_key
    CHECK (checkout_key IS NULL OR checkout_key ~ '^[A-Za-z0-9._:-]{1,128}$');
