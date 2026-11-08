ALTER TABLE order_item
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PLACED',
ADD CONSTRAINT chk_order_item_status
CHECK (status IN (
        'PLACED',
        'SHIPPED',
        'DELIVERED',
        'CANCELLED'
    ));
