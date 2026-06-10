# AGENTS.md — Java GraphQL Blueprint

## Project Overview

This is a **Spring Boot 4.0.6** reactive GraphQL boilerplate using **Domain-Driven Design** and **Test-Driven Development**. It serves as a starting point for AI coding agents to build Java backend services.

**Core stack:**
- **Java 25** (Gradle toolchain)
- **Spring Boot 4.0.6** with WebFlux (Netty, reactive)
- **Spring for GraphQL** — schema-first, annotation-based resolvers
- **PostgreSQL** via R2DBC (reactive) + JDBC (Flyway migrations)
- **Flyway** for database migrations
- **Testcontainers** for integration tests
- **Actuator** for health/metrics monitoring
- **Gradle 9.5.1** (Kotlin DSL)

## Build Commands

```bash
./gradlew test              # Run all tests
./gradlew bootRun           # Start the application locally
./gradlew bootBuildImage    # Build Docker image
./gradlew build             # Compile and package
```

## Blueprint Initialization

When instructed to create a new project from this blueprint, accept a **project name** and **full base package** (e.g. `my-api` / `com.acme.myapi`), then execute the following checklist mechanically. The leaf package (e.g. `myapi`) becomes the sub-package under which `domain/`, `application/`, `infrastructure/` live.

### Phase 1: Rename identity

| # | File | Action |
|---|---|---|
| 1 | `settings.gradle.kts` | Replace `rootProject.name = "reytech"` with the new project name |
| 2 | `build.gradle.kts` | Replace `group = "de.reytech"` with the new group (everything before the leaf package); replace the `imageName` prefix (`ghcr.io/reytech-dev/java-graphql-blueprint`) with the new registry path; reset `version` to `"0.1.0-SNAPSHOT"` |
| 3 | `src/main/java/de/reytech/reytech/` | Rename the directory tree to `src/main/java/<new-group-slash-separated>/<leaf-pkg>/` |
| 4 | `src/test/java/de/reytech/reytech/` | Same — move to `src/test/java/<new-group-slash-separated>/<leaf-pkg>/` |
| 5 | All `.java` files | Replace `package de.reytech.reytech...` with `package <new-group>.<leaf-pkg>...`; update all cross-file imports |
| 6 | `Application.java` | Rename the class (e.g. `MyApiApplication.java`). Keep the package declaration and `@SpringBootApplication` annotation. |
| 7 | `AGENTS.md` | Replace all occurrences of `reytech` / `de.reytech.reytech` with the new project name and package; update the image tag and `rootProject.name` reference in prose. Keep the boilerplate structure documentation. |
| 8 | `.github/workflows/ci.yml` | Update job names and any path/image references to the new project |
| 9 | `.github/workflows/release.yml` | Update the `prepareCmd` in `.releaserc.yml` — replace the image path with the new registry path. **Do not** strip `--publishImage`; keep it so the release workflow pushes automatically. |

### Phase 2: Strip boilerplate example

Delete the entire `Book` example domain:

| # | File | Action |
|---|---|---|
| 10 | `src/main/java/<pkg>/book/` | Delete the entire `book/` directory (4 files: `Book.java`, `BookService.java`, `BookRepository.java`, `BookGraphqlController.java`) |
| 11 | `src/test/java/<pkg>/book/` | Delete `book/application/BookServiceTest.java` |
| 12 | `src/main/resources/db/migration/V1__create_book_table.sql` | Delete |
| 13 | `src/main/resources/graphql/schema.graphqls` | Replace with a minimal scaffold. A GraphQL schema must have at least a root `Query` type: `type Query { _empty: String }` |
| 14 | `src/test/java/<pkg>/ApplicationTests.java` | Strip to only the `actuatorHealthReturnsUp()` test and the `@BeforeEach` setup (keep the two-`WebTestClient` pattern with root + `/graphql` base URLs). Remove all Book-specific tests, `@Order` annotations, `@TestMethodOrder`, and graphQlTester field entirely (not needed with only the actuator health test). |

### Phase 3: Cleanup residue

| # | File | Action |
|---|---|---|
| 15 | `build/` | Delete the entire directory (stale compiled classes with old package) |
| 16 | `application.yml` | **Keep as-is.** The default dev credentials (`postgres` / `backend` / `backend`) work out-of-the-box with the companion `docker-compose.yaml`. Users can adjust later. |

### Verify

```bash
./gradlew test
```

The single smoke test should pass, confirming the scaffold compiles and the application context loads cleanly. The project is now ready for new domain code.

### Post-initialization skeleton

```
src/main/java/<pkg>/
└── Application.java              # renamed entry point

src/main/resources/
├── application.yml               # unchanged dev defaults
├── graphql/
│   └── schema.graphqls           # empty scaffold (type Query { _empty: String })
└── db/migration/                 # empty (no migrations yet)

src/test/java/<pkg>/
└── ApplicationTests.java         # smoke test only (actuator health)
```

