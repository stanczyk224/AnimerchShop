-- Fail rather than silently discard existing duplicate carts or cart items.
ALTER TABLE cart
    ADD CONSTRAINT uq_cart_consumer UNIQUE (consumer_id);

ALTER TABLE cart_item
    ADD CONSTRAINT uq_cart_item_cart_product UNIQUE (cart_id, product_id);

-- NOT VALID preserves legacy catalog rows; new writes must satisfy these rules.
ALTER TABLE product
    ADD CONSTRAINT chk_product_quantity CHECK (quantity >= 0) NOT VALID;
ALTER TABLE product
    ADD CONSTRAINT chk_product_price CHECK (price IS NOT NULL AND price > 0) NOT VALID;
