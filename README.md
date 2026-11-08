# AnimerchShop

A RESTful backend application for an anime merchandise store, built with Java and Spring Boot.

AnimerchShop provides product and seller management, shopping cart functionality, JWT authentication, image uploads, and PostgreSQL integration.

The project follows a layered architecture and uses Docker Compose for simplified deployment and development.

## Tech Stack

| Technology | Purpose |
|---|---|
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

### Product Management
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
- Customers can review products present in their order history after checkout.
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
The customer identity comes from the token. Order history currently has no payment or delivery status,
so an existing checkout qualifies for a review.

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

Java, Maven, and PostgreSQL do not need to be installed locally when using Docker Compose.

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
docker compose up --build -d
```

Docker Compose will:

1. Build the Spring Boot application using a multi-stage Dockerfile.
2. Create a PostgreSQL container.
3. Initialize the database.
4. Wait for PostgreSQL to become healthy.
5. Start the Spring Boot application.
6. Execute Flyway migrations.

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

The project uses a multi-stage Docker build:

- **Build stage:** Maven compiles and packages the Spring Boot application.
- **Runtime stage:** The application runs using a Java 21 JRE image.

Docker Compose manages two services:

| Service | Description |
|---|---|
| app | Spring Boot REST API |
| db | PostgreSQL database |

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

## Project Status

The project is under active development.

Current focus:
- Backend functionality and business logic.
- Authentication and authorization.
- Docker-based deployment.
- CI/CD pipeline implementation.

## Author

**stanczyk224**

GitHub: [github.com/stanczyk224](https://github.com/stanczyk224)

---

*Built as a Java Backend portfolio project.*