## Project Structure (DDD)

```
src/main/java/de/reytech/reytech/
├── Application.java                         # @SpringBootApplication entry point
└── book/
    ├── domain/
    │   └── Book.java                        # Domain entity (record)
    ├── application/
    │   └── BookService.java                 # Application service (use cases)
    └── infrastructure/
        ├── BookRepository.java              # ReactiveCrudRepository (persistence adapter)
        └── BookGraphqlController.java       # @Controller with @QueryMapping/@MutationMapping

src/main/resources/
├── application.yml                          # App configuration
├── graphql/
│   └── schema.graphqls                      # GraphQL schema (source of truth)
└── db/migration/
    └── V1__create_book_table.sql            # Flyway migrations

src/test/java/de/reytech/reytech/
├── ApplicationTests.java                    # Integration tests (real DB via Testcontainers)
└── book/application/
    └── BookServiceTest.java                 # Unit tests (mocked dependencies)
```

### DDD Layer Responsibilities

| Layer | Purpose | Rules |
|---|---|---|
| **domain** | Entity records, value objects, domain logic | No framework annotations except `@Id`/`@Table`. Pure Java. |
| **application** | Use cases, orchestration, service classes | Depends on domain + repository interfaces. No GraphQL/schema concerns. |
| **infrastructure** | Adapters — GraphQL controllers, repositories, external integrations | Bridges framework to application layer. Contains `@Controller`, `@Repository`. |

### DDD Rule
Dependencies always flow inward: `infrastructure → application → domain`. Domain never depends on anything outside itself.

### Supporting Files

| File | Purpose |
|---|---|
| `commitlint.config.cjs` | Enforces conventional commits with mandatory scope |
| `.releaserc.yml` | semantic-release config (versioning, changelog, Docker publish command) |
| `settings.gradle.kts` | Root project name (`reytech`) |
| `package.json` / `package-lock.json` | Commitlint + semantic-release npm dependencies |

## TDD Workflow

**Always follow red-green-refactor:**

1. **Red** — Write a failing test first
2. **Green** — Write minimal code to pass the test
3. **Refactor** — Clean up without changing behavior

### Test Location Conventions

- **Unit tests** (`*Test.java`): Test a single class in isolation. Mock dependencies. Place in the same package path under `src/test/` as the class being tested.
  - Example: `src/main/.../book/application/BookService.java` → `src/test/.../book/application/BookServiceTest.java`
- **Integration tests** (`ApplicationTests.java`): Test the full stack (HTTP, GraphQL, DB). Use `@SpringBootTest` + Testcontainers.

### Test Dependencies

- `spring-boot-starter-graphql-test` — `HttpGraphQlTester` for GraphQL integration testing
- `spring-boot-starter-webflux-test` — `WebTestClient` for reactive HTTP testing
- `spring-boot-starter-actuator-test` — Actuator test assertions
- `spring-boot-testcontainers` — `@ServiceConnection` auto-configures test DB
- `testcontainers-junit-jupiter`, `testcontainers-postgresql`, `testcontainers-r2dbc` — PostgreSQL via Testcontainers
- `junit-platform-launcher` — JUnit 5 Platform (test runtime)
- Project Reactor `StepVerifier` — for asserting reactive streams in unit tests (included via starters)

## Adding a New GraphQL Feature

### Step-by-step (TDD order):

1. **Define the GraphQL schema** in `src/main/resources/graphql/schema.graphqls`
2. **Create the domain entity** in `src/main/java/de/reytech/reytech/<feature>/domain/`
3. **Write the Flyway migration** in `src/main/resources/db/migration/` (format: `V<number>__<description>.sql`)
4. **Create the R2DBC repository** in `src/main/java/de/reytech/reytech/<feature>/infrastructure/`
5. **Write unit tests for the service** (TDD: failing first)
6. **Create the application service** in `src/main/java/de/reytech/reytech/<feature>/application/`
7. **Create the GraphQL controller** in `src/main/java/de/reytech/reytech/<feature>/infrastructure/`
8. **Write integration tests** in `src/test/java/de/reytech/reytech/ApplicationTests.java`

### Controller Annotation Patterns

```java
@QueryMapping
public Flux<Entity> entities() { ... }              // Query: returns list

@QueryMapping
public Mono<Entity> entityById(@Argument Long id)   // Query: returns single by argument

@MutationMapping
public Mono<Entity> createEntity(@Argument String name)  // Mutation: creates
```

### Repository Pattern

```java
@Repository
public interface EntityRepository extends ReactiveCrudRepository<Entity, Long> {
    Flux<Entity> findBySomeField(String value);  // Custom query methods
}
```

## Flyway Migrations

- Location: `src/main/resources/db/migration/`
- Naming: `V<version>__<description>.sql` (double underscore after version)
- Versions are sequential integers: `V1`, `V2`, `V3`, ...
- Use `SERIAL PRIMARY KEY` for auto-incrementing IDs (PostgreSQL)
- Default schema: `public` (no custom Flyway schemas configured)

