# 🌐 UPI Mesh-Routed Deferred Settlement

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Architecture-Distributed_Systems-0052CC?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Security-AES--256--GCM_+_RSA--OAEP-DC143C?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Status-Demo_Project-F4A261?style=for-the-badge" />
</p>

> **A production-grade distributed systems simulation** that models offline-first UPI payment routing through a peer-to-peer mesh network, with cryptographically secured deferred settlement upon reconnection. Built to demonstrate real-world backend engineering: from gossip protocols to exactly-once transaction guarantees.

---

## 🧠 The Problem This Solves

Standard payment systems require constant internet connectivity. But what happens in rural India, during network outages, or in low-connectivity environments?

This project simulates a **resilient payment infrastructure** where:

- Users can **initiate payments offline**, with no internet access
- Transactions **propagate peer-to-peer** through nearby devices (like Bluetooth mesh)
- A **bridge node** uploads all collected transactions once it reconnects
- The backend settles all transactions **safely, atomically, and exactly once**

This mirrors real-world challenges faced by systems like **ONDC, RuPay offline payments, and CBDC edge-node architectures**.

---

## ✨ Key Engineering Highlights

| Concept | Implementation |
|---|---|
| **Offline-first design** | Payments created & encrypted without any server call |
| **Gossip protocol** | TTL-based multi-hop peer propagation across virtual devices |
| **Hybrid encryption** | AES-256-GCM (payload) + RSA-OAEP (key wrap) |
| **Exactly-once semantics** | SHA-256 ciphertext fingerprint + atomic `putIfAbsent` |
| **Optimistic locking** | JPA `@Version` prevents concurrent balance corruption |
| **Eventual consistency** | Deferred settlement engine with idempotency layer |
| **Replay protection** | Timestamp + nonce-based payload uniqueness |

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        OFFLINE ZONE                             │
│                                                                 │
│   [Sender Device]                                               │
│        │  Encrypt payload                                       │
│        │  AES-256-GCM  +  RSA-OAEP key wrap                    │
│        ▼                                                        │
│   [Mesh Network] ──── gossip ────► [Device A]                  │
│        │                               │                        │
│        └─────────── gossip ───────► [Device B]                 │
│                         TTL-controlled multi-hop                │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                    [Bridge Node regains internet]
                                 │ HTTPS Upload
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     SPRING BOOT BACKEND                         │
│                                                                 │
│   BridgeIngestionService                                        │
│        │                                                        │
│        ├──► IdempotencyService  (SHA-256 fingerprint + CAS)    │
│        ├──► HybridCryptoService (AES-GCM decrypt + verify)     │
│        ├──► ValidationLayer     (freshness + replay check)     │
│        └──► SettlementEngine    (@Transactional + @Version)    │
│                                 │                               │
│                                 ▼                               │
│                          H2 Ledger DB                           │
│                    (append-only transactions)                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Security Design

### Hybrid Encryption Pipeline

Every payment payload is encrypted **before it leaves the sender device**, using a two-layer scheme:

```
PaymentInstruction (JSON)
        │
        ▼
AES-256-GCM encryption
  → produces: [IV | Ciphertext | Auth Tag]
        │
AES key wrapped with RSA-OAEP (server public key)
        │
        ▼
MeshPacket { encryptedKey, iv, ciphertext, signedAt, nonce }
```

This ensures:
- **Confidentiality** — intermediate mesh nodes cannot read the payload
- **Integrity** — GCM authentication tag detects any tampering
- **Forward secrecy** — each payment uses a fresh AES key

### Idempotency & Replay Protection

```
Incoming MeshPacket
        │
        ▼
SHA-256(ciphertext) → idempotencyKey
        │
ConcurrentHashMap.putIfAbsent(key, "CLAIMED")
        │
   ┌────┴────┐
CLAIMED    NEW
   │          │
  Reject    Proceed to decryption & settlement
```

Prevents double-spend even under concurrent bridge uploads from multiple nodes.

---

## 📁 Project Structure

```
src/main/java/com/meshrouteddeferredsettlement/upi/
│
├── controller/
│   ├── ApiController.java          # REST endpoints for mesh simulation
│   └── DashboardController.java    # Thymeleaf UI controller
│
├── service/
│   ├── DemoService.java            # Orchestrates end-to-end demo flow
│   ├── MeshSimulatorService.java   # Manages virtual device topology
│   ├── VirtualDevice.java          # Individual node in the mesh
│   ├── SettlementService.java      # Atomic debit/credit engine
│   ├── BridgeIngestionService.java # Entry point for bridge uploads
│   └── IdempotencyService.java     # CAS-based deduplication
│
├── crypto/
│   ├── HybridCryptoService.java    # AES-GCM + RSA-OAEP encrypt/decrypt
│   └── ServerKeyHolder.java        # RSA keypair lifecycle management
│
├── model/
│   ├── Account.java                # JPA entity with @Version for locking
│   ├── Transaction.java            # Append-only ledger entry
│   ├── MeshPacket.java             # Encrypted payload container
│   ├── PaymentInstruction.java     # Decrypted payment intent
│   ├── AccountRepository.java
│   └── TransactionRepository.java
│
└── config/
    └── AppConfig.java              # Spring beans and crypto config
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Run locally

```bash
git clone https://github.com/Shrutibose/Mesh-routed-deferred-settlement.git
cd Mesh-routed-deferred-settlement
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

