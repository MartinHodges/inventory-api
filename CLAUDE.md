# Inventory API

Spring Boot backend API for the Inventory Management application.

## Tech Stack

- Java 21
- Spring Boot 3.4.1
- PostgreSQL with Flyway migrations
- Maven build

## Project Structure

```
src/main/java/com/requillion/solutions/inventory/
├── controller/    # REST endpoints
├── dto/           # Data Transfer Objects
├── model/         # JPA entities
├── repository/    # Spring Data JPA repositories
├── service/       # Business logic
├── security/      # Auth (RequestIdFilter, RequestAspect, UserContext)
├── exception/     # Custom exceptions and GlobalExceptionHandler
├── util/          # Utilities (LoggerUtil)
└── config/        # Configuration classes
```

## Key Patterns

### Security
- `x-userinfo` header (base64-encoded JSON) contains user claims from Keycloak
- `RequestIdFilter` decodes header, creates `RequestContext` with UUID requestId
- `RequestAspect` enriches context with User entity, auto-creates users on first sight
- `UserContext` provides ThreadLocal access to current request context

### Logging
- Use `LoggerUtil` for all logging - auto-prefixes with requestId
- Example: `LoggerUtil.info(log, "Created item: %s", itemId)`

### Exceptions
- `NotFoundException` (404), `NotAuthorizedException` (403), `NotAuthenticatedException` (401), `BadInputException` (400)
- All extend `ApiException` with userMessage (client-safe) and systemMessage (logs only)
- `GlobalExceptionHandler` handles all exceptions centrally

### Database
- Schema: `inventory`
- Flyway migrations in `src/main/resources/db/migration/`
- Naming: `V1.XX__description.sql`
- JPA hibernate.ddl-auto: validate (never auto-generate)
- All timestamps stored as UTC (TIMESTAMP WITH TIME ZONE)

## Running Locally

```bash
# Set environment variables or use application-dev.yml
export DATABASE_URL=jdbc:postgresql://localhost:5432/inventory
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres

# Run
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Commit Messages

- Use prefixes: `fix:`, `feat:`, `task:`, `perf:`

## API Endpoints

- `GET /api/v1/health` - Health check
- `GET /api/v1/me` - Current user info (requires auth)

## Notes

- Never build docker images without explicit instruction
- Images stored in database (not filesystem)
- All times in UTC, converted to browser timezone by UI
