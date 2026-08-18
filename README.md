# Waypoint Carpool API — Spring Boot backend

A REST API for the carpooling app: registration/login (JWT), car
registration, posting rides with mid-stops, searching/joining rides, and
status tracking for both rides and bookings. Same domain model as the
Supabase version, reimplemented with Spring Data JPA + Spring Security.

## Requirements

- Java 17+
- Maven 3.9+ (uses the Maven Wrapper conventions; a plain `mvn` install works too)
- PostgreSQL 14+ **or** just use the built-in H2 profile to try it instantly

## Quick start (no install, in-memory H2)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

The API comes up on **http://localhost:8080**. Data resets every restart —
good for trying things out, not for real use.

## Running against PostgreSQL

```bash
createdb carpool
```

Then either export env vars or edit `src/main/resources/application.properties` directly:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/carpool
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=$(openssl rand -base64 48)   # use a real secret in production
mvn spring-boot:run
```

Hibernate creates/updates all tables automatically on startup
(`spring.jpa.hibernate.ddl-auto=update`) — no manual schema.sql needed.

## Configuration

All of these can be overridden via environment variables (see
`application.properties`):

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | localhost / postgres / postgres | PostgreSQL connection |
| `JWT_SECRET` | a dev placeholder — **change this** | signs JWTs, must be 32+ bytes |
| `JWT_EXPIRATION_MS` | 86400000 (24h) | token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | frontend origin(s), comma-separated |
| `COMMISSION_PERCENT` | `10` | % of a ride's total price kept as platform commission (see below) |

## API overview

All endpoints are under `/api`. Everything except `/api/auth/**` requires
`Authorization: Bearer <token>`.

**Auth**
```
POST /api/auth/register   { fullName, email, password }  -> { token, user }
POST /api/auth/login      { email, password }             -> { token, user }
GET  /api/auth/me                                          -> current user
```

**Cars** — registering one flips the user's `isDriver` flag to true
```
GET    /api/cars/mine
POST   /api/cars     { make, model, color, plateNumber, seats }
DELETE /api/cars/{id}
```

**Rides**
```
GET   /api/rides?source=&destination=&date=YYYY-MM-DD   (scheduled + seats > 0)
GET   /api/rides/{id}
GET   /api/rides/mine/driving
POST  /api/rides   { carId, source, destination, departureTime, availableSeats,
                      pricePerSeat, notes, stops: [{ stopName, estimatedTime }] }
PATCH /api/rides/{id}/status   { status: SCHEDULED|ONGOING|COMPLETED|CANCELLED }
```

**Bookings**
```
POST  /api/rides/{rideId}/bookings          { seatsBooked, pickupStopId? }
GET   /api/rides/{rideId}/bookings          (driver only — all riders on the ride)
GET   /api/rides/{rideId}/bookings/mine     (204 if none)
GET   /api/bookings/mine                    (this user's full ride history)
PATCH /api/bookings/{id}/status             { status: PENDING|CONFIRMED|CANCELLED|COMPLETED }
```
Seat counts are adjusted server-side automatically: joining decrements a
ride's `availableSeats`, cancelling a pending/confirmed booking gives them
back.

**Notifications** — in-app only, created automatically by the booking flow
```
GET   /api/notifications                (mine, newest first)
GET   /api/notifications/unread-count   -> { count }
PATCH /api/notifications/{id}/read
PATCH /api/notifications/read-all
```
Triggered automatically:
- rider joins a ride &rarr; notifies the **driver** (`BOOKING_REQUESTED`)
- driver confirms a pending request &rarr; notifies the **rider** (`BOOKING_CONFIRMED`)
- driver declines a pending request &rarr; notifies the **rider** (`BOOKING_DECLINED`)
- rider cancels an already-confirmed seat &rarr; notifies the **driver** (`BOOKING_CANCELLED`)

## Pricing & platform commission

Each ride now has two price fields set by the driver:
- `pricePerSeat` — what an individual rider pays (shown to everyone)
- `totalPrice` — what the whole ride is worth to the driver (**driver-only**, used for the payout breakdown)

At creation time, the ride snapshots the current `app.commission.percent`
(default 10%, override via `COMMISSION_PERCENT` env var) and computes:

```
platformCommissionAmount = totalPrice × platformCommissionPercent / 100
driverPayout             = totalPrice − platformCommissionAmount   (computed in the DTO, not stored)
```

This is bookkeeping only — nothing is actually charged or paid out. The
rate is stored per-ride (not just read from config) so historical rides
keep the rate that applied when they were created even if the platform
default changes later.

`GET /api/rides/{id}` and `GET /api/rides/mine/driving` only include
`totalPrice` / `platformCommissionPercent` / `platformCommissionAmount` /
`driverPayout` in the response when the caller **is** that ride's driver —
riders and the public search endpoint never see them.

**Payments** — self-reported UPI-style payment tracking, created automatically when a driver confirms a booking
```
GET  /api/bookings/{id}/payment              (payer or payee only)
POST /api/bookings/{id}/payment/mark-paid    { transactionRef? }   (payer/rider only)
PATCH /api/auth/me/upi                       { upiId }             (set your own UPI ID)
```

## Payments — how it works (and its limits)

This is **not** a real payment gateway integration. No money moves through
the app, no bank/UPI provider is contacted, and nothing here is PCI
compliant or production-payment-ready. What it actually does:

1. A driver sets their UPI ID once, from "My cars &rarr; Payment details"
   (`PATCH /api/auth/me/upi`).
2. Riders join, get confirmed, and ride along as normal — no payment is
   requested yet.
3. When the driver marks the ride **COMPLETED**, a `Payment` row is
   created automatically for every rider whose booking was `CONFIRMED`
   (`amount = ride.pricePerSeat × seatsBooked`, status `PENDING`), and
   each rider gets a `PAYMENT_REQUESTED` notification. Payment is
   deliberately requested only *after* the ride happens — safer for the
   rider than paying up-front for a ride that might get cancelled partway
   through.
4. The rider's ride page then shows that amount, the driver's UPI ID, and
   a `upi://pay?...` deep link (opens their UPI app directly on a phone)
   — generated by `PaymentResponse`, not stored.
5. The rider pays the driver **directly**, however they actually settle it
   (UPI app, cash, anything) — this app has no way to know a transfer
   happened — then taps **"I've paid"**, optionally typing in a UPI
   transaction reference for their own records.
6. That flips the payment to `PAID` and fires a `PAYMENT_RECEIVED`
   notification to the driver.

Nothing here verifies the rider actually paid. It's a shared, timestamped
record of "the rider says they paid" — appropriate for a basic/demo app,
not for handling real transactions. A real integration would swap step 5
for a webhook from an actual PSP (Razorpay, Cashfree, PhonePe's API, etc.)
confirming the transfer server-side instead of trusting the client.

## Authorization rules enforced server-side

- Only a car's owner can use it on a ride.
- Only a ride's driver can change its status or confirm/decline bookings.
- A rider can only cancel their own booking, not anyone else's.
- You can't join your own ride, join twice, or join a full/non-scheduled ride.

## Project structure

```
src/main/java/com/waypoint/carpool/
  entity/          User, Car, Ride, RideStop, RideBooking, Notification, Payment (+ enums/)
  repository/      Spring Data JPA repositories
  dto/             request/response records (auth, car, ride, booking, notification, payment)
  security/        JwtService, JwtAuthFilter
  config/          SecurityConfig (stateless JWT, CORS)
  service/         AuthService, CarService, RideService, BookingService, NotificationService, PaymentService
  controller/      AuthController, CarController, RideController, BookingController, NotificationController
  exception/       GlobalExceptionHandler + custom exceptions
```

## Google Sign-In

`POST /api/auth/google` accepts `{ "idToken": "..." }` — the ID token
(JWT credential) your frontend gets back from Google Identity Services. The
backend verifies it against Google's public keys and your OAuth client ID
before trusting anything in it, then finds or creates the matching user and
returns the same `{ token, user }` shape as `/api/auth/login`.

**Setup:**

1. Get an OAuth 2.0 Client ID: Google Cloud Console → APIs & Services →
   Credentials → Create Credentials → OAuth client ID → Web application.
   Add your frontend origin(s) (e.g. `http://localhost:5173`) under
   *Authorized JavaScript origins*.
2. Set the **same** client ID on both sides:
   - Backend: `app.google.client-id` in `application.properties` /
     `application-h2.properties` (or env var `GOOGLE_CLIENT_ID`).
   - Frontend: `VITE_GOOGLE_CLIENT_ID` in `.env`.
3. If your Postgres database already exists and you're not using
   `spring.jpa.hibernate.ddl-auto=update`, add these columns by hand:
   ```sql
   ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
   ALTER TABLE users ADD COLUMN provider VARCHAR(255) NOT NULL DEFAULT 'LOCAL';
   ALTER TABLE users ADD COLUMN google_id VARCHAR(255) UNIQUE;
   ```
   (The H2 trial profile already has `ddl-auto=update`, so it picks these up
   automatically — no manual step needed there.)

An account that signs up with email/password first and later signs in with
Google on the same email gets linked automatically (same row, `google_id`
gets filled in) rather than creating a duplicate account.

## Note on this environment

This project was written and reviewed carefully but **not compiled here** —
the sandbox this was built in can't reach Maven Central to resolve Spring
Boot/JPA/JWT dependencies. Run `mvn spring-boot:run -Dspring-boot.run.profiles=h2`
on your own machine first; if anything doesn't compile, send me the error
and I'll fix it immediately.
