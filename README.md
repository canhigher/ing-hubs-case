# ING Brokerage API

A RESTful API service for a brokerage platform that allows customers to manage assets and place orders for buying and selling various financial instruments.

## Table of Contents

- [Features](#features)
- [Technologies](#technologies)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Security](#security)
- [Testing](#testing)
- [License](#license)

## Features

- **User Authentication**: Secure registration and login with JWT token-based authentication
- **Role-Based Access Control**: Different permissions for customers and administrators
- **Asset Management**: View asset balances and update them (admin only)
- **Order Management**: Create, view, cancel, and match orders
- **Filtering Options**: Filter orders by date range, status, and customer ID
- **Security**: Protected endpoints with proper authorization checks

## Technologies

- **Java 17**
- **Spring Boot 3.x**
- **Spring Security** with JWT authentication
- **Spring Data JPA** for database operations
- **H2/MySQL/PostgreSQL** (configurable database)
- **Maven** for dependency management
- **JUnit 5** and Spring Boot Test for testing
- **Swagger/OpenAPI** for API documentation

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.6 or later
- Your preferred IDE (IntelliJ IDEA, Eclipse, VS Code)

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/your-username/ing-brokerage-api.git
   cd ing-brokerage-api
   ```

2. Build the project:

   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080` by default.

### Configuration

The application uses `application.properties` or `application.yml` for configuration. Sample configuration:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/brokerage_db
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update

# JWT Configuration
app.jwt.secret=yourSecretKey
app.jwt.expiration=86400000

# Server Configuration
server.port=8080
```

## API Documentation

The API provides the following main endpoints:

### Authentication

- `POST /api/auth/register`: Register a new user
- `POST /api/auth/login`: Authenticate and get JWT token

### Assets

- `GET /api/assets`: Get customer assets
- `POST /api/assets/balance`: Add balance to a customer's asset (admin only)

### Orders

- `POST /api/orders`: Create a new order
- `GET /api/orders`: Get all orders (admin) or filtered by customer ID
- `GET /api/orders/{id}`: Get order by ID
- `DELETE /api/orders/{id}`: Cancel an order
- `PUT /api/orders/{id}/match`: Match an order (admin only)

For detailed API documentation, run the application and visit:

```
http://localhost:8080/swagger-ui.html
```

## Security

The API uses JWT (JSON Web Token) for authentication. To access protected endpoints:

1. Register or login to get a JWT token
2. Include the token in the Authorization header of your requests:
   ```
   Authorization: Bearer your_jwt_token
   ```

## Testing

The project includes comprehensive unit and integration tests. To run the tests:

```bash
mvn test
```

A Postman collection is also included for manual testing. See [POSTMAN.md](POSTMAN.md) for details on how to use it.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
