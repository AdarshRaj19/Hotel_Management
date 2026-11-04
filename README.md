# HotelEase — Hotel Booking & Management System

> **HCL Java Training | Team 8 Assignment**  
> Spring Boot 4 · Java 19 · MySQL · Thymeleaf · Bootstrap 5.3

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Quick Start](#quick-start)
4. [Default Accounts](#default-accounts)
5. [Database Schema](#database-schema)
6. [API Reference](#api-reference)
7. [Custom DSA Implementations](#custom-dsa-implementations)
8. [Running Tests](#running-tests)
9. [Project Structure](#project-structure)
10. [Team Members](#team-members)

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────┐
│                        Browser / Client                     │
└──────────────┬──────────────────────┬───────────────────────┘
               │  Thymeleaf UI        │  REST JSON
               ▼                     ▼
┌──────────────────────┐  ┌─────────────────────────┐
│  Web Controllers      │  │  API Controllers         │
│  (MVC, @Controller)  │  │  (@RestController)       │
└──────────┬───────────┘  └──────────┬───────────────┘
           │                         │
           ▼                         ▼
┌─────────────────────────────────────────────────────┐
│                    Service Layer                      │
│  BookingService · PaymentService · RoomService       │
│  CheckInOutService · ReportService · UserService     │
│  CheckInQueueService (CustomCircularQueue)           │
│  RoomCacheService    (CustomHashTable)               │
└──────────────────────┬──────────────────────────────┘
                       │
           ┌───────────┴───────────┐
           ▼                       ▼
┌──────────────────┐   ┌────────────────────────┐
│  Spring Data JPA │   │  Custom DSA Layer        │
│  Repositories    │   │  CircularQueue + HashMap │
└────────┬─────────┘   └────────────────────────┘
         │
         ▼
┌────────────────────┐
│   MySQL Database   │
│  hotel_booking_db  │
└────────────────────┘
```

**Spring Security** guards every route with role-based access:
- `ROLE_CUSTOMER` → `/customer/**`, `/booking/**`, `/rooms/**`
- `ROLE_RECEPTION` → `/reception/**` + all customer routes
- `ROLE_ADMIN` → `/admin/**` + all routes

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.9 |
| Language | Java 19 |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| View Layer | Thymeleaf 3 + Bootstrap 5.3.3 |
| Security | Spring Security 6 |
| Testing | JUnit 5 + Mockito |
| Build | Maven 3 |
| Dev Reload | Spring Boot DevTools |

---

## Quick Start

### Prerequisites
- Java 19+
- Maven 3.9+
- MySQL 8 running on port 3306

### 1 — Create Database

```sql
CREATE DATABASE hotel_booking_db;
```

### 2 — Configure Credentials

Edit [`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hotel_booking_db
spring.datasource.username=root
spring.datasource.password=MySQL#2026NewPass!
spring.jpa.hibernate.ddl-auto=update
server.port=8080
```

### 3 — Run the Application

```bash
./mvnw spring-boot:run
```

The app starts at **http://localhost:8080**

Tables and seed data (rooms, room types, test users) are created automatically on first launch via `DataInitializer`.

---

## Default Accounts

| Role | Email | Password | Dashboard |
|---|---|---|---|
| Admin | admin@hotelease.com | admin123 | `/admin/dashboard` |
| Reception | reception@hotelease.com | reception123 | `/reception/dashboard` |
| Customer | customer@hotelease.com | customer123 | `/customer/dashboard` |

> Passwords are BCrypt-hashed in the database. `DataInitializer` upserts these accounts on every startup.

---

## Database Schema

```
┌──────────┐       ┌──────────┐       ┌──────────────┐
│  users   │       │  rooms   │       │  room_types  │
├──────────┤       ├──────────┤       ├──────────────┤
│ id (PK)  │       │ id (PK)  │       │ id (PK)      │
│ full_name│       │ room_num │       │ name         │
│ email    │       │ room_type│◄──────│ description  │
│ password │       │ price    │       │ default_price│
│ phone    │       │ capacity │       └──────────────┘
│ role     │       │ status   │
└────┬─────┘       └────┬─────┘
     │                  │
     │   ┌──────────────┴──────────────┐
     │   │         bookings            │
     │   ├─────────────────────────────┤
     └──►│ id (PK)                     │
         │ user_id (FK)                │
         │ room_id (FK)                │
         │ check_in_date               │
         │ check_out_date              │
         │ num_guests                  │
         │ total_amount                │
         │ booking_status              │
         └──────────┬──────────────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
┌────────┴───────┐   ┌─────────┴──────────┐
│    payments    │   │   stay_records      │
├────────────────┤   ├────────────────────┤
│ id (PK)        │   │ id (PK)            │
│ booking_id(FK) │   │ booking_id (FK)    │
│ amount         │   │ actual_check_in    │
│ gst_amount     │   │ actual_check_out   │
│ total_amount   │   │ checked_in_by      │
│ payment_method │   │ checked_out_by     │
│ payment_status │   └────────────────────┘
│ transaction_ref│
└────────────────┘
```

**BookingStatus enum:** `PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT | CANCELLED`  
**RoomStatus enum:** `AVAILABLE | OCCUPIED | MAINTENANCE`  
**PaymentStatus enum:** `PENDING | PAID | REFUNDED`

---

## API Reference

### Rooms

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/rooms` | Any | List all rooms |
| `GET` | `/api/rooms/{id}` | Any | Get room by ID |
| `GET` | `/api/rooms/available` | Any | List available rooms |
| `GET` | `/api/rooms/lookup/{number}` | Any | O(1) hash-table lookup by room number |
| `POST` | `/api/rooms` | ADMIN | Create room |
| `PUT` | `/api/rooms/{id}` | ADMIN | Update room |
| `DELETE` | `/api/rooms/{id}` | ADMIN | Delete room |

### Bookings

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/bookings` | ADMIN/RECEPTION | List all bookings |
| `GET` | `/api/bookings/{id}` | Any | Get booking by ID |
| `POST` | `/api/bookings/{id}/check-in` | RECEPTION | Check in guest |
| `POST` | `/api/bookings/{id}/check-out` | RECEPTION | Check out guest |

### Payments

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/payments/bill/{bookingId}` | Any | Get bill with GST |
| `POST` | `/api/payments` | RECEPTION | Process payment |
| `GET` | `/api/payments/{id}` | Any | Get payment details |

### Reports

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/reports/occupancy` | ADMIN | Occupancy by room type |
| `GET` | `/api/reports/revenue` | ADMIN | Revenue grouped by month |

### Queue (DSA Demo)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/queue` | RECEPTION | View circular queue contents |
| `POST` | `/api/queue/{bookingId}` | RECEPTION | Enqueue booking |
| `POST` | `/api/queue/serve` | RECEPTION | Dequeue + auto check-in |

### Error Responses (JSON)

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Room not found with id: 999",
  "timestamp": "2026-11-04T16:30:00"
}
```

---

## Custom DSA Implementations

> All data structures are written from scratch — no Java Collections (`ArrayList`, `HashMap`, etc.) used internally.

### CustomCircularQueue\<T\>
📄 [`src/main/java/com/hotel/booking/ds/CustomCircularQueue.java`](src/main/java/com/hotel/booking/ds/CustomCircularQueue.java)

| Operation | Complexity |
|---|---|
| `enqueue(T)` | O(1) |
| `dequeue()` | O(1) |
| `peek()` | O(1) |
| `size()` | O(1) |

**Used by:** `CheckInQueueService` — reception waiting queue (capacity 25 bookings).  
Wraps around using modulo (`head % capacity`, `tail % capacity`). Throws on overflow/underflow.

### CustomHashTable\<K, V\>
📄 [`src/main/java/com/hotel/booking/ds/CustomHashTable.java`](src/main/java/com/hotel/booking/ds/CustomHashTable.java)

| Operation | Average | Worst |
|---|---|---|
| `put(K, V)` | O(1) | O(n) |
| `get(K)` | O(1) | O(n) |
| `remove(K)` | O(1) | O(n) |

**Used by:** `RoomCacheService` — caches all rooms by room number for O(1) lookup.  
Separate chaining collision resolution. Auto-resizes at load factor 0.75.

---

## Running Tests

```bash
# Run all unit tests
./mvnw test
```

**Test Results (46 total):**

| Test Class | Tests | Coverage |
|---|---|---|
| `CustomCircularQueueTest` | 6 | FIFO order, overflow, underflow, wrap-around |
| `CustomHashTableTest` | 5 | put/get, update, remove, containsKey, resize |
| `BookingServiceTest` | 6 | success, past date, invalid checkout, overlap, cancel |
| `CheckInOutServiceTest` | 8 | check-in/out transitions, invalid statuses, not-found |
| `PaymentServiceTest` | 3 | GST calculation, success, duplicate rejection |
| `RoomServiceTest` | 9 | add, duplicate, search filters, type lookup |
| `RoomApiControllerTest` | 8 | all endpoints, 404, cache hit/miss |
| `HotelBookingApplicationTests` | 1 | ⏭️ Skipped (requires MySQL) |

> Integration test (`HotelBookingApplicationTests`) is `@Disabled` — run separately with a live MySQL instance.

---

## Project Structure

```
f:/hotel-booking/
├── src/main/java/com/hotel/booking/
│   ├── controller/          # MVC + REST controllers
│   │   ├── AdminController.java
│   │   ├── AuthController.java
│   │   ├── BookingApiController.java
│   │   ├── BookingController.java
│   │   ├── CustomerController.java
│   │   ├── HomeController.java
│   │   ├── PaymentApiController.java
│   │   ├── QueueApiController.java
│   │   ├── ReceptionController.java
│   │   ├── ReportApiController.java
│   │   ├── ReportController.java
│   │   ├── RoomApiController.java
│   │   └── AdminRoomController.java
│   ├── ds/                  # Custom DSA (from scratch)
│   │   ├── CustomCircularQueue.java
│   │   └── CustomHashTable.java
│   ├── dto/                 # Request/Response DTOs
│   ├── entity/              # JPA Entities + Enums
│   ├── exception/           # Custom exceptions + GlobalExceptionHandler
│   ├── repository/          # Spring Data JPA repositories
│   ├── security/            # SecurityConfig, CustomUserDetailsService
│   └── service/             # Business logic layer
├── src/main/resources/
│   ├── templates/           # Thymeleaf HTML templates
│   │   ├── fragments/       # navbar.html, footer.html
│   │   └── *.html           # All page templates
│   ├── static/css/          # style.css (Bootstrap + custom)
│   └── application.properties
├── src/test/java/com/hotel/booking/
│   ├── controller/          # RoomApiControllerTest
│   ├── ds/                  # Queue + HashTable tests
│   └── service/             # Service layer tests
└── pom.xml
```

---

## Team Members

**Team 8 — HCL Java Training**

| Name | Role |
|---|---|
| Team Member 1 | Backend / Spring Boot |
| Team Member 2 | Database / JPA |
| Team Member 3 | Frontend / Thymeleaf |
| Team Member 4 | Security / Testing |

---

## Git Commit Guidelines

```
feat: add check-in queue with CustomCircularQueue DSA
fix: correct DaoAuthenticationProvider constructor for Spring Boot 4
test: add CheckInOutService unit tests with Mockito
docs: update README with API reference
```

**Branch strategy:** `main` (production) · `develop` (integration) · `feature/*` (features)
