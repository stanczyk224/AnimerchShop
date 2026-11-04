CREATE TABLE product_photo (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(255) NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT fk_product_photo_product
        FOREIGN KEY (product_id)
        REFERENCES product(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_product_photo_product_id
    ON product_photo(product_id);