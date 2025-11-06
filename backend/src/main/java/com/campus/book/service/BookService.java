package com.campus.book.service;

import com.campus.book.model.Book;
import com.campus.book.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {
    @Autowired
    private BookRepository bookRepository;
    
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }
    
    public List<Book> getAllAvailableBooks() {
        return bookRepository.findBySoldFalse();
    }
    
    public List<Book> searchBooks(String keyword) {
        return bookRepository.searchBooks(keyword);
    }
    
    public List<Book> getBooksByCategory(String category) {
        return bookRepository.findByCategoryAndSoldFalse(category);
    }
    
    public List<Book> getUserBooks(String userId) {
        return bookRepository.findBySellerIdAndSoldFalse(userId);
    }
    
    public Optional<Book> getBookById(String id) {
        return bookRepository.findById(id);
    }
    
    public Book markAsSold(String id) {
        Optional<Book> bookOpt = bookRepository.findById(id);
        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();
            book.setSold(true);
            return bookRepository.save(book);
        }
        throw new RuntimeException("书籍不存在");
    }
}