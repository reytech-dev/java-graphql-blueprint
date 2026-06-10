package de.reytech.reytech.book.application;

import de.reytech.reytech.book.domain.Book;
import de.reytech.reytech.book.infrastructure.BookRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Flux<Book> findAll() {
        return bookRepository.findAll();
    }

    public Mono<Book> create(String title, String author, Integer publishedYear) {
        Book book = new Book(null, title, author, publishedYear);
        return bookRepository.save(book);
    }
}
