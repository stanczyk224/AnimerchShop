ALTER TABLE seller_photo
ADD CONSTRAINT uk_seller_photo_seller_id UNIQUE (seller_id);