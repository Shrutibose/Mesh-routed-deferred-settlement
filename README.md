# UPI Mesh Routed Deferred Settlement

![Java](https://img.shields.io/badge/Java-17+-red)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![Architecture](https://img.shields.io/badge/Architecture-Distributed_System-blue)
![Status](https://img.shields.io/badge/Status-Demo_Project-orange)

---

## Overview

A Spring Boot-based distributed systems simulation that demonstrates **offline-first payment routing using a Bluetooth-style mesh network with deferred settlement**.

The system models a scenario where payments are created in a fully offline environment, propagated through a peer-to-peer device mesh, and eventually settled when a bridge node regains internet connectivity.

It demonstrates core distributed systems concepts including:

* Offline-first message propagation
* Hybrid cryptographic security (RSA + AES-GCM)
* Exactly-once processing semantics
* Idempotent transaction ingestion
* Eventual consistency in financial systems

---

## Key Features

### 1. Offline Payment Creation

Payments are generated as encrypted payloads using:

* AES-GCM for data confidentiality and integrity
* RSA-OAEP for secure key exchange

### 2. Mesh-Based Propagation

Transactions propagate through a simulated peer-to-peer network:

* Devices exchange packets via gossip protocol
* TTL-based hop control
* Fully untrusted intermediate nodes

### 3. Bridge-Based Settlement

A device with internet access acts as a bridge:

* Uploads collected packets to backend
* Triggers settlement pipeline

### 4. Exactly-Once Guarantee

Ensured via:

* SHA-256 ciphertext hashing
* Atomic `putIfAbsent` idempotency layer
* Database-level uniqueness constraints

### 5. Secure Transaction Pipeline

Backend validates:

* Cryptographic integrity (AES-GCM authentication)
* Freshness (timestamp validation)
* Replay protection
* Balance consistency with optimistic locking

---

## System Architecture

```
Sender Device (Offline)
        |
        |  Encrypt (AES-GCM + RSA-OAEP)
        v
Mesh Network (Peer-to-Peer Gossip Layer)
        |
        |  Multi-hop propagation (TTL-based)
        v
Bridge Device (Internet-enabled node)
        |
        |  HTTPS upload
        v
Spring Boot Backend
        |
        |-- Idempotency Layer (SHA-256 + CAS)
        |-- Crypto Decryption Layer
        |-- Validation Layer (freshness + integrity)
        |-- Settlement Engine (@Transactional)
        v
Ledger (H2 / Database)
```

---

## Technology Stack

* Java 17+
* Spring Boot 3.x
* Spring Web
* Spring Data JPA
* H2 Database (in-memory)
* Lombok
* Java Cryptography Architecture (JCA)

---

## Project Structure

```
src/main/java/com/meshrouteddeferredsettlement/upi/

├── controller
│   ├── ApiController
│   └── DashboardController

├── service
│   ├── DemoService
│   ├── MeshSimulatorService
│   ├── VirtualDevice
│   ├── SettlementService
│   ├── BridgeIngestionService
│   ├── IdempotencyService

├── crypto
│   ├── HybridCryptoService
│   ├── ServerKeyHolder

├── model
│   ├── Account
│   ├── Transaction
│   ├── MeshPacket
│   ├── PaymentInstruction
│   ├── AccountRepository
│   ├── TransactionRepository

├── config
│   ├── AppConfig
```

---

## API Endpoints

### System APIs

| Method | Endpoint            | Description             |
| ------ | ------------------- | ----------------------- |
| GET    | `/api/server-key`   | Fetch RSA public key    |
| GET    | `/api/accounts`     | List all accounts       |
| GET    | `/api/transactions` | Get latest transactions |

---

### Mesh Simulation APIs

| Method | Endpoint           | Description                      |
| ------ | ------------------ | -------------------------------- |
| POST   | `/api/demo/send`   | Create and inject payment packet |
| GET    | `/api/mesh/state`  | View device states               |
| POST   | `/api/mesh/gossip` | Run gossip propagation round     |
| POST   | `/api/mesh/flush`  | Simulate bridge upload           |
| POST   | `/api/mesh/reset`  | Reset simulation state           |

---

### Production Ingestion API

| Method | Endpoint             | Description                    |
| ------ | -------------------- | ------------------------------ |
| POST   | `/api/bridge/ingest` | Ingest packet from bridge node |

---

## Security Design

### Hybrid Encryption

* AES-256-GCM for payload encryption
* RSA-OAEP for AES key wrapping
* Ensures confidentiality and integrity

### Idempotency Protection

* SHA-256(ciphertext) used as transaction fingerprint
* ConcurrentHashMap-based atomic claim
* Prevents double spending under concurrency

### Replay Protection

* Timestamp validation (`signedAt`)
* Expiry window enforcement
* Nonce-based uniqueness inside payload

---

## Settlement Guarantees

The settlement engine ensures:

* Atomic debit and credit within a transaction boundary
* Optimistic locking using `@Version`
* Ledger immutability (transaction table is append-only)
* Exactly-once execution semantics

---

## Testing Strategy

Key test scenarios include:

* Encryption/Decryption correctness
* Ciphertext tamper rejection
* Concurrent duplicate ingestion (idempotency validation)
* Multi-threaded settlement consistency

Run tests:

```bash
mvn test
```

---

## What This Project Demonstrates

This system is designed to showcase:

* Distributed systems design thinking
* Real-world payment system architecture
* Cryptographic protocol implementation
* Fault tolerance and deduplication strategies
* Eventual consistency modeling

---

## Production Differences

| Component      | Demo              | Production            |
| -------------- | ----------------- | --------------------- |
| Database       | H2                | PostgreSQL / MySQL    |
| Idempotency    | ConcurrentHashMap | Redis (SET NX)        |
| Key Storage    | JVM memory        | HSM / KMS             |
| Mesh Layer     | Simulation        | BLE / WiFi Direct     |
| Authentication | None              | mTLS / Signed devices |

---

## Limitations

* Offline double-spend is theoretically possible until settlement
* Mesh reliability is simulated, not real-world BLE
* No real banking integration layer
* No device trust enforcement layer
* Simplified networking model for demonstration purposes

---

## Summary

This project simulates a **decentralized offline payment network with secure deferred settlement**, combining:

* Cryptography
* Distributed systems design
* Idempotent backend processing
* Mesh networking simulation

It demonstrates how financial transactions could be routed and safely processed in disconnected environments with eventual consistency guarantees.
