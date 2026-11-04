CREATE TABLE seller_photo (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(255) NOT NULL,
    seller_id BIGINT NOT NULL,

    CONSTRAINT fk_seller_photo_seller
        FOREIGN KEY (seller_id)
        REFERENCES seller(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_seller_photo_seller_id
    ON seller_photo(seller_id);