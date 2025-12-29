# PeerLink Backend

A hybrid peer-to-peer file sharing Java backend application built with Java SE(com.sun.net.httpserver) and Maven.

## Overview

PeerLink Backend is a robust REST API service that enables secure file sharing between peers. It provides file upload, download, and peer management capabilities with support for invite-based access control.

## Prerequisites

- **Java 17** or higher
- **Maven 3.8.0** or higher
- **Git**

## Getting Started

### 1. Clone the Repository

```bash
git clone <backend-repo-url>
cd peerlink-backend
```

### 2. Install Dependencies

Maven will automatically download dependencies when you build the project:

```bash
mvn clean install
```

### 3. Build the Project

```bash
mvn clean package
```

This will compile the code and create a JAR file in the `target/` directory.

### 4. Run the Application

#### Option A: Using Maven
```bash
mvn spring-boot:run
```

#### Option B: Using the JAR file
```bash
java -jar target/peerlink-backend-1.0-SNAPSHOT.jar
```

The application will start and typically run on `http://localhost:8080` (check console for actual port).

## Project Structure

```
peerlink-backend/
├── src/
│   ├── main/java/p2p/
│   │   ├── App.java                 # Main application entry point
│   │   ├── controller/
│   │   │   └── FileController.java   # REST API endpoints for file operations
│   │   ├── services/
│   │   │   └── FileSharer.java       # Core file sharing logic
│   │   └── utils/
│   │       └── UploadUtils.java      # Utility functions for file upload
│   └── test/java/p2p/
│       └── AppTest.java              # Unit tests
├── pom.xml                           # Maven project configuration
└── Dockerfile                        # Docker configuration
```

## API Endpoints

### File Upload
- **POST** `/api/upload` - Upload a file
  - Request: multipart/form-data with file
  - Response: File metadata and sharing code

### File Download
- **GET** `/api/download/{fileId}` - Download a file
  - Query params: `inviteCode` (if peer-restricted)
  - Response: File binary data

### Peer Management
- **POST** `/api/peers/invite` - Generate invite code
- **GET** `/api/peers/{peerId}` - Get peer information
- **POST** `/api/peers/verify` - Verify peer access with invite code

For detailed API documentation, refer to the API specification or generate Swagger docs.

## Development

### Running Tests

```bash
mvn test
```

### Code Quality

```bash
# Build with all checks
mvn clean verify

# Run specific test class
mvn test -Dtest=AppTest
```

### Debugging

Run with debug mode:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
```

Connect your IDE debugger to port 5005.

## Docker

### Build Docker Image

```bash
docker build -t peerlink-backend:latest .
```

### Run Docker Container

```bash
docker run -p 8080:8080 peerlink-backend:latest
```

### Using Docker Compose (with frontend)

From the root directory:
```bash
docker-compose up
```

## Configuration

Key application properties can be configured via `application.properties` or environment variables:

```properties
server.port=8080
server.servlet.context-path=/api
file.upload.dir=/tmp/peer2peer
```

## Dependencies

Main dependencies (defined in `pom.xml`):
- Spring Framework - Web framework
- JUnit 5 - Testing framework
- Commons IO - File utilities
- Maven Plugins - Build and compilation tools

## Troubleshooting

### Port Already in Use
```bash
# Change port
mvn spring-boot:run -Dserver.port=8081
```

### Build Fails
```bash
# Clear Maven cache
mvn clean -U install
```

### Test Failures
```bash
# Run with verbose output
mvn test -X
```

## Contributing

1. Create a feature branch (`git checkout -b feature/amazing-feature`)
2. Commit changes (`git commit -m 'Add amazing feature'`)
3. Push to branch (`git push origin feature/amazing-feature`)
4. Open a Pull Request

## License

This project is licensed under the GPL 3.0 License - see LICENSE file for details.

## Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Contact: [sgr@duck.com]

## Related Projects

- **Frontend**: [peerlink-frontend](https://github.com/sgr9/peerlink-frontend)

---

**Version**: 1.0-SNAPSHOT  
**Java Version**: 17  
**Last Updated**: December 2025
