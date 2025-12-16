# Sentinel AI Surveillance System

**Version:** 1.0.0-SNAPSHOT  
**Status:** Phase 2 Complete (Real-Time AI Pipeline Operational)

Sentinel is a converged security platform that combines **Spring Boot** (Backend), **JavaFX** (Frontend), and **Deep Java Library (DJL)** (AI) into a single, high-performance desktop application. It performs real-time object detection on video streams using the **YOLOv8** architecture running on the **ONNX Runtime** engine. It's currently an experimental project supposedly for personal project highlighting.

---

## 🚀 Features

### **1. Universal Video Ingestion**
* **Local File Playback:** Supports MP4, MKV, AVI via **VLCJ 4.8.3** (Java wrapper for VLC).
* **RTSP Streaming:** Low-latency playback for IP Cameras (`:network-caching=300`, TCP transport).
* **Hardware Acceleration:** Leverages native VLC libraries for decoding.

### **2. AI Intelligence Layer**
* **Model:** **YOLOv8 Nano (v8n)** running via ONNX Runtime.
* **Inference Engine:** Deep Java Library (DJL) 0.30.0.
* **Custom Translation:** Implements a specialized `YoloV8Translator` to handle the transposed `[1, 84, 8400]` tensor layout output by standard YOLO exports.
* **Performance:**
    * **Zero-Copy Rendering:** Uses `PixelBuffer` (JavaFX 13+) for efficient video memory sharing.
    * **Async Processing:** The `VideoProcessor` runs inference on a separate thread pool to prevent UI freezing.
    * **Non-Maximum Suppression (NMS):** Java-side filtering to deduplicate overlapping bounding boxes.

### **3. Convergent Architecture**
* **Spring Boot 3.4.0:** Manages dependency injection (`@Service`, `@Component`) and database transactions.
* **JavaFX 21:** Provides the GUI, integrated seamlessly into the Spring application lifecycle.
* **PostgreSQL:** Database persistence layer (configured via Docker).

---

## 🛠️ Prerequisites

1.  **Java JDK 21:** (Tested with Amazon Corretto 21 or OpenJDK 21).
2.  **Maven:** 3.8+.
3.  **VLC Media Player (64-bit):**
    * **Required:** The application relies on `libvlc.dll` / `libvlc.so`.
    * **Download:** [VideoLAN.org](https://www.videolan.org/vlc/) (Ensure you install the **x64** version).
4.  **Docker:** Required for the PostgreSQL database container.

---

## ⚙️ Setup & Installation

### **1. Database Setup**
Start the PostgreSQL container using the credentials defined in `application.yml`:

```bash
docker run --name sentinel-db -p 5432:5432 -e POSTGRES_PASSWORD=password -e POSTGRES_DB=sentinel_db -d postgres:15

commit:

development-note-report:
	17:42 16/12/2025

]JSONB Testing: The DetectionEventRepositoryTest uses a real PostgreSQL instance via Docker. This is the only reliable way to test PostgreSQL-specific JSON operators like ->>. Ensure Docker is running when executing tests.

AttributeEncryptor: I included a test for this component assuming it follows the standard javax.persistence.AttributeConverter interface. If you haven't implemented the class yet, this test defines the specification you need to build (encrypt string -> base64, decrypt base64 -> string). 