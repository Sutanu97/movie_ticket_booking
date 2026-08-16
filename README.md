# 🎬 MovieTicketBookingSystem (v1.0 - Pessimistic Locking & Distributed Architecture)

A production-grade, distributed movie ticket booking REST API built with **Java 21**, **Spring Boot 3.3**, **MySQL**, **Redis (Redisson)**, **Apache Kafka**, and **Razorpay**. 

Version 1.0 focuses on absolute correctness and zero double-booking guarantees under high concurrency by combining **Distributed Redisson Locks**, **JPA Pessimistic Write Locks (`SELECT ... FOR UPDATE`)**, **Redisson Delayed Queues** for automatic session timeouts, and **Kafka-driven Event Sourcing** for async payment refund compensations.

---

## 📌 Architecture & Design Highlights (v1.0)

```
                       ┌────────────────────────────────────────┐
                       │           Client / Frontend            │
                       └─────┬────────────────────────────▲─────┘
                             │ 1. POST /shows/book        │
                             │                            │ 4. POST /booking_confirmation
                             ▼                            │    (with Signature)
┌─────────────────────────────────────────────────────────┼────────────────────────────────────────┐
│ Spring Boot Application                                 │                                        │
│                                                         │                                        │
│  ┌────────────────────────────────────────┐             │                                        │
│  │ 1. Distributed Show Lock (Redisson)    │             │                                        │
│  │    lock:show:{showId}                  │             │                                        │
│  └──────────────────┬─────────────────────┘             │                                        │
│                     │                                   │                                        │
│  ┌──────────────────▼─────────────────────┐             │                                        │
│  │ 2. DB Pessimistic Lock                 │             │                                        │
│  │    SELECT ... FOR UPDATE (Seats Left)  │             │                                        │
│  └──────────────────┬─────────────────────┘             │                                        │
│                     │                                   │                                        │
│  ┌──────────────────▼─────────────────────┐             │                                        │
│  │ 3. Razorpay Order Creation             │             │                                        │
│  │    + Schedule Redisson Delayed Queue   │             │                                        │
│  │      (5 min Timeout Payload)           │             │                                        │
│  └──────────────────┬─────────────────────┘             │                                        │
│                     │                                   │                                        │
│                     ▼                                   │                                        │
│     ┌───────────────────────────────┐                   ▼                                        │
│     │ Redis RDelayedQueue           │  ┌──────────────────────────────────────────────────────┐  │
│     │ (ZSET delay timer)            │  │ 5. Server-Side HMAC Signature Verification (Razorpay)│  │
│     └───────────────┬───────────────┘  └────────────────────────┬─────────────────────────────┘  │
│                     │                                           │                                │
│                     │ (fires after 5 min)                       ├──────────────┬─────────────────┤
│                     ▼                                           │ (Success)    │ (Expired & Paid)│
│     ┌───────────────────────────────┐                           ▼              ▼                 │
│     │ BookingTimeoutService         │                 ┌──────────────┐ ┌───────────────────────┐ │
│     │ - Check status                │                 │ Confirm DB   │ │ Publish Kafka Event   │ │
│     │ - Mark FAILED                 │                 │ Booking &    │ │ Topic: booking.refund │ │
│     │ - Reallocate Seats in DB      │                 │ Send Email   │ └───────────┬───────────┘ │
│     └───────────────────────────────┘                 └──────────────┘             │             │
└────────────────────────────────────────────────────────────────────────────────────┼─────────────┘
                                                                                     │
                                                                                     ▼
                                                                        ┌──────────────────────────┐
                                                                        │ RefundConsumer           │
                                                                        │ (Async Razorpay Refund)  │
                                                                        └──────────────────────────┘
```

---

## 🚀 Key Features

