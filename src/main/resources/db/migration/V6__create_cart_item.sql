CREATE TABLE cart_item (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    CONSTRAINT fk_cart_item_cart
        FOREIGN KEY (cart_id)
        REFERENCES cart(id),
    product_id BIGINT NOT NULL,
    CONSTRAINT fk_cart_item_product
        FOREIGN KEY (product_id)
        REFERENCES product(id),
    quantity INTEGER NOT NULL CHECK (quantity >= 1)
);