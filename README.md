# AnimerchShop

A fullstack anime merchandise store with a Java/Spring Boot REST API and a React/TypeScript storefront inspired by Berserk.

AnimerchShop provides product and seller browsing, a guest basket, customer registration and login, delivery checkout, order history, image uploads, and PostgreSQL integration.

The backend follows a layered architecture. Docker Compose starts the storefront,
API and database together. An optional Vite server supports frontend development.

This is a learning and portfolio project. The frontend was built with AI assistance
(honestly: vibecoded) while learning React and TypeScript.

## Tech Stack

| Technology | Purpose |
|---|---|
| React 19 | Storefront components and state |
| TypeScript | Frontend types |
| Vite | Frontend development server and production build |
| CSS and SVG | Responsive dark fantasy design and animations |
| Java 21 | Programming language |
| Spring Boot 4 | Backend framework |
| Spring Data JPA | Database access |
| Hibernate | ORM |
| PostgreSQL | Relational database |
| Flyway | Database migrations |
| Spring Security | Application security |
| JWT | Authentication |
| Maven | Build and dependency management |
| MapStruct | DTO mapping |
| Lombok | Boilerplate reduction |
| Docker | Containerization |
| Docker Compose | Container orchestration |

## Features

### Storefront

- Public product feed with uploaded images, prices, stock and rating summaries.
- Combined search, seller/category filters, price ranges, sorting and pagination.
- Seller names link to their products; page sizes are 6, 12 or 20.
- Guest basket with visible per-product quantities and calculated totals.
- Registration and login, with frontend and backend input validation.
- Customer checkout with delivery details, basket review and order confirmation.
- Retry-safe checkout, including recovery after a lost response or page reload.
- My orders: paginated purchases, item statuses, saved delivery details and totals.
- Responsive layouts, lightweight animations and reduced-motion support.

The browsing basket lives in memory and resets on reload. Signing in preserves it.
Checkout asks for confirmation before replacing any previously saved server cart
with the reviewed basket. Pending checkout details and the purchase key are kept
in tab session storage until the result is known. Login tokens stay in memory,
so reloading requires signing in again.

Checkout records an order; **no online payment is collected**. A customer profile
is required, so an administrator account without one cannot place orders. Wishlist,
review editing and catalog administration have backend APIs but no dedicated UI yet.
See [frontend setup and behavior](Frontend/STORE-SETUP.md) for details.

### Product Management
Catalog listing, individual product details, product photos and category GET
endpoints, along with seller list/detail GET endpoints, are available publicly.
Product search supports combining `search`, `sellerId`, `categoryId`, `minPrice`
and `maxPrice` with pageable sorting. Prices must be non-negative, with minimum
not exceeding maximum. The storefront exposes these controls and customer order
history through My orders.

Catalog changes, account data, server cart APIs and checkout remain protected. The React storefront supports guest
browsing and a local basket, with authenticated checkout, delivery validation and
retry-safe order submission. The UI does not collect online payments.

- Product CRUD operations.
- Product descriptions, prices, and stock management.
- Product categories.
- Seller association.
- Product photo uploads.
- CSV bulk import with validation.

### Seller Management
- Seller CRUD operations.
- Seller information management.
- Seller photo uploads.

### Product Reviews
Product responses include `averageRating` (a number, or `null` without reviews) and
`reviewCount` (zero without reviews). These reflect current reviews, including rating
edits and deletions. Product lists and search pages load summaries with one grouped
query for all returned products. No stored counters or database migration are needed.

- Customers can review products with at least one delivered order item in their own order history.
- One review per customer per product, with a rating from 1 to 5 and required text (up to 5000 characters).
- Authenticated users can read paginated product reviews.

Create a review with `POST /api/products/{productId}/reviews` and a bearer token:

```json
{
  "rating": 5,
  "comment": "Great quality!"
}
```

