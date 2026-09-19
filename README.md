# Tuition Network backend

Spring Boot API for a tuition-fees collection network: institutions upload
outstanding fees, guardians or bank officers pay them, receipts are issued, and
optional instalment (EPP) plans are created against a bank mock.

There is no frontend in this repository. Card, source, EPP, and MOI calls go to
an external bank-mock service (default `http://localhost:8000`), which is not
shipped here.

## Start everything

Install Git and Docker Desktop (Windows/macOS), or Docker Engine with Compose 2.23.1+
(Linux). Start Docker before running these commands. On Windows, use Linux
containers with the WSL 2 backend. The first build needs internet access and can
take several minutes. Docker supplies Java and Maven, so a host JDK is optional
for this route.

The app will not start without crypto secrets. From `tuitionnetwork`, export at
least these (CI uses the same names). JWT and national-id keys must decode to
32+ bytes:

```sh
export JWT_SECRET_BASE64='MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE='
export NATIONAL_ID_HMAC_KEY_BASE64='YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNERUY='
export NATIONAL_ID_ENCRYPTION_KEY_BASE64='MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY='
```

Do not reuse these values outside local development.

```sh
cd tuitionnetwork
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 app
```

Wait for the app log to say `Started TuitionnetworkApplication`. An `Up` container
alone does not prove the application has finished starting.

| Component | Local address | Purpose |
| --- | --- | --- |
| Backend | http://localhost:8080 | Spring Boot API |
| Swagger UI | http://localhost:8080/swagger-ui.html | Interactive OpenAPI |
| OpenAPI JSON | http://localhost:8080/v3/api-docs | Machine-readable contract |
| pgAdmin | http://localhost:5051 | Browser interface to inspect PostgreSQL |
| PostgreSQL | localhost:5433 | Database connection from your IDE/host |
| Bank mock | http://localhost:8000 | External; required for card, source, EPP, MOI |

Compose points the app at `http://host.docker.internal:8000` for the bank mock.

## Authentication

The API is a stateless JWT resource server. CSRF is disabled. There is no form
login and no default `user` password.

1. `POST /api/v1/auth/login` with `{"username","password"}` (no token).
2. Use `Authorization: Bearer <token>` on every other `/api/**` route.
3. Tokens are HS256, issuer `tuitionnetwork`, audience `tuitionnetwork-api`,
   15-minute lifetime. Claims include `actor_id`, `actor_type`, `role`,
   `token_version`, and optional `institution_id` / `branch`.
4. Deactivating an account or changing `token_version` invalidates outstanding
   tokens. Five failed logins lock the account for 15 minutes.

Roles (`identity.role`, enforced in services rather than `@PreAuthorize`):

| Role | Typical access |
| --- | --- |
| `PAYER` | Pay fees for linked students (guardian); list own fees. Online payments are stamped with the seeded ONLINE CHANNEL employee. |
| `BANK_OFFICER` | Create payments for any linked student; cash collection; payment history; fee search by national ID. |
| `INSTITUTION_OFFICER` | CSV fee uploads, student fee records, collections, and daily-collection reports for the institution on the JWT. |
| `ADMIN` | Institutions, staff, and auth-account lifecycle. |

Admin HTTP APIs create backoffice and institution logins. There is no public
endpoint to provision a payer account.

Swagger UI and `/v3/api-docs` are anonymous in every profile; revisit that
before deployment. Authorize in Swagger with the Bearer token from login.
`OpenApiConfig` sets the title and version; live controllers appear
automatically.

Unauthenticated API calls return 401. There is no home-page controller, so
`GET /` is 401 without a token and 404 with one.

pgAdmin runs in desktop mode (`SERVER_MODE=False`), so its web UI normally opens
without a login. Its configured account is `admin@tuitionnetwork.dev`, password
`tuition`. Expand **Servers → Tuitionnetwork (local)** and enter database password
`tuition` when asked. Database name and username are both `tuition`.

These credentials, sample secrets, and loopback-only port bindings are for local
development. Each teammate has their own Docker volumes and database; records
are not shared.

## HTTP API

Base path `/api/v1`. Create-payment and create-EPP-plan require header
`Idempotency-Key`.

| Area | Routes |
| --- | --- |
| Auth | `POST /auth/login` |
| Payments | `POST /payment-orders`, `POST …/{id}/pay`, `/pay-cash`, `/pay-source`, `/3ds`, `GET …/{id}`, `GET …/{id}/receipt`, `GET /payment-orders`, `GET /payment-orders/customer-lookup` |
| EPP | `GET /epp/quotes`, `POST /epp/plans`, `GET /epp/plans`, `GET /epp/plans/{id}`, `POST /epp/plans/{id}/cancel` |
| Uploads | `POST /integration/uploads`, `GET /integration/uploads`, `GET /integration/uploads/{batchId}` |
| Billing | `GET /billing/students/{studentId}/fees`, `GET /billing/collections` |
| Fee search | `POST /fee-searches` (bank officer, national ID), `GET /fee-searches/me` (payer) |
| Reporting | `GET /reporting/daily-collections`, `/daily-collections/csv`, `/daily-collections/pdf` |
| Admin | `/admin/institutions…`, `/admin/backoffice-employees…`, `/admin/accounts…` |
| Reconciliation | `/admin/gateway-checks` (also runs on a 120s schedule) |

