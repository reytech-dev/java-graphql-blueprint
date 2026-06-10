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

- `spring-boot-starter-test` — JUnit 5, Mockito included
- `spring-boot-starter-graphql-test` — `HttpGraphQlTester` for GraphQL integration testing
- `spring-boot-testcontainers` — `@ServiceConnection` auto-configures test DB
- Project Reactor `StepVerifier` — for asserting reactive streams in unit tests

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

- Default dev credentials: host `postgres`, db `backend`, user/pass `backend/backend`
- GraphQL endpoint: `POST /graphql`
- GraphiQL: `http://localhost:8080/graphiql`
- Actuator health: `GET /actuator/health`

## Testing Patterns

### Integration Test (GraphQL + DB)

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@Testcontainers
class ApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private HttpGraphQlTester graphQlTester;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void myQueryTest() {
        graphQlTester.document("query { books { id } }")
                .execute()
                .path("books")
                .matchesJson("[]");
    }
}
```

`@ServiceConnection` automatically configures both R2DBC and JDBC connections to point at the Testcontainers PostgreSQL instance. Flyway migrations run automatically on startup.

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

## CI/CD

- **CI** (`.github/workflows/ci.yml`): Runs `./gradlew test` on PRs and pushes to main. Spins up a PostgreSQL service container.
- **Release** (`.github/workflows/release.yml`): semantic-release with conventional commits. On merge to main, auto-versions, generates changelog, and publishes Docker image to GitHub Container Registry.

## Code Style Conventions

- **No comments** in source code unless explicitly requested. Let the code speak through clear naming.
- **Records** for domain entities (immutable by default).
- **Constructor injection** (no `@Autowired` on fields).
- **Reactive types**: `Mono` for single values, `Flux` for collections.
- Follow existing naming conventions and file organization patterns.
- Do not introduce new libraries or frameworks without checking the existing dependency list first.
- Commit messages must follow [Conventional Commits](https://www.conventionalcommits.org/): `type(scope): description` where type is one of `feat`, `fix`, `test`, `refactor`, `docs`, `perf`, `chore`.