Returns `201 Created`; customers who have not ordered the product receive `403 Forbidden`,
duplicate reviews return `409 Conflict`, and invalid input returns `400 Bad Request`.
The customer identity comes from the token. Only `DELIVERED` order items qualify for creating
a review; `PLACED`, `SHIPPED`, and `CANCELLED` items do not. Existing reviews remain editable
and deletable by their authors.

Read reviews with `GET /api/products/{productId}/reviews?page=0&size=20` and a bearer token.
Results default to newest first and include the customer's display name, rating, comment, and creation time.
Flyway migration `V16__create_review.sql` creates the review table on startup.

Authors can edit their review with `PUT /api/products/{productId}/reviews/{reviewId}`
using the same rating and comment JSON body, or delete it with `DELETE` at that URL
(`204 No Content`). Both require a bearer token; modifying someone else's review returns `403`.
An unknown review or mismatched product returns `404`.

Review responses include `edited` and `editedAt`. New reviews have `edited: false`
and `editedAt: null`. Changing the rating or trimmed comment sets `edited: true` and
records the latest edit time while preserving `createdAt`. Submitting unchanged content
preserves the previous edit status and date. Migration `V17__add_review_edited_at.sql`
adds the timestamp to existing reviews.

### Consumer Management
- Consumer CRUD operations.
- Automatic shopping cart creation.

### Wishlist

Customers can bookmark products without purchasing or reserving stock. Out-of-stock
products can be saved. All endpoints require a bearer token and a customer profile;
identity comes from the token, and each customer can access only their own wishlist.

- `POST /api/wishlist/items/{productId}`: save a product (`200 OK` with the bookmark).
  Saving again returns the existing bookmark, including concurrent duplicate saves.
  An unknown product returns `404`.
- `GET /api/wishlist?page=0&size=20`: return a page of bookmarks with product ID,
  name, current price, and save timestamp. Newest first, with ID descending for ties.
  Page must be non-negative; size must be between 1 and 100 (`400` otherwise).
- `DELETE /api/wishlist/items/{productId}`: remove your bookmark (`204 No Content`).
  Removing a missing bookmark also returns `204` and never modifies another customer's list.

Migration V21 enforces a unique customer/product pair. Deleting a product or customer
removes their associated bookmarks. `WishlistDatabaseTests` covers authenticated HTTP
requests, customer isolation, pagination, stock independence, constraints, and concurrent saves.

### Retrying checkout

`POST /api/order/checkout` requires a bearer token and an `Idempotency-Key` header.
It also requires a JSON body containing the delivery snapshot:

```json
{
  "deliveryAddress": {
    "recipientName": "Recipient Name",
    "phone": "+998901234567",
    "addressLine1": "12 Main Street",
    "addressLine2": "Apartment 4",
    "city": "Tashkent",
    "postalCode": "100000",
    "country": "Uzbekistan"
  }
}
```

Every field except `addressLine2` is required and must not be blank.
Phone numbers must contain 7–15 digits and can use an optional leading `+`, spaces,
parentheses and hyphens. Names, city and country accept international letters and
normal name punctuation; addresses cannot contain control characters. Postal codes
accept letters, numbers, spaces and hyphens. These rules are enforced by the backend
and the storefront. They validate formatting rather than actual ownership or delivery
service availability.

Maximum lengths are 255 for recipient name and each address line, 30 for phone, 100 for city and
country, and 20 for postal code. Invalid bodies return `400`. The address is saved
on the order and returned as `deliveryAddress` in checkout, order details, and order
history. Later customer profile changes do not change this snapshot. Migration V20
keeps `deliveryAddress: null` for existing orders whose historical address is unknown.

Use a fresh UUID for each new purchase and reuse that UUID when retrying the same
purchase after a timeout:

```http
POST /api/order/checkout
Authorization: Bearer <token>
Idempotency-Key: 6cf0de21-65fa-4a1a-b588-d878853a1112
```

