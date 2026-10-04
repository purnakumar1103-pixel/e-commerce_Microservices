CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uq_categories_name_lower ON categories (LOWER(name));

CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID REFERENCES categories(id),
    sku VARCHAR(80) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    price NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_products_price_positive CHECK (price > 0),
    CONSTRAINT ck_products_currency_upper CHECK (currency = UPPER(currency))
);

CREATE UNIQUE INDEX uq_products_sku_lower ON products (LOWER(sku));
CREATE INDEX ix_products_active_name ON products (active, name);

CREATE TABLE inventory (
    product_id UUID PRIMARY KEY REFERENCES products(id),
    available_quantity INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_inventory_quantity_nonnegative CHECK (available_quantity >= 0)
);

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    order_reference VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    released_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_reservation_status CHECK (status IN ('ACTIVE', 'RELEASED'))
);

CREATE TABLE reservation_items (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES inventory_reservations(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    CONSTRAINT ck_reservation_quantity_positive CHECK (quantity > 0),
    CONSTRAINT uq_reservation_product UNIQUE (reservation_id, product_id)
);

CREATE INDEX ix_reservation_items_product ON reservation_items (product_id);

CREATE TABLE stock_adjustments (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity_change INTEGER NOT NULL,
    reason VARCHAR(500) NOT NULL,
    changed_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_stock_adjustments_product_created ON stock_adjustments (product_id, created_at);
