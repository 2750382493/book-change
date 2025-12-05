package com.campus.book.service;

import com.campus.book.exception.BookNotFoundException;
import com.campus.book.model.Book;
import com.campus.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    /**
     * 保存书籍信息
     */
    @Transactional
    public Book saveBook(Book book) {
        log.info("保存书籍信息，书名: {}", book.getTitle());
        validateBook(book);
        return bookRepository.save(book);
    }

    /**
     * 获取所有可用的书籍
     */
    public List<Book> getAllAvailableBooks() {
        log.debug("获取所有可用书籍");
        return bookRepository.findBySoldFalse();
    }

    /**
     * 搜索书籍（支持标题、作者、描述等）
     */
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("搜索关键词为空，返回空列表");
            return List.of();
        }

        log.info("搜索书籍，关键词: {}", keyword);
        return bookRepository.searchBooks(keyword.trim());
    }

    /**
     * 按分类获取可用书籍
     */
    public List<Book> getBooksByCategory(String category) {
        validateCategory(category);
        log.debug("按分类获取书籍，分类: {}", category);
        return bookRepository.findByCategoryAndSoldFalse(category);
    }

    /**
     * 获取用户发布的书籍
     */
    public List<Book> getUserBooks(String userId) {
        validateUserId(userId);
        log.debug("获取用户书籍，用户ID: {}", userId);
        return bookRepository.findBySellerIdAndSoldFalse(userId);
    }

    /**
     * 根据ID获取书籍
     */
    public Book getBookById(String id) {
        validateBookId(id);
        log.debug("根据ID获取书籍，ID: {}", id);

        return bookRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("书籍不存在，ID: {}", id);
                    return new BookNotFoundException("书籍不存在，ID: " + id);
                });
    }

    /**
     * 标记书籍为已售出
     */
    @Transactional
    public Book markAsSold(String id) {
        validateBookId(id);
        log.info("标记书籍为已售出，ID: {}", id);

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("标记售出失败，书籍不存在，ID: {}", id);
                    return new BookNotFoundException("书籍不存在，ID: " + id);
                });

        if (book.isSold()) {
            log.warn("书籍已售出，无需重复标记，ID: {}", id);
            return book;
        }

        book.markAsSold();
        Book savedBook = bookRepository.save(book);
        log.info("书籍标记为已售出成功，ID: {}", id);

        return savedBook;
    }

    /**
     * 批量标记为售出
     */
    @Transactional
    public List<Book> batchMarkAsSold(List<String> ids) {
        log.info("批量标记书籍为已售出，数量: {}", ids.size());

        return ids.stream()
                .map(this::markAsSold)
                .toList();
    }

    /**
     * 更新书籍信息
     */
    @Transactional
    public Book updateBook(String id, Book updatedBook) {
        validateBookId(id);
        log.info("更新书籍信息，ID: {}", id);

        Book existingBook = getBookById(id);

        // 只更新允许修改的字段
        existingBook.updateFrom(updatedBook);
        validateBook(existingBook);

        return bookRepository.save(existingBook);
    }

    /**
     * 删除书籍（逻辑删除）
     */
    @Transactional
    public void deleteBook(String id) {
        validateBookId(id);
        log.info("删除书籍，ID: {}", id);

        Book book = getBookById(id);
        book.delete();
        bookRepository.save(book);
    }

    /**
     * 获取热门书籍（按某种规则，如浏览量）
     */
    public List<Book> getPopularBooks(int limit) {
        log.debug("获取热门书籍，数量限制: {}", limit);
        return bookRepository.findPopularBooks(limit);
    }

    // ============ 私有方法 ============

    private void validateBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("书籍不能为null");
        }

        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("书籍标题不能为空");
        }

        if (book.getPrice() == null || book.getPrice().doubleValue() <= 0) {
            throw new IllegalArgumentException("书籍价格必须大于0");
        }
    }

    private void validateBookId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("书籍ID不能为空");
        }
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
    }

    private void validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("分类不能为空");
        }
    }
}