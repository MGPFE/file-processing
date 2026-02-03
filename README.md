# File Processing Service

REST API for secure file management with asynchronous malware scanning integration.

## Features

- File Upload/Download with JWT Authentication
- VirusTotal API Integration for Security Scanning
- Asynchronous Processing via Apache Kafka
- SHA256 Checksum Deduplication
- Distributed Rate Limiting with Redis
- Idempotency Protection for POST Requests
- File Visibility Control (Private/Public)
- Role-Based Authorization
- Comprehensive Error Handling

## Tech Stack

- Spring Boot 4.0.1
- Spring Security with JWT
- Apache Kafka
- Redis
- PostgreSQL/H2
- Spring Data JPA
- Docker

## API Endpoints

### Authentication
- POST /auth/sign-up - Register user
- POST /auth/sign-in - Login

### File Operations (Require Authentication)
- GET /files - List user files (paginated)
- GET /files/{uuid} - Get file details
- GET /files/download/{uuid} - Download file
- POST /files - Upload file (requires Idempotency-Key header)
- DELETE /files/{uuid} - Delete file
- PATCH /files/{uuid} - Update file visibility

## Quick Start

### Prerequisites
- Java 25+
- Maven 3.9+
- Docker & Docker Compose

### Local Development

1. Clone repository:
```bash
git clone https://github.com/MGPFE/file-processing.git
cd file-processing
```

2. Configure environment variables in .env

3. Start dependencies:
```bash
docker-compose up -d
```

4. Build and run:
```bash
mvn clean package
mvn spring-boot:run
```

Application runs on http://localhost:8080/api/v1

## Configuration

Key environment variables:
- SCAN_API_KEY: VirusTotal API key
- JWT_SECRET: JWT signing secret
- DB_NAME
- DB_USERNAME
- DB_PASSWORD
- DB_HOST
- DB_PORT
- spring.kafka.bootstrap-servers: Kafka brokers
- spring.redis.host: Redis host

## Architecture

The application follows a layered architecture pattern:

- Controller Layer: REST endpoints (FileController, JwtAuthController)
- Service Layer: Business logic (FileService, JwtAuthService)
- Repository Layer: Data access (JPA Repositories)
- Database: PostgreSQL persistence

Background processes:
- Kafka Consumer: Asynchronous file scanning
- Scheduled Tasks: Retry failed scans
- Orphan files cleanup: Clean up files that are present in storage but not in DB
- Interceptors: Rate limiting and idempotency checks

## Key Features

### File Security
- Files compressed before external scanning
- Malicious files automatically deleted
- Checksum-based duplicate detection

### Rate Limiting
- Per-IP request throttling via Bucket4j + Redis
- Returns 429 status with Retry-After header

### Idempotency
- Requires Idempotency-Key header for POST requests
- Prevents duplicate processing
- Redis-backed state management

### Error Handling
- Global exception handler with unified response format
- Proper HTTP status codes (400, 404, 429, 500)
- Structured error responses with timestamps

## Testing

- Unit tests with Mockito
- Integration tests with MockMvc
- Comprehensive test coverage for services, controllers, and interceptors

Run tests:
```bash
mvn test
```

## Security

- JWT token-based authentication
- Password requirements: 8-128 characters
- Customizable file type restrictions (jpg, png, jpeg)
- Max upload size: 20MB
- User-level access control

## License

MIT