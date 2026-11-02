ALTER TABLE order_item
DROP CONSTRAINT chk_order_item_unit_price;

ALTER TABLE order_item
ADD CONSTRAINT chk_order_item_unit_price
CHECK (unit_price > 0);