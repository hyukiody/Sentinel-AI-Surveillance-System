# eyeOSurveillance system

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=java&logoColor=white" alt="JavaFX"/>
  <img src="https://img.shields.io/badge/AI-YOLOv8-purple?style=for-the-badge&logo=pytorch&logoColor=white" alt="YOLOv8"/>
  <img src="https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
</p>

<p align="center">
  <strong>Enterprise-Grade Real-Time AI Video Analytics Platform</strong><br/>
  Converging Computer Vision, Event-Driven Architecture, and Desktop-Native Performance
</p>

---

## 🎯 Executive Summary

**Sentinel** is a production-ready AI surveillance platform that demonstrates enterprise software engineering at scale. It integrates real-time video processing, deep learning inference (YOLOv8), event-driven alerting, and a responsive desktop GUI—all in a single, deployable Java application.

| Metric | Achievement |
|--------|-------------|
| **Video Rendering** | 60 FPS (sub-frame latency) |
| **AI Inference** | 2 FPS (throttled for efficiency) |
| **Audit Latency** | <10ms per event |
| **Test Coverage** | 42+ automated tests |
| **Architecture** | Fail-safe, thread-safe, production-ready |

> 📋 **For detailed portfolio information and technical deep-dive, see [PORTFOLIO.md](PORTFOLIO.md)**

---

## 🚀 Key Features

### **1. Universal Video Ingestion**
* **Local File Playback:** Supports MP4, MKV, AVI via **VLCJ 4.8.3** (Java wrapper for VLC)
* **RTSP Streaming:** Low-latency playback for IP Cameras (`:network-caching=300`, TCP transport)
* **Hardware Acceleration:** Leverages native VLC libraries for efficient decoding

### **2. AI Intelligence Layer**
* **Model:** **YOLOv8 Nano (v8n)** running via ONNX Runtime
* **Inference Engine:** Deep Java Library (DJL) 0.30.0
* **Custom Translation:** Implements a specialized `YoloV8Translator` to handle the transposed `[1, 84, 8400]` tensor layout
* **Performance Optimizations:**
    * **Zero-Copy Rendering:** Uses `PixelBuffer` (JavaFX 13+) for efficient video memory sharing
    * **Async Processing:** `VideoProcessor` runs inference on a separate thread pool
    * **Non-Maximum Suppression (NMS):** Java-side filtering to deduplicate overlapping bounding boxes

### **3. Convergent Architecture**
* **Spring Boot 3.4.0:** Manages dependency injection (`@Service`, `@Component`) and database transactions
* **JavaFX 21:** Provides the GUI, integrated seamlessly into the Spring application lifecycle
* **PostgreSQL:** JSONB-enabled persistence layer with encrypted storage

### **4. Security & Compliance**
* **AES-256 Encryption:** Sensitive data encryption at rest
* **Immutable Audit Trail:** Complete security event logging
* **Geofence Zones:** Configurable detection regions with severity levels

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SENTINEL AI PLATFORM                               │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │   VIDEO     │───▶│   AI/ML     │───▶│   ALERT     │───▶│  DASHBOARD  │  │
│  │  INGESTION  │    │  PIPELINE   │    │   ENGINE    │    │    VIEW     │  │
│  │  (VLCJ)     │    │  (YOLOv8)   │    │ (Event-Pub) │    │  (JavaFX)   │  │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘  │
│         │                  │                  │                  │         │
│         └──────────────────┴──────────────────┴──────────────────┘         │
│                                    │                                        │
│                          ┌─────────▼─────────┐                              │
│                          │   PERSISTENCE     │                              │
│                          │  (PostgreSQL +    │                              │
│                          │   Spring Data)    │                              │
│                          └───────────────────┘                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| **Java JDK** | 21+ | Amazon Corretto 21 or OpenJDK 21 recommended |
| **Maven** | 3.8+ | Build automation |
| **VLC Media Player** | 3.0+ (64-bit) | Required for video decoding (`libvlc.dll`/`libvlc.so`) |
| **Docker** | 20.10+ | PostgreSQL container |
| **PostgreSQL** | 15+ | Database (via Docker or standalone) |

---

## ⚙️ Quick Start

### **1. Database Setup**
Start the PostgreSQL container:

```bash
docker run --name sentinel-db -p 5432:5432 -e POSTGRES_PASSWORD=password -e POSTGRES_DB=sentinel_db -d postgres:15
```