Missing guardian on fee search returns an empty list. There is no public
endpoint to provision a payer account.

Reconciliation, EPP list/quotes, and customer-lookup currently require only a
valid JWT (no extra role check in those controllers).

## Develop with the IDE

Install JDK 21 and set both the IDE project SDK and Maven runner to it. Open
`tuitionnetwork/pom.xml` as a Maven project. Use the checked-in Maven wrapper,
which selects Maven 3.9.16; a separate Maven install is unnecessary.

Run the database and pgAdmin in Docker, then run Java on your computer:

```sh
cd tuitionnetwork
docker compose stop app
docker compose up -d --wait postgres pgadmin
```

Export the same crypto secrets (and `BANKMOCK_BASE_URL` if the mock is not on
`localhost:8000`). Host Java uses `localhost:${POSTGRES_PORT:5432}` unless you
override `SPRING_DATASOURCE_*`. Compose maps Postgres to host **5433** by
default, so set `POSTGRES_PORT=5433` for IDE/Maven runs.

macOS/Linux:

```sh
export POSTGRES_PORT=5433
./mvnw clean verify
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
$env:POSTGRES_PORT=5433
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Alternatively run `TuitionnetworkApplication.main()` in the IDE. Do not run the
Docker app and IDE app on port 8080 simultaneously. Stop the IDE process before
returning to the full Docker stack. Tests require PostgreSQL to be running and
apply migrations to the configured database; use only a local development/test
database. CI uses a fresh PostgreSQL instance.

Optional sample rows: `tuitionnetwork/scripts/seed-dev-data.sql` (not a Flyway
migration). It wipes previous demo logins and inserts:

| Username | Role | Portal |
| --- | --- | --- |
| `nadia` | ADMIN | `/backoffice/login` |
| `karim` | BANK_OFFICER | `/backoffice/login` |
| `laila` | INSTITUTION_OFFICER | `/institution/login` (Nile Valley School) |
| `heba` | INSTITUTION_OFFICER | `/institution/login` (Maadi Language School) |
| `mona` | PAYER | API only (`GET /api/v1/fee-searches/me`) |

Password for each: `Admin123!`. Search / lookup National ID: `29805150101023`.
Then `./scripts/walkthrough-cycle.sh` for the API cycle.

## How it fits together

- `pom.xml` selects Java 21, Spring Boot 4.1.1, Spring AI 2.0.1 (unused),
  springdoc-openapi 3.1.0, commons-csv 1.12.0, and OAuth2 resource-server JWT.
- `TuitionnetworkApplication.java` starts Spring, the embedded web server,
  security, JPA, Flyway, and scheduling (outbox poll and gateway sweeps).
- `application.properties` configures the host database, Flyway schemas, JWT
  and national-id keys, bank-mock URL/API key, and multipart limits (5MB file /
  6MB request). Compose overrides the datasource with `postgres:5432` inside
  Docker; `localhost` in a container refers to that container itself.
- Packages under `com.tuitionnetwork.tuitionnetwork`:
  - `identity` — login, JWT, roles, guardians/students, admin, MOI adapter, audit
  - `billing` — JPA `Fee`, institution collections, fee search
  - `integration` — CSV upload, batch dedupe, guardian/student/fee provisioning
  - `payment` — orders, saga, receipts, outbox, reconciliation
  - `payments` — RestClient adapters to the bank mock
  - `epp` — quotes and instalment plans (credit card only; tenors 3/6/12/18)
  - `reporting` — daily collection JSON, CSV, and PDF for an institution
  - `config` — `SecurityConfig`, `JwtCryptoConfig`, OpenAPI
- Flyway applies `src/main/resources/db/migration/V1__…` through `V10__…`,
  recording them in `public.flyway_schema_history`. Schemas: `identity`,
  `billing`, `payments`, `epp`, `receipts`, `notifications`, `reporting`,
  `integration`, `common`. Hibernate `ddl-auto=validate`; schema changes belong
  in new Flyway files, never in already-applied ones.
- Payment flow: `POST /payment-orders` creates `CREATED` + allocations;
  `/pay`, `/pay-cash`, or `/pay-source` runs the saga (`PENDING` → charge →
  settle fees → receipt → `SUCCESSFUL`, or `FAILED` / `RECON_REQUIRED`).
  Outbox events are persisted and currently logged only; `notifications` and
  `payments.settlement` tables have no Java writers yet.
- The Dockerfile builds the JAR with a JDK and runs it with a smaller Java
  runtime. It skips tests because the image build has no database; run
  `clean verify` separately. CI does this before building the image.
- pgAdmin is optional database tooling. The app talks directly to PostgreSQL.
- The Spring AI PostgresML dependency is present, but plain `postgres:16` does
  not provide the PostgresML extension. No AI feature calls it. See the
  [Spring AI documentation](https://docs.spring.io/spring-ai/reference/api/embeddings/postgresml-embeddings.html).

## Tests

`./mvnw clean verify` runs the default suite (unit, MVC, and persistence tests
that use the configured Postgres). Tests tagged `@Tag("integration")` call the
live bank mock and are **excluded by default** (`excludedGroups=integration` in
`pom.xml`) so CI does not depend on that service.

```sh
./mvnw test -Dgroups=integration -DexcludedGroups=
```

## Team workflow

1. Use feature branches and pull requests. Pull the latest changes before work.
2. Keep JDK 21, wrapper files, Docker configuration, and dependency changes shared
   through Git. Do not commit `.env`, IDE files, credentials, or database contents.
3. Never edit a migration already applied by teammates. Add the next versioned
   SQL file and coordinate version numbers to avoid duplicate versions.
4. Run `clean verify` before opening a PR. The GitHub workflow repeats it against
   fresh PostgreSQL (with the crypto env vars set) and builds the Docker image.
   Enable its required status check in branch protection if the team wants to
   enforce it.
5. After pulling changes, rerun `docker compose up -d --build` for Docker mode,
   or restart the IDE app. Flyway applies new migrations automatically.

Java and the Maven wrapper are shared across operating systems; the Dockerfile
sets the shell wrapper executable bit even for Windows checkouts, and Git
attributes preserve shell/batch line endings. Use native Docker images for your
CPU architecture. Actual startup must still be checked on each teammate's device;
one successful machine cannot guarantee every OS, network, or Docker installation.
The CI workflow currently verifies Linux, not a Windows/macOS device matrix.

pgAdmin is pinned to the tested image digest. PostgreSQL stays on major version 16
and Temurin on Java 21; their tags still receive updates. Coordinate image updates
and run verification after pulling them.

## Ports and troubleshooting

Compose reads a `tuitionnetwork/.env` file automatically if you create one;
Java launched from the IDE/Maven does not. Host-port overrides:

| Variable | Default | Who reads it |
| --- | --- | --- |
| `APP_PORT` | 8080 | Compose host binding only (`SERVER_PORT` for a host JVM) |
| `POSTGRES_PORT` | 5433 in Compose; 5432 in `application.properties` | Compose host binding; also set it for IDE/Maven |
| `PGADMIN_PORT` | 5051 | Compose host binding |
| `BANKMOCK_BASE_URL` | `http://localhost:8000` | Spring (Compose sets `host.docker.internal:8000`) |
| `BANKMOCK_API_KEY` | `wit-intern-2026` | Spring |

