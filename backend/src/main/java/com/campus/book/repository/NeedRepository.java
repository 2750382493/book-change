package com.campus.book.repository;

import com.campus.book.model.Need;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface NeedRepository extends MongoRepository<Need, String> {
    List<Need> findByUserId(String userId);
    List<Need> findByFulfilledFalse();
    
    @Query("{'$and': ["
        + "{'fulfilled': false}, "
        + "{'$or': ["
        + "  {'title': {$regex: ?0, $options: 'i'}}, "
        + "  {'author': {$regex: ?0, $options: 'i'}}, "
        + "  {'description': {$regex: ?0, $options: 'i'}}"
        + "]}]}")
    List<Need> searchNeeds(String keyword);
}