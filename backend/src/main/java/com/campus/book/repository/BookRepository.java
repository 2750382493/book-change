package com.campus.book.repository;

import com.campus.book.model.Book;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface BookRepository extends MongoRepository<Book, String> {
    List<Book> findBySellerIdAndSoldFalse(String sellerId);
    List<Book> findBySoldFalse();
    
    @Query("{'$and': ["
        + "{'sold': false}, "
        + "{'$or': ["
        + "  {'title': {$regex: ?0, $options: 'i'}}, "
        + "  {'author': {$regex: ?0, $options: 'i'}}, "
        + "  {'description': {$regex: ?0, $options: 'i'}}"
        + "]}]}")
    List<Book> searchBooks(String keyword);
    
    List<Book> findByCategoryAndSoldFalse(String category);
}