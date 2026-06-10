package de.reytech.reytech;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;
    private HttpGraphQlTester graphQlTester;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        graphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:" + port + "/graphql")
                        .build());
    }

    @Test
    @Order(1)
    void actuatorHealthReturnsUp() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @Order(2)
    void booksQueryReturnsEmptyListWhenNoBooks() {
        graphQlTester.document("""
                        query {
                            books {
                                id
                                title
                                author
                                publishedYear
                            }
                        }
                        """)
                .execute()
                .path("books")
                .entityList(Object.class)
                .hasSize(0);
    }

    @Test
    @Order(3)
    void createBookMutationCreatesAndReturnsBook() {
        graphQlTester.document("""
                        mutation {
                            createBook(title: "Clean Code", author: "Robert C. Martin", publishedYear: 2008) {
                                id
                                title
                                author
                                publishedYear
                            }
                        }
                        """)
                .execute()
                .path("createBook.title")
                .entity(String.class)
                .isEqualTo("Clean Code");
    }

    @Test
    @Order(4)
    void booksQueryReturnsCreatedBook() {
        graphQlTester.document("""
                        query {
                            books {
                                id
                                title
                                author
                                publishedYear
                            }
                        }
                        """)
                .execute()
                .path("books")
                .entityList(Object.class)
                .hasSize(1);
    }
}
