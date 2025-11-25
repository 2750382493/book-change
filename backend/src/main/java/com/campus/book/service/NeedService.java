package com.campus.book.service;

import com.campus.book.model.Need;
import com.campus.book.repository.NeedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NeedService {
    @Autowired
    private NeedRepository needRepository;
    
    public Need saveNeed(Need need) {
        return needRepository.save(need);
    }
    
    public List<Need> getAllActiveNeeds() {
        return needRepository.findByFulfilledFalse();
    }
    
    public List<Need> searchNeeds(String keyword) {
        return needRepository.searchNeeds(keyword);
    }
    
    public List<Need> getUserNeeds(String userId) {
        return needRepository.findByUserId(userId);
    }
    
    public Optional<Need> getNeedById(String id) {
        return needRepository.findById(id);
    }
    
    public Need markAsFulfilled(String id) {
        Optional<Need> needOpt = needRepository.findById(id);
        if (needOpt.isPresent()) {
            Need need = needOpt.get();
            need.setFulfilled(true);
            return needRepository.save(need);
        }
        throw new RuntimeException("需求不存在");
    }
    
    public void deleteNeed(String id) {
        needRepository.deleteById(id);
    }
}