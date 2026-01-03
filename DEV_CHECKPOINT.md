# Development Checkpoint - Containerized Services

**Date**: January 3, 2026  
**Status**: ✅ Ready for Testing  
**Version**: v1.0-dev

## Overview

This checkpoint provides fully containerized services ready for development and testing. All services are packaged as Docker containers with a simplified orchestration setup.

## 🚀 Quick Start

### Prerequisites

- Docker Desktop installed and running
- Docker Compose v2.0+
- 8GB RAM minimum (16GB recommended)
- Ports available: 3306, 3307, 8081, 8082, 9090, 5173

### Start All Services

```powershell
# Copy development environment variables
Copy-Item .env.dev .env

# Start all services (builds if needed)
docker-compose -f docker-compose.dev.yml up -d --build

# Check status
docker-compose -f docker-compose.dev.yml ps

# View logs
docker-compose -f docker-compose.dev.yml logs -f
```

### Start Individual Services

```powershell
# Start only databases
docker-compose -f docker-compose.dev.yml up -d identity-db stream-db

# Start backend services
docker-compose -f docker-compose.dev.yml up -d identity-service stream-processing data-core

# Start frontend
docker-compose -f docker-compose.dev.yml up -d frontend
```

## 📦 Containerized Services

### Identity Service
- **Image**: `eyeo/identity-service:dev`
- **Port**: 8081
- **Health**: http://localhost:8081/actuator/health
- **Functions**: User authentication, JWT token management
- **Database**: MySQL (identity-db)

### Stream Processing Service
- **Image**: `eyeo/stream-processing:dev`
- **Port**: 8082
- **Functions**: Data transformation, stream handling
- **Database**: MySQL (stream-db)

### Data Core Service
- **Image**: `eyeo/data-core:dev`
- **Port**: 9090
- **Health**: http://localhost:9090/health
- **Functions**: Protected storage, file management
- **Storage**: Docker volume (protected-storage)

### Frontend Application
- **Image**: `eyeo/frontend:dev`
- **Port**: 5173 (mapped to 80 in container)
- **Access**: http://localhost:5173
- **Technology**: React + Vite

### Databases
- **Identity DB**: MySQL 8.0 on port 3306
- **Stream DB**: MySQL 8.0 on port 3307

## 🧪 Testing the Services

### Health Checks

```powershell
# Identity Service
curl http://localhost:8081/actuator/health

# Data Core
curl http://localhost:9090/health

# Frontend
curl http://localhost:5173
```

### API Testing

```powershell
# Register a user
curl -X POST http://localhost:8081/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"username":"testuser","password":"Test123!","email":"test@example.com"}'

# Login
curl -X POST http://localhost:8081/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"testuser","password":"Test123!"}'

# Test stream processing
curl -X POST http://localhost:8082/api/stream/process `
  -H "Authorization: Bearer YOUR_JWT_TOKEN" `
  -H "Content-Type: application/json" `
  -d '{"data":"test data"}'
```

### Database Access

```powershell
# Connect to identity database
docker exec -it eyeo-identity-db mysql -u identity_user -pDevIdentity2024 identity_db

# Connect to stream database
docker exec -it eyeo-stream-db mysql -u stream_user -pDevStream2024 stream_db
```

## 📊 Service Architecture

```
┌─────────────────┐
│    Frontend     │ :5173
│   (React/Vite)  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│          Backend Services                │
├─────────────┬─────────────┬─────────────┤
│  Identity   │   Stream    │  Data Core  │
│  Service    │ Processing  │   Service   │
│   :8081     │    :8082    │    :9090    │
└──────┬──────┴──────┬──────┴──────┬──────┘
       │             │              │
       ▼             ▼              │
┌─────────────┬─────────────┐      │
│ Identity DB │  Stream DB  │      │
│   :3306     │    :3307    │      │
└─────────────┴─────────────┘      │
                                   ▼
                         ┌──────────────────┐
                         │ Protected Storage│
                         │  (Docker Volume) │
                         └──────────────────┘
```

## 🛠️ Development Workflow

### Build Individual Service

```powershell
# Build specific service
docker-compose -f docker-compose.dev.yml build identity-service

# Build without cache
docker-compose -f docker-compose.dev.yml build --no-cache data-core
```

### Restart Service After Code Changes

```powershell
# Rebuild and restart
docker-compose -f docker-compose.dev.yml up -d --build identity-service

# View logs
docker-compose -f docker-compose.dev.yml logs -f identity-service
```

### Clean Restart

```powershell
# Stop all services
docker-compose -f docker-compose.dev.yml down

# Stop and remove volumes (CAUTION: Deletes all data)
docker-compose -f docker-compose.dev.yml down -v

# Start fresh
docker-compose -f docker-compose.dev.yml up -d --build
```

## 📝 Environment Variables

All configuration is in `.env.dev` (copied to `.env`):

- `IDENTITY_DB_PASSWORD`: Identity database password
- `STREAM_DB_PASSWORD`: Stream database password
- `JWT_SECRET_KEY`: JWT signing key (dev only)
- `EYEO_MASTER_KEY`: Master encryption key (dev only)

**⚠️ WARNING**: These are development credentials. Never use in production!

## 🐛 Troubleshooting

### Service Won't Start

```powershell
# Check logs
docker-compose -f docker-compose.dev.yml logs service-name

# Check health
docker inspect eyeo-service-name --format='{{.State.Health.Status}}'
```

### Database Connection Issues

```powershell
# Verify database is running
docker-compose -f docker-compose.dev.yml ps identity-db

# Check database logs
docker-compose -f docker-compose.dev.yml logs identity-db

# Wait for healthy status
docker-compose -f docker-compose.dev.yml up -d identity-db
Start-Sleep 30
```

### Port Conflicts

If ports are in use:
1. Stop conflicting services
2. Or modify ports in `docker-compose.dev.yml`
3. Update frontend API URLs accordingly

### Build Failures

```powershell
# Clean Docker cache
docker builder prune -f

# Rebuild from scratch
docker-compose -f docker-compose.dev.yml build --no-cache
```

## 📦 Exporting Containers

### Save Images for Distribution

```powershell
# Save all images
docker save -o eyeo-platform-dev.tar `
  eyeo/identity-service:dev `
  eyeo/stream-processing:dev `
  eyeo/data-core:dev `
  eyeo/frontend:dev

# Load on another machine
docker load -i eyeo-platform-dev.tar
```

### Push to Registry (Optional)

```powershell
# Tag for registry
docker tag eyeo/identity-service:dev myregistry/eyeo-identity:dev

# Push
docker push myregistry/eyeo-identity:dev
```

## ✅ Validation Checklist

- [ ] All containers build successfully
- [ ] Databases start and become healthy
- [ ] Backend services start and pass health checks
- [ ] Frontend serves on http://localhost:5173
- [ ] Can register a new user
- [ ] Can login and receive JWT token
- [ ] Can call protected endpoints with token
- [ ] Services can communicate with each other

## 📚 Next Steps

1. **Load Testing**: Use provided test scripts
2. **API Documentation**: Review OpenAPI specs at service endpoints
3. **Security Review**: Review SECURITY.md before production
4. **Deployment**: See DEPLOYMENT_READINESS.md for production deployment

## 🔗 Related Documentation

- [README.md](README.md) - Main project documentation
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture
- [SECURITY.md](SECURITY.md) - Security guidelines
- [API_TESTING_GUIDE.md](API_TESTING_GUIDE.md) - API testing examples

---

**Checkpoint Created**: January 3, 2026  
**Purpose**: Enable containerized testing and development  
**Status**: ✅ Production-ready containers with development configuration
