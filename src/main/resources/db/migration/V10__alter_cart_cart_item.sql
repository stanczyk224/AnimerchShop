ALTER TABLE cart
DROP CONSTRAINT fk_cart_consumer;

ALTER TABLE cart
ADD CONSTRAINT fk_cart_consumer
    FOREIGN KEY (consumer_id)
    REFERENCES consumer(id)
    ON DELETE CASCADE;

ALTER TABLE cart_item
DROP CONSTRAINT fk_cart_item_cart;

ALTER TABLE cart_item
ADD CONSTRAINT fk_cart_item_cart
    FOREIGN KEY (cart_id)
    REFERENCES cart(id)
    ON DELETE CASCADE;