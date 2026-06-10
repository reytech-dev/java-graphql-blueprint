package de.reytech.reytech.book.infrastructure;

import de.reytech.reytech.book.application.BookService;
import de.reytech.reytech.book.domain.Book;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class BookGraphqlController {

    private final BookService bookService;

    public BookGraphqlController(BookService bookService) {
        this.bookService = bookService;
    }

    @QueryMapping
    public Flux<Book> books() {
        return bookService.findAll();
    }

    @MutationMapping
    public Mono<Book> createBook(
            @Argument String title,
            @Argument String author,
            @Argument Integer publishedYear) {
        return bookService.create(title, author, publishedYear);
    }
}