Keys are case-sensitive, 1–128 characters, using letters, digits, `.`, `_`, `:`, or
`-`. Missing or invalid keys return `400`. Keys are scoped to the authenticated
customer, so different customers can use the same key independently. Successful
requests and retries return `201` with the same order ID. Retries return that
order's current details, including any later status changes, without consuming
the current cart or deducting stock again. Use a new key for a new purchase.
Failed checkout transactions do not reserve the key; the same key can be retried
after correcting the problem. Keys remain associated with their orders, including
cancelled orders. Migration V19 adds the nullable key for legacy orders and enforces
uniqueness per customer. Clients must now supply this header for checkout.

Retries must send the same delivery field values (JSON property order does not
matter). A reused key with different delivery details returns `409 Conflict` without
changing the saved order, cart, or stock. Null and omitted address line 2 are
equivalent; other values are compared exactly, including whitespace and case.
An order created before delivery snapshots cannot be retried with a new address;
retrieve it through the order details endpoint instead.

### Order details and history

`GET /api/order/{orderId}` returns one order with items, statuses, and totals.
Customers can view only their own orders (`403` for another customer's order).
Admins can view any order without a customer profile. Unknown orders return `404`.
All order endpoints require a bearer token.

Order lists accept zero-based `page` (default `0`) and `size` (default `20`, range
`1`–`100`). Negative pages and invalid sizes return `400`. Results are ordered by
creation time descending, then order ID descending to break ties:

- `GET /api/order/my?page=0&size=20`: the authenticated customer's orders.
- `GET /api/order?page=0&size=20`: all orders, admin only.
- `GET /api/order/consumer/{consumerId}?page=0&size=20`: one customer's orders,
  admin only (`404` for an unknown customer).

**API response change:** these list endpoints now return a page object with
`content`, `number`, `size`, `totalElements`, and `totalPages` instead of a plain
array. Pages beyond the available results have empty `content`.

### Cancelling an order item

Authenticated customers can cancel their own order items with
`PATCH /api/order/{orderId}/items/{itemId}/cancel` (no request body).
Returns the updated item with `200 OK`. Only `PLACED` items can be cancelled;
shipped, delivered, or already cancelled items return `400 Bad Request`.
Cancelling another customer's item returns `403 Forbidden`; an unknown item or
mismatched order returns `404 Not Found`. Cancellation restores the item's stock
once, including when requests compete with another cancellation or admin shipping.
The order total remains the original purchase total.

Order responses also include `originalTotal`, `cancelledTotal`, and `activeTotal`.
These are calculated from saved order-item unit prices and quantities, so later
catalog price changes do not affect them. `originalTotal` includes every item,
`cancelledTotal` includes only `CANCELLED` items, and `activeTotal` is their difference
(including placed, shipped, and delivered items). The existing `totalPrice` field
continues to hold the original checkout total. For example, a 100.00 order with
30.00 cancelled returns 100.00 original, 30.00 cancelled, and 70.00 active.

Database regression tests in `CheckoutDatabaseTests` require a disposable PostgreSQL
database configured through `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`, plus the
application's admin and JWT environment variables. Run them with `./mvnw test`.

### Shopping Cart
- Add products to the cart.
- Update product quantities.
- Remove products from the cart.
- Retrieve cart items.
- Stock availability validation.

### Authentication
- JWT-based authentication.
- Configurable administrator credentials.
- Protected API endpoints.

### File Storage
- Local storage for product and seller images.
- Image metadata stored in PostgreSQL.
- Persistent Docker volume for uploaded files.

### Database
- PostgreSQL integration.
- Flyway database migrations.
- Hibernate schema validation.

## Architecture

The application follows a layered architecture:

```text
AnimerchShop/
├── Frontend/            # React + TypeScript storefront
│   ├── src/             # Catalog, checkout, account forms and order history
│   └── tests/           # Frontend validation and cart regression tests
├── src/main/java/       # Spring Boot application
├── src/main/resources/  # Configuration and Flyway migrations
├── src/test/java/       # Unit, HTTP/security and PostgreSQL regression tests
└── compose.yaml         # API and PostgreSQL services
```

Backend packages:

```text
src/main/java/com/shop/animerch/
│
├── controller/     # REST API endpoints
├── service/        # Business logic
├── repository/     # Database operations
├── model/          # JPA entities
├── record/         # DTOs
├── mapper/         # MapStruct mappers
├── exception/      # Custom exceptions and handlers
├── config/         # Application configuration
└── utils/          # Utility classes
```

## Getting Started

### Prerequisites

- Git
- Docker Desktop or Docker Engine
- Docker Compose

Java, Maven, Node.js, npm and PostgreSQL do not need to be installed locally when
using Docker Compose. Node.js 24 is needed only for the optional local frontend workflow.

### 1. Clone the repository

```bash
git clone https://github.com/stanczyk224/AnimerchShop.git
cd AnimerchShop
```

### 2. Configure environment variables

Create your `.env` file from `.env.example`.

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Linux / macOS:

```bash
cp .env.example .env
```

Open `.env` and configure your own credentials and secrets.

### 3. Start the application

```bash
docker compose up
```

Docker Compose will:

1. Build the Spring Boot application using a multi-stage Dockerfile.
2. Create a PostgreSQL container.
3. Initialize the database.
4. Wait for PostgreSQL to become healthy.
5. Start the Spring Boot application.
6. Execute Flyway migrations.
7. Build and test the React frontend, then serve it through Nginx once the API is healthy.

Open the storefront at [http://localhost:3000](http://localhost:3000).
Use Sign up to create a customer account; browsing and the basket work without one.
Set `FRONTEND_PORT` in `.env` to use a different storefront port.

The first startup builds the images automatically. After changing source files,
run `docker compose up --build` to rebuild. Add `-d` to run in the background.

### 4. Verify the containers

```bash
docker compose ps
```

View application logs:

```bash
docker compose logs -f app
```

The backend will be available at:

```text
http://localhost:8080
```

### Optional: frontend development with hot reload

Docker already serves the compiled storefront at port 3000. To develop with hot
reload instead, install Node.js 24 and run these from the repository root:

```bash
cd Frontend
npm ci
npm run dev
```

Open the URL Vite prints, normally [http://localhost:5173](http://localhost:5173).
Keep the Docker backend running while using this development server.

Vite forwards `/api` requests to the backend at `http://localhost:8080`.
`ANIMERCH_API_TARGET` can override the proxy target
for isolated development/testing. Never put admin passwords or JWT signing secrets
in frontend files.

To build and check the frontend from its folder:

```bash
npm test
npm run build
```

The production output is `Frontend/dist/`. The frontend Docker image serves this
output with Nginx and forwards `/api` to `app:8080`. When hosting the static files
elsewhere, configure an equivalent API proxy; Vite's proxy is not included in the build.
Browser checkout QA is documented in [STORE-SETUP.md](Frontend/STORE-SETUP.md) and
must use a disposable database because it creates test customers and orders.

## API Documentation — Swagger

Swagger UI provides interactive API documentation and allows you to test endpoints directly from your browser.

**Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### How to authenticate using JWT

To access protected endpoints, you first need to obtain an access token.

**Step 1. Obtain a JWT token**

Open Swagger UI and find the authentication endpoint:

```http
POST /api/auth/login
```

1. Expand the login endpoint.
2. Click **Try it out**.
3. Enter your administrator credentials in the request body.
4. Click **Execute**.

The response should contain a JWT token.

**Step 2. Authorize Swagger**

1. Copy the JWT token from the login response.
2. Click the **Authorize** button at the top of Swagger UI.
3. Paste the token into the authorization field.
4. Click **Authorize**.
5. Close the authorization window.

If the configured OpenAPI security scheme uses HTTP Bearer authentication, paste the token itself without adding `Bearer ` manually. Swagger will add the prefix automatically.

**Step 3. Test protected endpoints**

You can now execute protected endpoints directly through Swagger UI. Swagger will include the JWT token in the `Authorization` header.

```http
Authorization: Bearer <your-jwt-token>
```

If you restart Swagger UI or open a new browser session, you may need to authorize again.

## Docker Configuration

The backend uses a multi-stage Docker build:

- **Build stage:** Maven compiles and packages the Spring Boot application.
- **Runtime stage:** The application runs using a Java 21 JRE image.

The frontend uses a Node.js 24 build stage (`npm ci`, tests, TypeScript and Vite build)
and an Nginx runtime stage containing only the built assets and proxy configuration.
It serves React fallback routes and waits for the backend health check at startup.

Docker Compose manages three services:

| Service | Description |
|---|---|
| app | Spring Boot REST API |
| db | PostgreSQL database |
| frontend | Nginx storefront at `localhost:3000`, with an internal API proxy |

On networks with HTTPS inspection, npm inside Docker may need the network's
already-trusted CA. [compose.ca.example.yaml](compose.ca.example.yaml) shows an
optional build-secret override: export the public CA as `.local-certs/npm-ca.pem`
and copy the example to `compose.override.yaml`. Both local files are ignored by
Git. The CA is mounted only during dependency installation and is not copied into
the runtime image. TLS verification remains enabled.

### Persistent Storage

Docker named volumes preserve application data:

| Volume | Purpose |
|---|---|
| `postgres_data` | PostgreSQL database files |
| `uploads_data` | Uploaded product and seller images |

Data persists when containers are stopped or recreated.

### Stop the application

```bash
docker compose down
```

This stops and removes containers while preserving named volumes.

**Warning:** The following command removes containers and their named volumes, including database data and uploaded images:

```bash
docker compose down -v
```

## Continuous integration

The existing GitHub Actions workflow verifies the backend. Frontend tests and builds
are currently run separately using the commands above.

The `CI` workflow in `.github/workflows/ci.yml` runs on every push, pull request,
and manual dispatch. It starts a fresh PostgreSQL 17 service, waits for database
health, installs Java 21, and runs `bash ./mvnw -B -ntp verify`. Spring Boot runs
Flyway migrations against this empty database and validates the resulting schema.
The Maven wrapper uses the repository's configured Maven version.

CI uses disposable test credentials and a test-only JWT key. No repository secrets
or GitHub environment approval are required. These values must not be used for a
deployed application. The workflow has read-only repository permissions.

After testing, `.github/scripts/check_test_reports.py` writes a per-suite summary
and fails the job if a required database test suite is missing or skipped, or any test
reports a failure or error. This prevents a green build caused by missing `DB_URL`.
The report check and artifact upload run even after a build failure.

In GitHub, open **Actions → CI → a workflow run** to view the summary and download
the `test-reports` artifact (Surefire reports and application logs, kept for 14 days).
Maven failures fail the workflow; the upload does not hide them. The workflow becomes
active when these files are pushed to GitHub. Requiring CI before merge is a separate
repository branch-protection setting.

To reproduce it locally, point `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` at a
disposable PostgreSQL database and supply the application admin/JWT variables.
Run `./mvnw -B -ntp verify` (Windows: `.\mvnw.cmd -B -ntp verify`), then
`python .github/scripts/check_test_reports.py`.

References: [GitHub PostgreSQL services](https://docs.github.com/en/actions/tutorials/use-containerized-services/create-postgresql-service-containers)
and [artifact uploads](https://github.com/actions/upload-artifact).

## Development focus

The project is under active development.

Current focus:
- Connecting the remaining backend features to the storefront.
- Frontend administration, wishlist and review workflows.
- Frontend CI and deployment alongside the Docker backend.

## Author

**stanczyk224**

GitHub: [github.com/stanczyk224](https://github.com/stanczyk224)

---

*Built as a fullstack learning and portfolio project.*
