package com.campus.book.repository;

import com.campus.book.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByPhone(String phone);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    List<User> findByStatus(Integer status);
    
    Page<User> findByStatus(Integer status, Pageable pageable);
    
    List<User> findByEmailVerified(Boolean emailVerified);
    
    List<User> findByRolesContaining(String role);
    
    long countByStatus(Integer status);
    
    long countByEmailVerified(Boolean emailVerified);
}