Spring also accepts `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` overrides.

| Symptom | Check |
| --- | --- |
| Cannot connect to Docker | Start Docker Desktop or the Docker daemon |
| Database connection refused | Run `docker compose ps` and wait for PostgreSQL health |
| App exits on missing JWT/national-id keys | Export the three `*_BASE64` variables before Compose or `spring-boot:run` |
| Card / EPP / MOI calls fail | Start the bank mock on port 8000 (or point `BANKMOCK_BASE_URL` at it) |
| Address already in use | Stop the other app or change host ports |
| `release version 21 not supported` | Check `java -version` and the IDE Maven JDK |
| `./mvnw: Permission denied` | Run `chmod +x mvnw` on macOS/Linux, or use `sh mvnw` |
| Flyway checksum mismatch | Restore the original migration; add a new migration |
| Dependencies/images fail to download | Check internet, VPN/proxy, and registry access |
| pgAdmin cannot connect | Inside Docker use host `postgres`, port `5432` |
| Unauthenticated `/api/v1/**` is 401 | Call `/api/v1/auth/login` and send `Authorization: Bearer` |

Compose waits for the PostgreSQL health check before starting the app and pgAdmin,
as described in [Docker's startup-order documentation](https://docs.docker.com/compose/how-tos/startup-order/).
The pgAdmin server connection is defined inline in Compose, avoiding a host-file
bind mount. This uses [Compose configs content](https://docs.docker.com/reference/compose-file/configs/)
and requires Compose 2.23.1 or newer.

Useful commands, from `tuitionnetwork`:

```sh
docker compose logs --tail=100 app postgres pgadmin
docker compose exec postgres psql -U tuition -d tuition -c "SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;"
docker compose stop
docker compose up -d
docker compose down
```

`stop` and `down` preserve named database volumes. `down -v` deletes those volumes
and their data; it is not a routine fix for migration errors.
