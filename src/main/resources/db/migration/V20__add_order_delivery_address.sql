-- Existing orders have no historical address to backfill. Keep their snapshot NULL.
ALTER TABLE customer_order
    ADD COLUMN delivery_recipient_name VARCHAR(255),
    ADD COLUMN delivery_phone VARCHAR(30),
    ADD COLUMN delivery_address_line1 VARCHAR(255),
    ADD COLUMN delivery_address_line2 VARCHAR(255),
    ADD COLUMN delivery_city VARCHAR(100),
    ADD COLUMN delivery_postal_code VARCHAR(20),
    ADD COLUMN delivery_country VARCHAR(100);

ALTER TABLE customer_order ADD CONSTRAINT chk_order_delivery_address CHECK (
    (delivery_recipient_name IS NULL AND delivery_phone IS NULL
     AND delivery_address_line1 IS NULL AND delivery_address_line2 IS NULL
     AND delivery_city IS NULL AND delivery_postal_code IS NULL AND delivery_country IS NULL)
    OR
    (delivery_recipient_name IS NOT NULL AND btrim(delivery_recipient_name) <> ''
     AND delivery_phone IS NOT NULL AND btrim(delivery_phone) <> ''
     AND delivery_address_line1 IS NOT NULL AND btrim(delivery_address_line1) <> ''
     AND delivery_city IS NOT NULL AND btrim(delivery_city) <> ''
     AND delivery_postal_code IS NOT NULL AND btrim(delivery_postal_code) <> ''
     AND delivery_country IS NOT NULL AND btrim(delivery_country) <> '')
);
