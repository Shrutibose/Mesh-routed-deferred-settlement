
# UPI Mesh Routed Deferred Settlement - Demo

A Spring Boot backend that simulates **offline UPI-style payments routed through a Bluetooth mesh network with delayed settlement**.

You are in a network-dead zone - a basement with no internet. You send ₹500 to a friend. Your phone encrypts the payment, broadcasts it to nearby phones, and the packet hops device-to-device until one phone eventually walks outside, gets internet access, and uploads it to this backend.

The backend then:

* decrypts the packet
* validates freshness and integrity
* prevents duplicates
* and finally settles the transaction atomically

This project also includes a **software-based mesh simulator**, so the entire system can be demonstrated on a single laptop — no real Bluetooth hardware required.

---

## Table of Contents

* What this demo proves
* How to run it
* Demo flow (step-by-step)
* Architecture
* Core problems solved
* File-by-file breakdown
* API reference
* Tests
* What’s NOT real (production differences)
* Limitations

---

# What this demo proves

This system demonstrates three key ideas:

### 1. Secure offline transfer

Payments can move through **untrusted devices** without exposing sensitive data.

* AES-GCM encrypts payload (confidential + tamper-proof)
* RSA-OAEP protects AES key
* intermediaries cannot read or modify data

---

### 2. Exactly-once settlement

Even if multiple devices upload the same transaction:

> only one settlement is processed

This is guaranteed using:

* SHA-256 ciphertext hashing
* atomic `putIfAbsent` idempotency check

---

### 3. Replay + tamper protection

* Modified packets fail decryption (AES-GCM auth failure)
* Old packets are rejected (freshness window)
* Duplicate packets are ignored

---

# How to run it

## Prerequisites

* Java 17+
* That’s it.

---

## Run (Windows)

```bash
mvnw.cmd spring-boot:run
```

---

## Run (Mac/Linux)

```bash
./mvnw spring-boot:run
```

---

## Open dashboard

```
http://localhost:8080
```

You’ll see a live simulation dashboard.

---

## Run tests

```bash
mvnw.cmd test
```

Key test:

* `IdempotencyConcurrencyTest` → verifies exactly-once settlement under concurrency

---

# Demo Flow (Step-by-step)

## Inject Payment

You create a payment:

* sender
* receiver
* amount
* PIN

System:

* builds `PaymentInstruction`
* encrypts it using hybrid encryption
* wraps into `MeshPacket`
* injects into virtual device

---

## Mesh Gossip

Click gossip:

* devices exchange packets
* TTL decreases per hop
* packets spread across network

---

## Bridge Upload

When a device with internet appears:

* it uploads stored packets to backend
* triggers `/api/bridge/ingest`

Backend pipeline runs:

1. hash ciphertext
2. idempotency check
3. decrypt payload
4. freshness validation
5. settlement execution

---

## Settlement Result

You will see:

* account balances updated
* transaction recorded
* duplicates rejected safely

---

# Architecture

```
Sender Phone
   │
   ▼
Encrypt (AES-GCM + RSA-OAEP)
   │
Mesh Network (Bluetooth simulation)
   │
   ▼
Bridge Node (internet available)
   │
   ▼
Spring Boot Backend
   │
   ├── Idempotency Check (SHA-256)
   ├── Decryption (Hybrid Crypto)
   ├── Freshness Validation
   └── Settlement (DB transaction)
```

---

# Core Problems Solved

## 1. Untrusted devices

Solved using:

* Hybrid encryption (RSA + AES-GCM)

---

## 2. Duplicate uploads (critical problem)

Solved using:

* `SHA-256(ciphertext)`
* `ConcurrentHashMap.putIfAbsent`

ensures exactly one settlement

---

## 3. Replay attacks

Solved using:

* timestamp (`signedAt`)
* nonce inside payload
* freshness window (24h)

---

# File Structure

```
model/
  Account → bank account entity
  Transaction → immutable ledger entry
  MeshPacket → encrypted transport packet
  PaymentInstruction → decrypted payload

crypto/
  HybridCryptoService → AES + RSA encryption/decryption
  ServerKeyHolder → RSA keypair generator

service/
  MeshSimulatorService → virtual Bluetooth network
  VirtualDevice → simulated phone
  SettlementService → debit/credit logic
  BridgeIngestionService → main backend pipeline
  IdempotencyService → duplicate prevention
  DemoService → creates packets

controller/
  ApiController → REST APIs
  DashboardController → UI entry

config/
  AppConfig → scheduling + config setup
```

---

#  API Reference

| Method | Endpoint             | Description              |
| ------ | -------------------- | ------------------------ |
| GET    | `/api/server-key`    | Public RSA key           |
| POST   | `/api/demo/send`     | Create + inject packet   |
| GET    | `/api/mesh/state`    | Mesh status              |
| POST   | `/api/mesh/gossip`   | Run gossip round         |
| POST   | `/api/mesh/flush`    | Upload from bridge nodes |
| POST   | `/api/mesh/reset`    | Reset simulation         |
| POST   | `/api/bridge/ingest` | Production ingestion     |
| GET    | `/api/accounts`      | Account balances         |
| GET    | `/api/transactions`  | Transaction ledger       |

---

# Tests

* ✔ encryption round-trip test
* ✔ tamper detection test
* ✔ concurrency idempotency test (3 bridges → 1 settlement)

---

# What is NOT real (important)

| Demo              | Production           |
| ----------------- | -------------------- |
| In-memory DB      | PostgreSQL           |
| ConcurrentHashMap | Redis SETNX          |
| Generated RSA key | HSM / AWS KMS        |
| Simulated mesh    | Real BLE/WiFi Direct |
| No auth on ingest | mTLS + signed nodes  |
| Local JVM state   | distributed system   |

---

# Limitations

* Offline double-spend possible until settlement
* Bluetooth mesh reliability not modeled
* No real bank integration
* Device trust model is simplified

---