## Configuration

`src/main/resources/application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://<host>:5432/<database>
    username: <user>
    password: <password>
  flyway:
    url: jdbc:postgresql://<host>:5432/<database>
    user: <user>
    password: <password>
  graphql:
    graphiql:
      enabled: true        # GraphiQL playground at /graphiql

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

- Default dev credentials: host `postgres`, db `backend`, user/pass `backend/backend` (the actual `application.yml` file contains these concrete values, not placeholders)
- GraphQL endpoint: `POST /graphql`
- GraphiQL: `http://localhost:8080/graphiql`
- Actuator health: `GET /actuator/health`

## Testing Patterns

### Integration Test (GraphQL + DB)

See the reference implementation in `src/test/java/de/reytech/reytech/ApplicationTests.java`.

Key conventions:

- `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` — starts full application on a random port
- `@Testcontainers` + `@Container` + `@ServiceConnection` — auto-wires R2DBC and JDBC to a PostgreSQL Testcontainers instance; Flyway migrations run automatically on startup
- `@LocalServerPort` injects the random port for manual `WebTestClient` setup
- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` — ensures ordered test execution (health check before GraphQL tests)
- **Two separate `WebTestClient` instances** are needed in `@BeforeEach`:
  - One at the root base URL (`localhost:PORT`) for actuator and HTTP endpoints
  - One at `/graphql` base URL (`localhost:PORT/graphql`) for `HttpGraphQlTester.create()`
- Include an actuator health check test (`GET /actuator/health`) to verify the application context loads correctly
- Use `HttpGraphQlTester.create(webTestClient)` (not `@AutoConfigureHttpGraphQlTester`, which is unavailable in Spring GraphQL 2.x)
- Assert empty GraphQL results with `.entityList(Object.class).hasSize(0)`

> **Important:** `HttpGraphQlTester.create()` in Spring GraphQL 2.x does **not** auto-append `/graphql` to requests. The WebTestClient base URL must include `/graphql` explicitly.

### Unit Test (Service with Mocks)

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyService service;

    @Test
    void myTest() {
        when(repository.findAll()).thenReturn(Flux.just(entity));
        StepVerifier.create(service.findAll())
                .expectNext(entity)
                .verifyComplete();
    }
}
```

## Docker

Build the image:
```bash
./gradlew bootBuildImage
```

The image is tagged as `ghcr.io/reytech-dev/java-graphql-blueprint:<version>`. Uses Paketo builder `paketobuildpacks/builder-noble-java-tiny:latest` with Java 25.

Publishing requires `REGISTRY_USERNAME` and `REGISTRY_PASSWORD` (or `REGISTRY_TOKEN`) environment variables for Gradle's `docker.publishRegistry` configuration. The build file sets `publish.set(false)` by default; use the `--publishImage` Gradle flag to push.

Additional Gradle config in `build.gradle.kts`:
- `springBoot { buildInfo() }` — generates `META-INF/build-info.properties`, exposed via `/actuator/info`
- `bootJar { layered { enabled.set(true) } }` — layered JAR for Docker layer caching

## CI/CD

- **CI** (`.github/workflows/ci.yml`): Runs `./gradlew test` on PRs and pushes to main. Spins up a PostgreSQL 16 service container. Uses Java 21 to bootstrap Gradle; the Java 25 toolchain is auto-provisioned by Gradle.
- **Release** (`.github/workflows/release.yml`): three jobs:
  - `commitlint` — validates all commits follow conventional commit format
  - `semantic-release-dry-run` — previews version bump on non-main branches
  - `release` — on main: calculates next version via semantic-release, builds and publishes the Docker image via `./gradlew clean bootBuildImage --publishImage -Pversion="v<version>"`, and pushes to `ghcr.io/reytech-dev/java-graphql-blueprint`. Requires `packages: write` repository permission. The Gradle `bootBuildImage` task uses its own `docker.publishRegistry` config (in `build.gradle.kts`) which reads `REGISTRY_USERNAME` and `REGISTRY_PASSWORD` environment variables — these must be set alongside the `docker/login-action` step.

## Code Style Conventions

- **No comments** in source code unless explicitly requested. Let the code speak through clear naming.
- **Records** for domain entities (immutable by default).
- **Constructor injection** (no `@Autowired` on fields).
- **Reactive types**: `Mono` for single values, `Flux` for collections.
- Follow existing naming conventions and file organization patterns.
- Do not introduce new libraries or frameworks without checking the existing dependency list first.
- Commit messages must follow [Conventional Commits](https://www.conventionalcommits.org/): `type(scope): description` where type is one of `feat`, `fix`, `test`, `refactor`, `docs`, `perf`, `chore`. The scope is **mandatory** (enforced by `commitlint.config.cjs` with `scope-empty: never`).
