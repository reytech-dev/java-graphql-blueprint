package de.reytech.reytech.book.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("book")
public record Book(
        @Id Long id,
        String title,
        String author,
        Integer publishedYear) {
}
