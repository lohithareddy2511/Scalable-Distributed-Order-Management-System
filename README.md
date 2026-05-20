# Scalable Distributed Order Management System

A production-ready, scalable backend order management system built with Java, Spring Boot, and PostgreSQL for distributed transaction and order processing workflows.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    REST API Layer                         │
│  (CustomerController, ProductController, OrderController,│
│   PaymentController, InventoryController)                │
├──────────────────────────────────────────────────────────┤
│                   Service Layer                           │
│  (CustomerService, ProductService, OrderService,         │
│   PaymentService, InventoryService)                      │
├──────────────────────────────────────────────────────────┤
│                  Repository Layer                         │
│  (Spring Data JPA Repositories)                          │
├──────────────────────────────────────────────────────────┤
│                   Domain Layer                            │
│  (Entities, Enums, Value Objects)                        │
├──────────────────────────────────────────────────────────┤
│                   Database Layer                          │
│  (PostgreSQL + Flyway Migrations)                        │
└──────────────────────────────────────────────────────────┘
```

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Language |
| Spring Boot 3.2 | Framework |
| Spring Data JPA | Data Access |
| PostgreSQL 16 | Database |
| Flyway | DB Migration |
| MapStruct | Object Mapping |
| Lombok | Boilerplate Reduction |
| Spring AOP | Cross-cutting Concerns |
| Micrometer + Prometheus | Metrics & Monitoring |
| SpringDoc OpenAPI | API Documentation |
| JUnit 5 + Mockito | Testing |
| Testcontainers | Integration Testing |
| Docker | Containerization |
| JaCoCo | Code Coverage |

## Features

- **RESTful API Design** - Clean, versioned APIs following REST best practices
- **Distributed Transaction Management** - Transactional workflows with inventory reservation/release
- **Order Lifecycle Management** - Full state machine for order status transitions
- **Inventory Management** - Real-time stock tracking with pessimistic locking
- **Payment Processing** - Payment workflow with status tracking and refunds
- **Request Validation** - Bean Validation with custom error responses
- **Centralized Exception Handling** - Global exception handler with structured error responses
- **Logging & Debugging** - Structured logging with AOP-based request/response tracking
- **Monitoring** - Prometheus metrics with custom business metrics
- **Database Migrations** - Versioned schema migrations with Flyway
- **Pagination & Sorting** - Flexible querying with pagination support
- **API Documentation** - Interactive Swagger UI with OpenAPI 3.0 specs
- **Automated Testing** - Unit & integration tests with 80%+ coverage target
- **Docker Support** - Containerized deployment with Docker Compose

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 16 (or Docker)

### Option 1: Run with Docker Compose

```bash
docker-compose up -d
```

The application will be available at `http://localhost:8080/api`

### Option 2: Local Development

1. **Start PostgreSQL:**
```bash
# Create database
createdb order_management_db
```

2. **Configure database** (edit `src/main/resources/application.properties` if needed)

3. **Build and run:**
```bash
mvn clean install
mvn spring-boot:run
```

### Run Tests

```bash
mvn test
```

### Generate Coverage Report

```bash
mvn test jacoco:report
# Report at target/site/jacoco/index.html
```

## API Endpoints

### Customers
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/customers` | Create customer |
| GET | `/api/v1/customers/{id}` | Get customer by ID |
| GET | `/api/v1/customers` | List all customers (paginated) |
| GET | `/api/v1/customers/search?query=` | Search customers |
| PUT | `/api/v1/customers/{id}` | Update customer |
| DELETE | `/api/v1/customers/{id}` | Delete customer |

### Products
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/products` | Create product |
| GET | `/api/v1/products/{id}` | Get product by ID |
| GET | `/api/v1/products/sku/{sku}` | Get product by SKU |
| GET | `/api/v1/products` | List all products (paginated) |
| GET | `/api/v1/products/active` | List active products |
| GET | `/api/v1/products/category/{category}` | Products by category |
| GET | `/api/v1/products/search?query=` | Search products |
| GET | `/api/v1/products/low-stock?threshold=` | Low stock products |
| PUT | `/api/v1/products/{id}` | Update product |
| DELETE | `/api/v1/products/{id}` | Delete product |

### Orders
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/orders` | Create order |
| GET | `/api/v1/orders/{id}` | Get order by ID |
| GET | `/api/v1/orders/number/{orderNumber}` | Get order by number |
| GET | `/api/v1/orders` | List all orders (paginated) |
| GET | `/api/v1/orders/customer/{customerId}` | Orders by customer |
| GET | `/api/v1/orders/status/{status}` | Orders by status |
| PATCH | `/api/v1/orders/{id}/status` | Update order status |
| POST | `/api/v1/orders/{id}/cancel` | Cancel order |

### Payments
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/payments` | Process payment |
| GET | `/api/v1/payments/{id}` | Get payment by ID |
| GET | `/api/v1/payments/order/{orderId}` | Payments by order |
| POST | `/api/v1/payments/{id}/refund` | Refund payment |

### Inventory
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/inventory/{productId}/add` | Add stock |
| POST | `/api/v1/inventory/{productId}/adjust` | Adjust stock |

## API Documentation

Once the application is running, access the interactive API docs at:
- **Swagger UI:** `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api/api-docs`

## Monitoring

- **Health Check:** `http://localhost:8080/api/actuator/health`
- **Metrics:** `http://localhost:8080/api/actuator/metrics`
- **Prometheus:** `http://localhost:8080/api/actuator/prometheus`

## Order Status Flow

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → REFUNDED
    ↓         ↓           ↓
 CANCELLED  CANCELLED   CANCELLED
```

## Project Structure

```
src/
├── main/
│   ├── java/com/ordermanagement/
│   │   ├── OrderManagementApplication.java
│   │   ├── aspect/          # AOP (logging, metrics)
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── domain/
│   │   │   ├── entity/      # JPA entities
│   │   │   └── enums/       # Domain enumerations
│   │   ├── dto/
│   │   │   ├── mapper/      # MapStruct mappers
│   │   │   ├── request/     # Request DTOs
│   │   │   └── response/    # Response DTOs
│   │   ├── exception/       # Custom exceptions & handler
│   │   ├── repository/      # Spring Data repositories
│   │   └── service/         # Business logic
│   └── resources/
│       ├── application.properties
│       └── db/migration/    # Flyway SQL migrations
├── test/
│   ├── java/com/ordermanagement/
│   │   ├── controller/      # Controller tests (MockMvc)
│   │   └── service/         # Service unit tests
│   └── resources/
│       └── application-test.properties
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## Design Principles

- **Clean Architecture** - Clear separation of concerns with layered design
- **SOLID Principles** - Single responsibility, interface segregation
- **Domain-Driven Design** - Rich domain entities with business logic
- **Transactional Integrity** - Proper transaction boundaries with rollback support
- **Fail-Fast Validation** - Input validation at controller layer
- **Defensive Programming** - Null checks, state validation, pessimistic locking

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit changes (`git commit -m 'Add my feature'`)
4. Push to branch (`git push origin feature/my-feature`)
5. Open a Pull Request