The H2 console is available at `http://localhost:8080/h2-console` for inspecting the in-memory ledger.

---

## 📡 API Reference

### System APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/server-key` | Fetch the RSA public key (used by sender devices to wrap AES keys) |
| `GET` | `/api/accounts` | List all accounts with current balances |
| `GET` | `/api/transactions` | Retrieve latest settled transactions |

### Mesh Simulation APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/demo/send` | Encrypt and inject a payment into the mesh |
| `GET` | `/api/mesh/state` | Inspect virtual device states and packet queues |
| `POST` | `/api/mesh/gossip` | Trigger one round of gossip propagation |
| `POST` | `/api/mesh/flush` | Simulate bridge upload → triggers settlement |
| `POST` | `/api/mesh/reset` | Reset simulation to initial state |

### Production Ingestion API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/bridge/ingest` | Ingest a raw `MeshPacket` from a real bridge node |

---

## 🧪 Testing

The test suite covers the most critical failure modes in a distributed payment system:

| Test Scenario | What It Validates |
|---|---|
| Encrypt → Decrypt round-trip | Cryptographic correctness |
| Tampered ciphertext rejection | GCM auth tag enforcement |
| Concurrent duplicate ingestion | Idempotency under race conditions |
| Multi-threaded settlement | Balance consistency with optimistic locking |

```bash
./mvnw test
```

---

## 🗺️ Production Roadmap

This project is intentionally scoped as a simulation. Here's how each component maps to a production-grade equivalent:

| Component | Current (Demo) | Production Equivalent |
|---|---|---|
| **Database** | H2 in-memory | PostgreSQL / MySQL with read replicas |
| **Idempotency store** | `ConcurrentHashMap` | Redis `SET NX` with TTL |
| **Key management** | JVM memory | HSM / AWS KMS / Google Cloud KMS |
| **Mesh transport** | Simulated gossip | Bluetooth LE / Wi-Fi Direct / NFC |
| **Device auth** | None | mTLS + signed device certificates |
| **Settlement locking** | JPA `@Version` | Distributed lock (Redisson / ZooKeeper) |
| **Monitoring** | None | Prometheus + Grafana + OpenTelemetry |

---

## 💡 Concepts Demonstrated

This project is a hands-on implementation of the following distributed systems principles:

- **Eventual consistency** — Ledger converges to correct state after all mesh packets are settled
- **Exactly-once delivery** — SHA-256 fingerprint + CAS prevents duplicate processing
- **Optimistic concurrency control** — `@Version` field prevents lost updates without pessimistic locking
- **Offline-first architecture** — Zero server dependency at payment creation time
- **Gossip / epidemic protocols** — Probabilistic message dissemination with TTL decay
- **Hybrid cryptography** — Combining asymmetric and symmetric algorithms for performance + security

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5, Spring Web, Spring Data JPA |
| Templating | Thymeleaf |
| Database | H2 (in-memory) |
| Cryptography | Java Cryptography Architecture (JCA) — built-in, no external libs |
| Build | Maven |
| Utilities | Lombok |
| Validation | Spring Validation (Jakarta Bean Validation) |

---

## ⚠️ Known Limitations

- **Offline double-spend window**: Two devices can both accept the same payment before either reaches the bridge. In production this is mitigated by reserved balance locking or CBDC-style offline spending caps.
- **Simulated mesh**: The gossip layer runs in-process. Real-world BLE / Wi-Fi Direct would add latency, packet loss, and device discovery complexity.
- **No device trust model**: Any device can act as a bridge. Production systems would require signed device attestation (e.g., Android Play Integrity / Secure Enclave).
- **No banking integration**: Settlement updates an in-memory ledger only. A real system would interface with NPCI / banking core via ISO 8583 or equivalent.

---

## 📄 License

This project is open-sourced for educational and portfolio purposes.

---

<p align="center">
  Built with ❤️ to explore the intersection of <strong>distributed systems</strong>, <strong>cryptography</strong>, and <strong>fintech infrastructure</strong>.
</p>
