# Product and Inventory Service

Spring Boot service for catalog data and stock reservations. It runs on port `8082` by default and owns the `product_db` PostgreSQL database.

## Run locally

Prerequisites: Java 21, Maven, and PostgreSQL.

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

The default local database settings are:

```text
JDBC URL:  jdbc:postgresql://localhost:5434/product_db
Username: product_app
Password: PRODUCT_DB_PASSWORD
```

Override settings with environment variables. Never commit real credentials:

```text
PRODUCT_SERVICE_PORT
PRODUCT_DB_URL
PRODUCT_DB_USERNAME
PRODUCT_DB_PASSWORD
JWT_ISSUER
JWT_SECRET                 # at least 32 characters; shared with User Service
INTERNAL_API_KEY           # shared with Order Service
MAX_PAGE_SIZE              # defaults to 100
```

Flyway creates the schema from `src/main/resources/db/migration/V1__create_product_inventory_schema.sql`.

## API

Public catalog:

```text
GET    /api/v1/products?q=&categoryId=&page=0&size=20&sort=name
GET    /api/v1/products/{id}
```

Administrator APIs require a JWT with `role=ADMIN`:

```text
GET    /api/v1/admin/products?q=&categoryId=&page=0&size=20&sort=name
POST   /api/v1/admin/products
PUT    /api/v1/admin/products/{id}
PATCH  /api/v1/admin/products/{id}/stock
DELETE /api/v1/admin/products/{id}
```

Order Service APIs require this header:

```text
X-Internal-Api-Key: <configured key>
```

```text
POST /internal/v1/inventory/reservations
POST /internal/v1/inventory/reservations/{id}/release
```

All responses include `X-Correlation-ID`. Validation and business failures use the `errorCode` response shape documented in the implementation guide. Health and API documentation are available at `/actuator/health`, `/v3/api-docs`, and `/swagger-ui.html`.

## Tests

Fast tests use an in-memory H2 database and disable Flyway so they can run without PostgreSQL:

```bash
./mvnw clean test
```

The tests cover atomic reservation rollback, available/reserved quantity changes, stock restoration, API authorization, shared errors, and idempotent release. PostgreSQL/Testcontainers verification is part of the service completion checklist.
