# Campus Resource Sharing & Management System

Layered Spring Boot monolith. Students, faculty, and staff lend and borrow physical campus items. A listing is an offer. Each physical item is its own `ResourceUnit`. A reservation is only a request. A `BorrowingRecord` is created at pickup, not at approval.

## Assumptions

- The sandbox JDK is 17, so the compiler target is 17. The same sources run on Java 21.
- Local profile uses an H2 file database in PostgreSQL compatibility mode so the app can start without a database server. **H2 with Hibernate is not a native OODBMS and is not PostgreSQL.**
- PostgreSQL is the intended shared database. Start it with the `postgres` profile or Docker Compose.
- Overlap protection is a transaction plus `PESSIMISTIC_WRITE` on the unit row and a service query. A GiST exclusion constraint is PostgreSQL-specific and is not in the shared Flyway script, because that script also runs on H2.
- Self-reservation and self-approval are rejected.
- A unit may be requested for a non-overlapping future window while it is `RESERVED` or `BORROWED`. It cannot be reserved when status is `MAINTENANCE_REQUIRED`, `UNDER_REPAIR`, `INACTIVE`, or `LOST`.
- Pickup uses `POST /api/borrowings/{reservationId}/pickup` because the borrowing row does not exist until pickup. Return uses the borrowing id.
- The seeded administrator is a `Staff` record with role `ADMIN`, because `User` is abstract and administrator is not its own subclass.
- Charges are calculated from actual pickup-to-return time. Payment rows are simulated (`SIMULATED` or `WAIVED`). No payment gateway.
- Demo passwords exist only so a class demonstration can sign in. Override `ADMIN_PASSWORD`. Do not ship the default.

## Run

```bash
cd crsms
export ADMIN_PASSWORD='choose-a-password'
mvn spring-boot:run
```

Open the home page served by the app. Demo members when `crsms.seed-demo=true`:

| Email | Password | Role |
| --- | --- | --- |
| alice@campus.edu | Student@123 | Student, owns BIKE-01 and BIKE-02 |
| bob@campus.edu | Student@123 | Student |
| carol@campus.edu | Faculty@123 | Faculty |
| admin@campus.edu | value of ADMIN_PASSWORD | Admin |

PostgreSQL:

```bash
export SPRING_PROFILES_ACTIVE=postgres
export DATABASE_URL=jdbc:postgresql://localhost:5432/crsms
export DATABASE_USERNAME=crsms
export DATABASE_PASSWORD=secret
export ADMIN_PASSWORD=secret
mvn spring-boot:run
```

Docker Compose from this directory starts Postgres 16 and the app on port 8080. Flyway applies `src/main/resources/db/migration`.

Tests:

```bash
mvn test
```

JMeter plan `jmeter/crsms.jmx` is a template for 10, 50, 100, or 200 users (`-Jusers=50`). This repository does not invent throughput numbers. Run it against a deployed instance and record average, p95, throughput, and error rate yourself.

## Persistence and a future OODBMS

Domain classes do not reference JDBC. Services depend on Spring Data repository interfaces. Replacing PostgreSQL with ObjectDB means providing another implementation of those interfaces (or ObjectDB's JPA provider behind the same entities) and leaving services unchanged. Hibernate on PostgreSQL remains a relational mapping, not a native object database.

## Package layout

`com.crsms` config, controller, service, model, repository, dto, security, exception, pricing.
