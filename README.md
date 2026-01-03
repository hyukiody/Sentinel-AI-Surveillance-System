# eyeO Platform 👁️

**Zero-Trust Microservices Security Platform**

Production-grade platform demonstrating Zero-Trust architecture, client-side AES-256-GCM encryption, JWT authentication, and tiered licensing. Built with Spring Boot 3.4, React 18, and Docker Compose.

---

## Core Features

- **Zero-Trust Security**: Client-side AES-256-GCM encryption, PBKDF2 key derivation
- **JWT Authentication**: HS512 signatures with custom license claims
- **3-Tier Licensing**: FREE/PRO/ENTERPRISE with quota enforcement
- **Microservices**: Independent services with Shared-Nothing Architecture
- **Modern Stack**: Spring Boot 3.4, React 18 + TypeScript, MySQL 8.0

---

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.4, Spring Security, JPA/Hibernate
- **Frontend**: React 18, TypeScript 5.6, Vite 5.2, Web Crypto API
- **Database**: MySQL 8.0
- **Infrastructure**: Docker Compose, Nginx
- **Security**: JWT (jjwt), AES-256-GCM, PBKDF2

---

## Quick Start

### Prerequisites
- Docker Desktop
- Node.js 18+
- JDK 17+

### Start Services
```bash
# Clone repository
git clone https://github.com/yourusername/eyeo-platform.git
cd eyeo-platform

# Start databases
docker-compose up -d mysql-identity mysql-stream

# Start backend (separate terminals)
cd identity-service && mvn spring-boot:run
cd data-core && mvn spring-boot:run
cd stream-processing && mvn spring-boot:run

# Start frontend
cd frontend && npm install && npm run dev
```

Access at `http://localhost:5173`

---

## Project Structure

```
eyeo-platform/
├── identity-service/     # JWT auth, license validation (8081)
├── data-core/           # Storage, encryption, quotas (8082)
├── stream-processing/   # Event processing (8083)
├── frontend/            # React dashboard (5173)
├── contracts/           # Shared interfaces
├── docs/               # Documentation
└── docker-compose.yml
```

---

## Documentation

- [Architecture](ARCHITECTURE.md) - System design and patterns
- [Security](SECURITY.md) - Security policies and best practices
- [Deployment](DEPLOYMENT.md) - Production deployment guide
- [Contributing](CONTRIBUTING.md) - Development workflow

---

## License

MIT License - See [LICENSE](LICENSE) file for details.

---

**Built with Spring Boot 3.4, React 18, and Docker Compose**

