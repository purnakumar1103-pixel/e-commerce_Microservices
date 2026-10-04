-- V1 is retained as an immutable baseline. This migration aligns the schema
-- with the Product and Inventory Service implementation guide.

ALTER TABLE products
    ALTER COLUMN sku TYPE VARCHAR(64),
    ALTER COLUMN name TYPE VARCHAR(160),
    ALTER COLUMN description TYPE TEXT,
    ALTER COLUMN price TYPE NUMERIC(19, 2);

ALTER TABLE products DROP CONSTRAINT IF EXISTS ck_products_price_positive;
ALTER TABLE products ADD CONSTRAINT ck_products_price_nonnegative CHECK (price >= 0);
ALTER TABLE products ALTER COLUMN category_id SET NOT NULL;

ALTER TABLE inventory
    ADD COLUMN reserved_quantity INTEGER NOT NULL DEFAULT 0;
ALTER TABLE inventory ADD CONSTRAINT ck_inventory_reserved_nonnegative CHECK (reserved_quantity >= 0);

ALTER TABLE inventory_reservations
    ALTER COLUMN order_reference TYPE UUID USING order_reference::uuid;
UPDATE inventory_reservations SET status = 'RESERVED' WHERE status = 'ACTIVE';
ALTER TABLE inventory_reservations DROP CONSTRAINT IF EXISTS ck_reservation_status;
ALTER TABLE inventory_reservations ADD CONSTRAINT ck_reservation_status
    CHECK (status IN ('RESERVED', 'RELEASED'));

ALTER TABLE reservation_items
    ALTER COLUMN unit_price TYPE NUMERIC(19, 2);

INSERT INTO categories (id, name, active, created_at) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Books', TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000002', 'Electronics', TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000003', 'Clothing', TRUE, NOW()),
    ('00000000-0000-0000-0000-000000000004', 'Home', TRUE, NOW())
ON CONFLICT (id) DO NOTHING;
