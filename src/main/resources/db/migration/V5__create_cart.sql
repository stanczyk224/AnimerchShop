CREATE TABLE cart (
    id BIGSERIAL PRIMARY KEY,
    consumer_id BIGINT NOT NULL,
    CONSTRAINT fk_cart_consumer
        FOREIGN KEY (consumer_id)
        REFERENCES consumer(id)
);