### **2. Build the Application**
```bash
mvn clean install
```

### **3. Run**
```bash
java -jar target/sentinel-surveillance-1.0.0-SNAPSHOT.jar
```

Or with Maven:
```bash
mvn spring-boot:run
```

### **4. Expected Startup Log**
```
🧠 Initializing AI Engine (ONNX)...
✅ YOLOv8 AI Core Ready!
📽️  Video Player Initialized
🚀 Sentinel Application Started on http://localhost:8080
```

---

## 📊 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/alerts/unacknowledged` | GET | Retrieve unacknowledged alerts (paginated: `?page=0&size=20`) |
| `/api/v1/alerts/zone/{zoneId}` | GET | Alerts filtered by geofence zone |
| `/api/v1/alerts/severity/{level}` | GET | Alerts by severity (CRITICAL, HIGH, MEDIUM, LOW) |
| `/api/v1/alerts/{alertId}/acknowledge` | POST | Acknowledge alert with operator username |
| `/api/v1/alerts/analytics/heatmap` | GET | Spatial detection heatmap |
| `/api/v1/alerts/analytics/dwell-time` | GET | Zone dwell time analytics |

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AlertEngineTest

# Generate test report
mvn clean test site:site
# View: target/site/surefire-report.html
```

**Test Suite Summary:**
- ✅ 42+ automated tests
- ✅ Unit tests (Mockito-based)
- ✅ Integration tests (Testcontainers + PostgreSQL)
- ✅ End-to-end pipeline validation

---

## 📁 Project Structure

```
sentinel-surveillance/
├── src/main/java/com/enterprise/sentinel/
│   ├── SentinelApplication.java          # Entry point (Spring Boot + JavaFX)
│   ├── api/                               # REST controllers
│   │   └── AlertController.java
│   ├── client/                            # Desktop UI components
│   │   ├── ui/                           # JavaFX views
│   │   └── video/                        # Video rendering
│   ├── config/                           # Configuration
│   ├── domain/
│   │   ├── model/                        # JPA entities
│   │   └── repository/                   # Spring Data repositories
│   └── service/
│       ├── analysis/                     # AI, alerting, analytics
│       ├── ingestion/                    # Video file & RTSP handling
│       └── security/                     # Encryption, audit logging
├── src/test/java/                        # Test suite
├── models/                               # AI models (YOLOv8 ONNX)
├── pom.xml                               # Maven configuration
└── application.yml                       # Spring configuration
```

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [PORTFOLIO.md](PORTFOLIO.md) | Technical portfolio for recruiters |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | Complete deployment instructions |
| [PHASE_2_DESIGN_SCHEME.md](PHASE_2_DESIGN_SCHEME.md) | Architecture & design patterns |
| [TEST_REPORT.md](TEST_REPORT.md) | Test suite documentation |

---

## 🔐 Security Features

- **Data Encryption:** AES-256 for sensitive fields via `AttributeEncryptor`
- **Audit Logging:** Immutable audit trail with `@Transactional` guarantees
- **Authentication Ready:** Spring Security integration configured
- **Input Validation:** Comprehensive bounds checking and sanitization

---

## 📈 Performance Targets

| Metric | Target | Achieved |
|--------|--------|----------|
| Video FPS | 60 fps | ✅ |
| AI Inference | 2 fps | ✅ (500ms throttle) |
| UI Latency | <16ms | ✅ |
| Audit Write | <10ms | ✅ |
| Memory Base | ~512MB | ✅ |

---

## 🛡️ Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Runtime** | Java 21 (LTS) | Platform foundation |
| **Framework** | Spring Boot 3.4 | Dependency injection, web, data |
| **AI/ML** | DJL 0.30.0 + ONNX Runtime | YOLOv8 inference |
| **Desktop UI** | JavaFX 21 | Native GUI rendering |
| **Video** | VLCJ 4.8.3 | Media playback & RTSP |
| **Database** | PostgreSQL 15 | JSONB persistence |
| **Testing** | JUnit 5, Mockito, Testcontainers | Quality assurance |
| **Build** | Maven 3.8+ | Project management |

---

## 📄 License

This project is available for commercial licensing. Contact the author for inquiries.

---

## 👤 Author

Built with precision engineering to demonstrate enterprise Java development, AI integration, and desktop application architecture.

---

<p align="center">
  <strong>⭐ Star this repository if you find it useful!</strong>
</p>
