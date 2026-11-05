# HotelEase — Hotel Booking & Management System

> **HCL Java Training | Team 8 Assignment**  
> Spring Boot 4 · Java 19 · MySQL · Thymeleaf · Bootstrap 5.3

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Quick Start](#quick-start)
4. [Default Accounts](#default-accounts)
5. [How the Main Workflows Work](#how-the-main-workflows-work)
6. [Database Schema](#database-schema)
7. [API Reference](#api-reference)
8. [Custom Data Structures](#custom-data-structures)
9. [Running Tests](#running-tests)
10. [Project Structure](#project-structure)

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

Spring Security protects the web pages with role-based access:
- Public pages include `/`, `/rooms/**`, `/register`, and `/login`.
- `ROLE_CUSTOMER` can access customer, booking, and personal booking pages.
- `ROLE_RECEPTION` can access reception pages and customer/booking pages.
- `ROLE_ADMIN` can access admin pages and customer/booking pages.

The current configuration permits all `/api/**` endpoints without authentication.
See the [API security note](#api-security-note) before using the API outside a local demo.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.9 |
| Language | Java 19 |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| View Layer | Thymeleaf 3 + Bootstrap 5.3.3 |
| Security | Spring Security |
| Testing | JUnit 5 + Mockito |
| Build | Maven 3 |
| Dev Reload | Spring Boot DevTools |

---

## Quick Start

### Prerequisites
- Java 19+
- MySQL 8 running on port 3306

The included Maven Wrapper downloads and runs the required Maven version, so a separate Maven installation is not needed.

### 1 — Create the database

```sql
CREATE DATABASE hotel_booking_db;
```

### 2 — Configure MySQL credentials

Edit `src/main/resources/application.properties` and set your local MySQL username and password:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hotel_booking_db
spring.datasource.username=root
spring.datasource.password=YOUR_LOCAL_MYSQL_PASSWORD
spring.jpa.hibernate.ddl-auto=update
server.port=8080
```

### 3 — Run the application

From PowerShell in the project directory on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

When startup completes, open **http://localhost:8080**. Stop the app with `Ctrl+C` in the terminal.

Hibernate creates or updates tables because `spring.jpa.hibernate.ddl-auto=update` is configured. `DataInitializer` adds sample rooms and room types to an empty database and creates each demo account if it does not already exist. Existing rooms and room types are not reset on every startup.

If the app cannot connect, check that MySQL is running, that `hotel_booking_db` exists, and that the credentials above are correct. If port 8080 is already in use, stop the other process or change `server.port`.

---

## Default Accounts

| Role | Email | Password | Dashboard |
|---|---|---|---|
| Admin | admin@hotelease.com | admin123 | `/admin/dashboard` |
| Reception | reception@hotelease.com | reception123 | `/reception/dashboard` |
| Customer | customer@hotelease.com | customer123 | `/customer/dashboard` |

The demo passwords are stored as BCrypt hashes. The initializer creates these accounts if they are missing; it does not reset an existing account's password on every startup.

Use `/register` to create another customer account. Registration does not create admin or reception accounts.

---

## How the Main Workflows Work

### Customer booking

1. Browse rooms at `/rooms` and open a room's details.
2. Sign in or register, then submit a booking with a room, dates, and guest count.
3. The application checks that the dates are valid, the guest count fits the room, the room is not under maintenance, and the dates do not conflict with another active booking.
4. The booking starts as `CONFIRMED`; the room charge is nightly price multiplied by the number of nights.
5. Customers can review their bookings in their dashboard or at `/my-bookings`. A customer can cancel their own eligible booking.

### Reception check-in, checkout, and payment

Reception staff can view today's arrivals and departures, add confirmed bookings to the waiting queue, and serve the next guest. Check-in changes the booking to `CHECKED_IN`, marks the room `OCCUPIED`, and records the staff member and actual check-in time. Checkout changes the booking to `CHECKED_OUT`, marks the room `AVAILABLE`, and records the actual checkout time.

The bill includes the booking's room charge, optional additional charges, and 12% tax. Payment is recorded in the application's database; there is no connection to a real payment gateway.

### Admin

Admins can review dashboard totals, manage rooms and bookings, cancel bookings, and view occupancy and revenue reports.

---

## Database Schema

The diagram below is a simplified relationship overview; field names and types are defined by the JPA entities in `src/main/java/com/hotel/booking/entity/`.

```
┌──────────┐       ┌──────────┐       ┌──────────────┐
│  users   │       │  rooms   │       │  room_types  │
├──────────┤       ├──────────┤       ├──────────────┤
│ id (PK)  │       │ id (PK)  │       │ id (PK)      │
│ full_name│       │room_number│      │ name         │
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
         │ number_of_guests            │
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
│ payment_method │   │ actual_check_out   │
│ payment_status │   │ checked_in_by      │
│ transaction_reference │
│ payment_date   │   └────────────────────┘
└────────────────┘
```

**BookingStatus enum:** `PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT | CANCELLED`  
**RoomStatus enum:** `AVAILABLE | OCCUPIED | MAINTENANCE`  
**PaymentStatus enum:** `PENDING | COMPLETED | FAILED | REFUNDED`

**PaymentMethod enum:** `CASH | CREDIT_CARD | DEBIT_CARD | UPI | NET_BANKING`

---

## API Reference

### Rooms

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/rooms` | Public* | List all rooms |
| `GET` | `/api/rooms/{id}` | Public* | Get room by ID |
| `GET` | `/api/rooms/available` | Public* | List available rooms |
| `GET` | `/api/rooms/lookup/{roomNumber}` | Public* | Look up a room by room number |
| `POST` | `/api/rooms` | Public* | Create a room |
| `PUT` | `/api/rooms/{id}` | Public* | Update a room |
| `DELETE` | `/api/rooms/{id}` | Public* | Delete a room |

### Bookings

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/bookings` | Public* | Create a booking |
| `GET` | `/api/bookings/{id}` | Public* | Get booking by ID |
| `GET` | `/api/bookings/my` | Public* | Get bookings for the selected/default user |
| `PUT` | `/api/bookings/{id}/cancel` | Public* | Cancel a booking |
| `POST` | `/api/bookings/{id}/check-in` | Public* | Check in a guest |
| `POST` | `/api/bookings/{id}/check-out` | Public* | Check out a guest |

### Payments

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/payments/bill/{bookingId}` | Public* | Get a bill; optional `additionalCharges` query parameter |
| `POST` | `/api/payments` | Public* | Record a payment |
| `GET` | `/api/payments/{bookingId}` | Public* | Get payment details for a booking |

### Reports

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/reports/occupancy` | Public* | Room counts and occupancy by room type |
| `GET` | `/api/reports/revenue` | Public* | Revenue grouped by payment method and room type |

### Queue (DSA Demo)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/queue` | Public* | View waiting queue |
| `POST` | `/api/queue/{bookingId}` | Public* | Add a booking to the queue |
| `POST` | `/api/queue/serve` | Public* | Serve the next booking and check in the guest |

### API security note

`SecurityConfig` currently permits `/api/**` without authentication. The `Public*` label above describes the current code, not a recommended production policy. Room changes, booking actions, payments, queue operations, and reports are therefore not restricted to the web roles through Spring Security. Protect these endpoints and validate ownership and authorization before exposing the API beyond a trusted local demo.

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

## Custom Data Structures

The queue and hash table implementations are written for this project. They use Java arrays and some Java collection types for helper methods that return list snapshots.

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

```text
# Windows PowerShell
.\mvnw.cmd test

# macOS or Linux
./mvnw test
```

The tests cover the custom data structures, services, API controller, and application startup. Check the Maven test output for the current results; database-dependent tests may require a running MySQL instance.

---

## Project Structure

```
hotel-booking/
├── src/main/java/com/hotel/booking/
│   ├── controller/          # MVC + REST controllers
│   │   ├── AdminController.java
│   │   ├── AdminRoomController.java
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
│   │   └── RoomController.java
│   ├── config/              # Startup seed data
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
├── mvnw.cmd                 # Maven Wrapper for Windows
└── pom.xml                  # Maven dependencies and build configuration
```