### 1. Concurrency & Race-Condition Prevention
- **Show-Level Distributed Lock (`RedissonClient`):** Prevents concurrent threads from stepping on seat reservation calculations for a given show.
- **Database Pessimistic Locking (`LockModeType.PESSIMISTIC_WRITE`):** Queries `SeatAvailability` with `SELECT ... FOR UPDATE` to serialize database-level mutations to the remaining seat string.
- **Order-Level Lock (`lock:order:{orderId}`):** Synchronizes confirmation requests against the timeout worker thread to eliminate race conditions between payment confirmations and session expirations.

### 2. Fault-Tolerant Session Timeout (Redisson Delayed Queue)
- When a user initiates a booking, a 5-minute delayed payload (`orderId|showId|seats`) is scheduled via `RDelayedQueue`.
- A dedicated background worker (`BookingTimeoutService`) listens on the `RBlockingQueue`.
- If the payment is not completed within 5 minutes, the worker marks the `RazorpayOrder` as `FAILED` and restores the seats back to `SeatAvailability` in the database.

### 3. Server-Side Payment Verification (Anti-Tamper)
- Frontend client returns `razorpay_order_id`, `razorpay_payment_id`, and `razorpay_signature`.
- The backend verifies the HMAC-SHA256 signature using the secret key via `com.razorpay.Utils.verifyPaymentSignature`.
- Rejects forged callbacks and guarantees that only genuinely captured payments are confirmed.

### 4. Event-Driven Compensation via Kafka (Refund Saga)
- **Problem Handled:** If a user completes payment at Razorpay right when the 5-minute timeout occurs, the session is already marked `FAILED` and seats reallocated.
- **Solution:** Instead of failing silently with money captured, `/booking_confirmation` detects the valid signature on an expired order and immediately publishes a `RefundEvent` to Kafka topic `booking.refund`.
- **Consumer:** `RefundConsumer` consumes the event and triggers an automated refund via Razorpay's Refund API.

### 5. Security & Access Control
- **JWT (JSON Web Token) Stateless Authentication:** Tokens signed with HMAC-SHA algorithm.
- **Role-Based Access Control (RBAC):** `ROLE_ADMIN` and `ROLE_USER` authorities protecting administrative and user-specific endpoints with Spring Security `@PreAuthorize`.
- **BCrypt Password Hashing:** Secure password encoding.

### 6. Email Notifications
- Async HTML confirmation emails sent to users upon successful booking with show time, cinema location, seats, and order ID via Jakarta Mail.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.3.0 |
| **Security** | Spring Security 6, JJWT 0.12.6, BCrypt |
| **Database & ORM** | MySQL 8.x, Hibernate / JPA, HikariCP |
| **Distributed Cache & Locking** | Redis (Redisson 3.31.0) |
| **Messaging & Events** | Apache Kafka (Spring Kafka) |
| **Payment Gateway** | Razorpay Java SDK 1.4.6 |
| **Email Service** | Jakarta Mail API / Angus Mail |
| **Build Tool** | Maven |

---

## 📊 Database Schema & Domain Model

- **`User`**: Credentials, contact info, and roles (`ROLE_USER`, `ROLE_ADMIN`).
- **`Movie`**: Movie details (title, genre, language, duration, release date).
- **`Multiplex`**: Theatre details (name, city, address).
- **`Screen`**: Screen definition within a multiplex with seating layout.
- **`Show`**: Mapping between Movie, Multiplex, Screen, and start/end time.
- **`SeatAvailability`**: Real-time remaining seats for a given `Show` (locked pessimistically).
- **`RazorpayOrder`**: Order tracking (`Created`, `Success`, `Failed`, `Refunded`).
- **`BookingHistory`**: Immutable finalized booking receipts for users.

---

## 🔌 API Endpoints Summary

