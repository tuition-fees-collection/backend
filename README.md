# Tuition Network backend

This repository currently provides a Spring Boot application and a PostgreSQL
database schema. Business controllers, entities, repositories, payment integrations,
and a frontend have not been implemented here yet. Starting it verifies the
foundation; it does not provide a working tuition payment product.

## Start everything

Install Git and Docker Desktop (Windows/macOS), or Docker Engine with Compose 2.23.1+
(Linux). Start Docker before running these commands. On Windows, use Linux
containers with the WSL 2 backend. The first build needs internet access and can
take several minutes. Docker supplies Java and Maven, so a host JDK is optional
for this route.

From the repository root, in Terminal or PowerShell:

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
| Backend | http://localhost:8080 | Spring Boot server |
| pgAdmin | http://localhost:5051 | Browser interface to inspect PostgreSQL |
| PostgreSQL | localhost:5433 | Database connection from your IDE/host |

## Swagger / OpenAPI

With the backend running, open http://localhost:8080/swagger-ui.html.
The JSON API contract is at http://localhost:8080/v3/api-docs. If you override
the backend host port, use that port in these URLs.

The project uses springdoc-openapi 3.1.0 for Spring Boot 4
([official documentation](https://springdoc.org/)). `OpenApiConfig` sets the
project title, description, and API version. There are no business controllers
yet, so an empty operations list is expected. New Spring MVC controller endpoints
will appear automatically; no sample business endpoint has been added.

`SecurityConfig` permits anonymous access to Swagger UI and the JSON documentation
routes. Other application routes require authentication. Form login, HTTP Basic,
and default CSRF protection remain enabled. Documentation access is public in
every profile with this configuration; revisit that policy before deployment.
The metadata describes the planned API scope, not already implemented features.

The normal `clean verify` command also tests the documentation JSON, project
metadata, Swagger UI assets, and protection of non-documentation routes. Existing
CI runs these tests automatically; no separate Swagger installation is needed.

Spring Security currently supplies its default login page. Use username `user`
and the generated password in `docker compose logs app` if you want to inspect
it. The password changes on restart. There is no home-page controller, so a 404
after login is expected. An unauthenticated API request may return 401.

pgAdmin runs in desktop mode (`SERVER_MODE=False`), so its web UI normally opens
without a login. Its configured account is `admin@tuitionnetwork.dev`, password
`tuition`. Expand **Servers → Tuitionnetwork (local)** and enter database password
`tuition` when asked. Database name and username are both `tuition`.

These credentials and loopback-only port bindings are for local development.
Each teammate has their own Docker volumes and database; records are not shared.

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

macOS/Linux:

```sh
./mvnw clean verify
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Alternatively run `TuitionnetworkApplication.main()` in the IDE. Do not run the
Docker app and IDE app on port 8080 simultaneously. Stop the IDE process before
returning to the full Docker stack. Tests require PostgreSQL to be running and
apply migrations to the configured database; use only a local development/test
database. CI uses a fresh PostgreSQL instance.

## How it fits together

- `pom.xml` selects Java 21, Spring Boot 4.1.1, Spring AI 2.0.1, and dependencies.
- `TuitionnetworkApplication.java` starts Spring, its embedded web server,
  security, and database configuration.
- `application.properties` configures the host database connection. Compose
  overrides it with `postgres:5432` inside Docker; `localhost` in a container
  refers to that container itself.
- Flyway applies `src/main/resources/db/migration/V1__init_schema.sql` and then
  `V2__sync_with_mock_services.sql`, recording them in
  `public.flyway_schema_history`. The schemas separate identity, billing,
  payments, instalments, receipts, notifications, reporting, integration, and audit.
- Hibernate uses `ddl-auto=validate`, so schema changes belong in Flyway SQL.
  There are no JPA entities yet, so entity-to-table validation is currently limited.
- The Dockerfile builds the JAR with a JDK and runs it with a smaller Java runtime.
  It skips tests because the image build has no database; run `clean verify`
  separately. CI does this before building the image.
- pgAdmin is optional database tooling. The app talks directly to PostgreSQL.
- The Spring AI PostgresML dependency is present, but plain `postgres:16` does
  not provide the PostgresML extension. No AI feature calls it yet; startup passing
  does not establish that embeddings work. Implement that infrastructure before
  adding embedding calls. See the [Spring AI documentation](https://docs.spring.io/spring-ai/reference/api/embeddings/postgresml-embeddings.html).

## Team workflow

1. Use feature branches and pull requests. Pull the latest changes before work.
2. Keep JDK 21, wrapper files, Docker configuration, and dependency changes shared
   through Git. Do not commit `.env`, IDE files, credentials, or database contents.
3. Never edit a migration already applied by teammates. Add the next versioned
   SQL file and coordinate version numbers to avoid duplicate versions.
4. Run `clean verify` before opening a PR. The GitHub workflow repeats it against
   fresh PostgreSQL and builds the Docker image. Enable its required status check
   in branch protection if the team wants to enforce it.
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

If a port is occupied, copy `tuitionnetwork/.env.example` to `tuitionnetwork/.env`
and change the host port, then run Compose again. For example, `APP_PORT=8081`
makes the Docker backend available on http://localhost:8081. Internal container
ports remain unchanged.

Compose reads `.env` automatically; Java launched from the IDE/Maven does not.
If you change `POSTGRES_PORT`, also set that environment variable in the IDE or
shell. `APP_PORT` only changes Docker's host binding; for a host Java process use
`SERVER_PORT` if needed. Spring also accepts `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` overrides.

| Symptom | Check |
| --- | --- |
| Cannot connect to Docker | Start Docker Desktop or the Docker daemon |
| Database connection refused | Run `docker compose ps` and wait for PostgreSQL health |
| Address already in use | Stop the other app or change host ports |
| `release version 21 not supported` | Check `java -version` and the IDE Maven JDK |
| `./mvnw: Permission denied` | Run `chmod +x mvnw` on macOS/Linux, or use `sh mvnw` |
| Flyway checksum mismatch | Restore the original migration; add a new migration |
| Dependencies/images fail to download | Check internet, VPN/proxy, and registry access |
| pgAdmin cannot connect | Inside Docker use host `postgres`, port `5432` |

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

## Verification recorded on 2026-09-05

On this Apple Silicon Mac with JDK 21 and Docker Compose 2.30.3:

- `./mvnw -B clean verify`: passed, 5 tests, 0 failures/errors (including Swagger security checks).
- V1 and V2 applied to a fresh local database and validated on subsequent app startup.
- The final Docker image built and the backend started with PostgreSQL connected.
- Backend `/login`: HTTP 200; unauthenticated `/`: HTTP 401, as expected.
- Swagger UI and `/v3/api-docs`: HTTP 200 without login; `/api/v1/fees`: HTTP 401 without authentication.
- pgAdmin web interface: HTTP 200.
- Image manifests include Linux ARM64 and AMD64 for PostgreSQL, both Java images,
  and the pinned pgAdmin image. Other teammates' devices have not been tested.
- The added GitHub Actions workflow has not yet run on GitHub.
