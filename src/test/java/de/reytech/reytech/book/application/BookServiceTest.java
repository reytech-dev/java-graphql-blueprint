package de.reytech.reytech.book.application;

import de.reytech.reytech.book.domain.Book;
import de.reytech.reytech.book.infrastructure.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void findAllReturnsBooksFromRepository() {
        Book book = new Book(1L, "Clean Code", "Robert C. Martin", 2008);
        when(bookRepository.findAll()).thenReturn(Flux.just(book));

        Flux<Book> result = bookService.findAll();

        StepVerifier.create(result)
                .expectNext(book)
                .verifyComplete();
    }

    @Test
    void createSavesAndReturnsBook() {
        Book saved = new Book(1L, "Clean Code", "Robert C. Martin", 2008);
        when(bookRepository.save(any(Book.class))).thenReturn(Mono.just(saved));

        Mono<Book> result = bookService.create("Clean Code", "Robert C. Martin", 2008);

        StepVerifier.create(result)
                .expectNext(saved)
                .verifyComplete();
    }
}