### 🔐 Authentication & Users
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/users/new` | Public | Register new user / admin |
| `POST` | `/users/login` | Public | Authenticate and obtain JWT token |
| `GET` | `/users` | `ADMIN` | List all users |
| `GET` | `/users/{username}` | `USER` (Self) / `ADMIN` | Fetch user details |
| `DELETE` | `/users/{username}` | `ADMIN` | Delete a user |

### 🎥 Movies & Shows
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/movies?city={city}` | Authenticated | List all movies in a city |
| `GET` | `/movies?city={city}&lowerBound={dt}&upperBound={dt}` | Authenticated | Search movies by city and time window |
| `GET` | `/shows?city={city}&movieName={name}` | Authenticated | Get all shows for a movie grouped by date |
| `GET` | `/shows/{showId}/seats` | Authenticated | Get available seat list for a specific show |

### 🎟️ Booking & Payment
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/shows/book` | Authenticated | Reserve seats (Pessimistic lock), create Razorpay order, schedule timeout |
| `POST` | `/booking_confirmation` | Authenticated | Verify signature, finalize booking OR trigger Kafka refund if expired |
| `GET` | `/shows/user/{username}` | `USER` (Self) / `ADMIN` | View user's booking history |

---

## ⚙️ Configuration & Setup

### 1. Prerequisites
- **JDK 21**
- **MySQL 8.x** running on port `3307` (or update `application.properties`)
- **Redis Server** running on port `6380` (or update `application.properties`)
- **Apache Kafka** running on port `9092`
- **Razorpay Test Account** (Key ID & Secret)

### 2. Environment Variables
Export the following environment variables before launching the application:

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_db_password
export JWT_SECRET_KEY=your_base64_encoded_256_bit_jwt_secret_key
export RAZORPAY_KEY_ID=rzp_test_your_key_id
export RAZORPAY_KEY_SECRET=your_razorpay_secret
export MAIL_CONFIG_FILE_PATH=/path/to/mailConfigs.properties
```

### 3. Kafka Topic Setup
Create the refund topic:
```bash
kafka-topics.sh --create --topic booking.refund --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### 4. Build and Run
```bash
# Clone the repository
git clone https://github.com/Sutanu97/MovieTicketBookingSystem.git
cd MovieTicketBookingSystem

# Compile and package
mvn clean install

# Run the Spring Boot application
mvn spring-boot:run
```

---

## 🔄 Concurrency Handling & Edge Cases Handled in v1.0

```
Scenario 1: Concurrent Booking for Same Show & Seats
Thread 1 (A1, A2) ───► Acquired Distributed Lock + DB Pessimistic Lock ───► Reserved in DB ───► Razorpay Order Created
Thread 2 (A1, A3) ───► Waits for Lock ───► Reads updated DB ──► Detects A1 already taken ──► Returns "Seats already taken"

Scenario 2: User Abandons Payment (Timeout)
T+0   : /shows/book reserves seats in DB, pushes to Redisson Delayed Queue (5m)
T+5m  : BookingTimeoutService pops from queue, checks order status ("Created"), marks FAILED, reallocates seats in DB.

Scenario 3: Late Payment (User pays at T+5m01s)
T+5m00s : Timeout marks order FAILED and reallocates seats.
T+5m01s : User completes Razorpay payment. Frontend calls /booking_confirmation.
Backend : Signature verified (Valid payment) ──► Detects order status != Created ──► Publishes RefundEvent to Kafka ──► Razorpay refund processed asynchronously.
```

---

## 🔮 Upcoming Architectural Evolution (Version 2.0)

While Version 1.0 guarantees consistency via **Pessimistic Locking + Distributed Locks**, it holds locks at the show level and performs early DB writes:
- **Show-Level Lock Contention:** High booking traffic for distinct seats on the same show is serialized.
- **Database I/O Overhead:** Immediate DB reservation and subsequent DB rollback on timeout.

**Version 2.0 Roadmap:**
- ⚡ **Seat-Level Redis SET NX PX + Lua Scripting:** Migrate seat holds entirely to atomic in-memory keys (`hold:seat:{showId}:{seatId}`) with native Redis TTLs.
- ⚡ **Deferred Database Writes:** Only write to MySQL once payment is confirmed, boosting throughput and removing the background timeout worker.
