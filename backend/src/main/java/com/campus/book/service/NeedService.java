package com.campus.book.service;

import com.campus.book.model.Book;
import com.campus.book.model.Need;
import com.campus.book.model.Need.NeedStatus;
import com.campus.book.model.Need.Priority;
import com.campus.book.repository.NeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NeedService {

    private final NeedRepository needRepository;
    private final MongoTemplate mongoTemplate;
    private final BookService bookService;
    private final NotificationService notificationService;

    /**
     * 保存或更新需求
     */
    @Transactional
    public Need saveNeed(Need need) {
        if (need == null) {
            throw new IllegalArgumentException("需求不能为空");
        }

        try {
            // 验证需求有效性
            if (!need.isValid()) {
                throw new IllegalArgumentException("需求信息不完整或无效");
            }

            // 设置更新时间
            need.setUpdateTime(new Date());

            // 如果需求已过期，自动更新状态
            need.updateExpiryStatus();

            // 保存需求
            Need savedNeed = needRepository.save(need);
            log.info("需求保存成功，ID: {}, 标题: {}, 用户: {}",
                    savedNeed.getId(), savedNeed.getTitle(), savedNeed.getUser().getId());

            return savedNeed;
        } catch (Exception e) {
            log.error("保存需求失败，标题: {}, 用户: {}",
                    need.getTitle(), need.getUser() != null ? need.getUser().getId() : "未知", e);
            throw new RuntimeException("保存需求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量保存需求
     */
    @Transactional
    public List<Need> saveAllNeeds(List<Need> needs) {
        if (CollectionUtils.isEmpty(needs)) {
            return Collections.emptyList();
        }

        needs.forEach(need -> {
            need.setUpdateTime(new Date());
            need.updateExpiryStatus();
        });

        try {
            return needRepository.saveAll(needs);
        } catch (Exception e) {
            log.error("批量保存需求失败，数量: {}", needs.size(), e);
            throw new RuntimeException("批量保存需求失败", e);
        }
    }

    /**
     * 根据ID获取需求
     */
    public Optional<Need> getNeedById(String id) {
        if (!StringUtils.hasText(id)) {
            return Optional.empty();
        }

        try {
            Optional<Need> needOpt = needRepository.findById(id);
            needOpt.ifPresent(this::incrementViewCount);
            return needOpt;
        } catch (Exception e) {
            log.error("获取需求失败，ID: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 根据业务ID获取需求
     */
    public Optional<Need> getNeedByNeedId(String needId) {
        return needRepository.findByNeedId(needId);
    }

    /**
     * 获取所有活跃需求
     */
    public List<Need> getAllActiveNeeds() {
        try {
            return needRepository.findByStatus(NeedStatus.ACTIVE);
        } catch (Exception e) {
            log.error("获取活跃需求失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取活跃需求（分页）
     */
    public Page<Need> getActiveNeeds(Pageable pageable) {
        return needRepository.findByStatus(NeedStatus.ACTIVE, pageable);
    }

    /**
     * 获取未满足的需求
     */
    public List<Need> getUnfulfilledNeeds() {
        try {
            return needRepository.findByFulfilledFalse();
        } catch (Exception e) {
            log.error("获取未满足需求失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 搜索需求
     */
    public List<Need> searchNeeds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        try {
            return needRepository.searchNeeds(keyword);
        } catch (Exception e) {
            log.error("搜索需求失败，关键字: {}", keyword, e);
            return Collections.emptyList();
        }
    }

    /**
     * 高级搜索需求
     */
    public List<Need> advancedSearch(String title, String author, String category,
                                     Double minPrice, Double maxPrice, NeedStatus status) {

        Query query = new Query();

        if (StringUtils.hasText(title)) {
            query.addCriteria(Criteria.where("title").regex(title, "i"));
        }

        if (StringUtils.hasText(author)) {
            query.addCriteria(Criteria.where("author").regex(author, "i"));
        }

        if (StringUtils.hasText(category)) {
            query.addCriteria(Criteria.where("category").is(category));
        }

        if (minPrice != null) {
            query.addCriteria(Criteria.where("maxPrice").gte(minPrice));
        }

        if (maxPrice != null) {
            query.addCriteria(Criteria.where("maxPrice").lte(maxPrice));
        }

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        query.addCriteria(Criteria.where("deleted").is(false));

        try {
            return mongoTemplate.find(query, Need.class);
        } catch (Exception e) {
            log.error("高级搜索需求失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户的需求列表
     */
    public List<Need> getUserNeeds(String userId) {
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }

        try {
            return needRepository.findByUserId(userId);
        } catch (Exception e) {
            log.error("获取用户需求失败，用户ID: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户的需求列表（按状态筛选）
     */
    public List<Need> getUserNeedsByStatus(String userId, NeedStatus status) {
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }

        try {
            return needRepository.findByUserIdAndStatus(userId, status);
        } catch (Exception e) {
            log.error("获取用户需求失败，用户ID: {}, 状态: {}", userId, status, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户的需求数量
     */
    public long countUserNeeds(String userId) {
        if (!StringUtils.hasText(userId)) {
            return 0;
        }

        try {
            return needRepository.countByUserId(userId);
        } catch (Exception e) {
            log.error("统计用户需求数量失败，用户ID: {}", userId, e);
            return 0;
        }
    }

    /**
     * 标记需求为已满足
     */
    @Transactional
    public Need markAsFulfilled(String id) {
        return updateNeedStatus(id, NeedStatus.FULFILLED, null);
    }

    /**
     * 标记需求为已匹配
     */
    @Transactional
    public Need markAsMatched(String needId, String bookId, Integer matchScore) {
        Optional<Need> needOpt = getNeedById(needId);
        if (!needOpt.isPresent()) {
            throw new RuntimeException("需求不存在，ID: " + needId);
        }

        Optional<Book> bookOpt = bookService.getBookById(bookId);
        if (!bookOpt.isPresent()) {
            throw new RuntimeException("书籍不存在，ID: " + bookId);
        }

        Need need = needOpt.get();
        Book book = bookOpt.get();

        // 标记为已匹配
        need.markAsMatched(book, matchScore);

        // 保存更新
        Need updatedNeed = needRepository.save(need);

        // 发送通知
        notificationService.notifyNeedMatched(updatedNeed, book);

        log.info("需求标记为已匹配，需求ID: {}, 书籍ID: {}, 匹配分数: {}",
                needId, bookId, matchScore);

        return updatedNeed;
    }

    /**
     * 取消需求
     */
    @Transactional
    public Need cancelNeed(String id, String reason) {
        Optional<Need> needOpt = getNeedById(id);
        if (!needOpt.isPresent()) {
            throw new RuntimeException("需求不存在，ID: " + id);
        }

        Need need = needOpt.get();
        need.cancel(reason);

        Need updatedNeed = needRepository.save(need);

        // 发送通知
        notificationService.notifyNeedCancelled(updatedNeed);

        log.info("需求已取消，ID: {}, 原因: {}", id, reason);

        return updatedNeed;
    }

    /**
     * 重新激活需求
     */
    @Transactional
    public Need reactivateNeed(String id) {
        Optional<Need> needOpt = getNeedById(id);
        if (!needOpt.isPresent()) {
            throw new RuntimeException("需求不存在，ID: " + id);
        }

        Need need = needOpt.get();

        // 检查是否可以被重新激活
        if (need.getStatus() != NeedStatus.CANCELLED &&
                need.getStatus() != NeedStatus.EXPIRED) {
            throw new IllegalStateException("只有已取消或已过期的需求可以重新激活");
        }

        need.reactivate();

        // 重置过期时间（默认30天）
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        need.setExpiryTime(calendar.getTime());

        Need updatedNeed = needRepository.save(need);

        // 发送通知
        notificationService.notifyNeedReactivated(updatedNeed);

        log.info("需求已重新激活，ID: {}", id);

        return updatedNeed;
    }

    /**
     * 更新需求状态
     */
    @Transactional
    public Need updateNeedStatus(String id, NeedStatus newStatus, String reason) {
        Optional<Need> needOpt = getNeedById(id);
        if (!needOpt.isPresent()) {
            throw new RuntimeException("需求不存在，ID: " + id);
        }

        Need need = needOpt.get();
        NeedStatus oldStatus = need.getStatus();

        // 验证状态转换是否有效
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new IllegalStateException("无效的状态转换: " + oldStatus + " -> " + newStatus);
        }

        need.setStatus(newStatus);

        // 如果标记为已满足，设置满足时间
        if (newStatus == NeedStatus.FULFILLED) {
            need.setFulfillTime(new Date());
            need.markAsFulfilled();
        }

        // 添加标签记录状态变更原因
        if (StringUtils.hasText(reason)) {
            need.addTag("status-change:" + newStatus.name().toLowerCase() + ":" + reason);
        }

        Need updatedNeed = needRepository.save(need);

        // 发送状态变更通知
        notificationService.notifyNeedStatusChanged(updatedNeed, oldStatus, newStatus);

        log.info("需求状态已更新，ID: {}, 状态: {} -> {}, 原因: {}",
                id, oldStatus, newStatus, reason);

        return updatedNeed;
    }

    /**
     * 验证状态转换是否有效
     */
    private boolean isValidStatusTransition(NeedStatus from, NeedStatus to) {
        // 允许的状态转换规则
        Map<NeedStatus, Set<NeedStatus>> allowedTransitions = new HashMap<>();

        allowedTransitions.put(NeedStatus.DRAFT, EnumSet.of(
                NeedStatus.ACTIVE, NeedStatus.CANCELLED
        ));

        allowedTransitions.put(NeedStatus.ACTIVE, EnumSet.of(
                NeedStatus.MATCHED, NeedStatus.NEGOTIATING, NeedStatus.PENDING,
                NeedStatus.FULFILLED, NeedStatus.EXPIRED, NeedStatus.CANCELLED,
                NeedStatus.ARCHIVED
        ));

        allowedTransitions.put(NeedStatus.MATCHED, EnumSet.of(
                NeedStatus.NEGOTIATING, NeedStatus.PENDING, NeedStatus.FULFILLED,
                NeedStatus.CANCELLED, NeedStatus.ACTIVE
        ));

        allowedTransitions.put(NeedStatus.NEGOTIATING, EnumSet.of(
                NeedStatus.PENDING, NeedStatus.FULFILLED, NeedStatus.CANCELLED,
                NeedStatus.ACTIVE
        ));

        allowedTransitions.put(NeedStatus.PENDING, EnumSet.of(
                NeedStatus.FULFILLED, NeedStatus.CANCELLED, NeedStatus.ACTIVE
        ));

        // 已满足、已过期、已取消、已归档的状态通常是终态
        Set<NeedStatus> finalStates = EnumSet.of(
                NeedStatus.FULFILLED, NeedStatus.EXPIRED,
                NeedStatus.CANCELLED, NeedStatus.ARCHIVED
        );

        // 终态不能转换到其他状态（除了重新激活的特殊情况）
        if (finalStates.contains(from) && from != NeedStatus.CANCELLED && from != NeedStatus.EXPIRED) {
            return false;
        }

        Set<NeedStatus> allowed = allowedTransitions.getOrDefault(from, Collections.emptySet());
        return allowed.contains(to);
    }

    /**
     * 删除需求（逻辑删除）
     */
    @Transactional
    public void deleteNeed(String id) {
        Optional<Need> needOpt = getNeedById(id);
        if (!needOpt.isPresent()) {
            throw new RuntimeException("需求不存在，ID: " + id);
        }

        Need need = needOpt.get();

        // 标记为删除，而不是物理删除
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update().set("deleted", true).set("updateTime", new Date());

        mongoTemplate.updateFirst(query, update, Need.class);

        log.info("需求已标记为删除，ID: {}", id);
    }

    /**
     * 批量删除需求
     */
    @Transactional
    public int batchDeleteNeeds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }

        Query query = new Query(Criteria.where("_id").in(ids));
        Update update = new Update().set("deleted", true).set("updateTime", new Date());

        try {
            int updatedCount = mongoTemplate.updateMulti(query, update, Need.class).getModifiedCount();
            log.info("批量删除需求完成，数量: {}", updatedCount);
            return updatedCount;
        } catch (Exception e) {
            log.error("批量删除需求失败，ID列表: {}", ids, e);
            return 0;
        }
    }

    /**
     * 获取匹配的书籍
     */
    public List<Book> findMatchingBooks(String needId, int threshold) {
        Optional<Need> needOpt = getNeedById(needId);
        if (!needOpt.isPresent()) {
            return Collections.emptyList();
        }

        Need need = needOpt.get();

        // 获取所有活跃的书籍
        List<Book> activeBooks = bookService.getActiveBooks();

        // 过滤匹配的书籍
        return activeBooks.stream()
                .filter(book -> need.isMatchedWith(book, threshold))
                .sorted((b1, b2) -> {
                    int score1 = need.calculateMatchScore(b1);
                    int score2 = need.calculateMatchScore(b2);
                    return Integer.compare(score2, score1); // 降序排序
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算需求与书籍的匹配度
     */
    public Integer calculateMatchScore(String needId, String bookId) {
        Optional<Need> needOpt = getNeedById(needId);
        Optional<Book> bookOpt = bookService.getBookById(bookId);

        if (!needOpt.isPresent() || !bookOpt.isPresent()) {
            return 0;
        }

        return needOpt.get().calculateMatchScore(bookOpt.get());
    }

    /**
     * 增加需求浏览次数
     */
    @Transactional
    public void incrementViewCount(String needId) {
        Query query = new Query(Criteria.where("_id").is(needId));
        Update update = new Update().inc("viewCount", 1).set("updateTime", new Date());

        mongoTemplate.updateFirst(query, update, Need.class);
    }

    /**
     * 增加需求收藏次数
     */
    @Transactional
    public void incrementFavoriteCount(String needId) {
        Query query = new Query(Criteria.where("_id").is(needId));
        Update update = new Update().inc("favoriteCount", 1).set("updateTime", new Date());

        mongoTemplate.updateFirst(query, update, Need.class);
    }

    /**
     * 增加需求报价次数
     */
    @Transactional
    public void incrementOfferCount(String needId) {
        Query query = new Query(Criteria.where("_id").is(needId));
        Update update = new Update().inc("offerCount", 1).set("updateTime", new Date());

        mongoTemplate.updateFirst(query, update, Need.class);
    }

    /**
     * 更新需求价格
     */
    @Transactional
    public Need updateNeedPrice(String needId, Double maxPrice, Double minPrice, Boolean negotiable) {
        Optional<Need> needOpt = getNeedById(needId);
        if (!needOpt.isPresent()) {
            throw new RuntimeException("需求不存在，ID: " + needId);
        }

        Need need = needOpt.get();

        if (maxPrice != null) {
            need.setMaxPrice(maxPrice);
        }

        if (minPrice != null) {
            need.setMinPrice(minPrice);
        }

        if (negotiable != null) {
            need.setNegotiable(negotiable);
        }

        Need updatedNeed = needRepository.save(need);

        log.info("需求价格已更新，ID: {}, 最高价: {}, 最低价: {}, 可议价: {}",
                needId, maxPrice, minPrice, negotiable);

        return updatedNeed;
    }

    /**
     * 更新需求优先级
     */
    @Transactional
    public Need updateNeedPriority(String needId, Integer urgency, Priority priority) {
        Optional<Need> needOpt = getNeedById(needId);
        if (!needOpt.isPresent()) {
            throw new RuntimeException("需求不存在，ID: " + needId);
        }

        Need need = needOpt.get();

        if (urgency != null) {
            need.setUrgency(urgency);
        }

        if (priority != null) {
            need.setPriority(priority);
        }

        Need updatedNeed = needRepository.save(need);

        log.info("需求优先级已更新，ID: {}, 紧急度: {}, 优先级: {}",
                needId, urgency, priority);

        return updatedNeed;
    }

    /**
     * 添加需求标签
     */
    @Transactional
    public void addNeedTag(String needId, String tag) {
        if (!StringUtils.hasText(tag)) {
            return;
        }

        Optional<Need> needOpt = getNeedById(needId);
        if (!needOpt.isPresent()) {
            return;
        }

        needOpt.get().addTag(tag);
        needRepository.save(needOpt.get());
    }

    /**
     * 获取热门需求（按浏览量和收藏量排序）
     */
    public List<Need> getPopularNeeds(int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(NeedStatus.ACTIVE));
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.desc("viewCount"),
                org.springframework.data.domain.Sort.Order.desc("favoriteCount")
        ));
        query.limit(limit);

        try {
            return mongoTemplate.find(query, Need.class);
        } catch (Exception e) {
            log.error("获取热门需求失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取紧急需求
     */
    public List<Need> getUrgentNeeds(int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(NeedStatus.ACTIVE));
        query.addCriteria(Criteria.where("urgency").gte(8)); // 紧急度8以上
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.desc("urgency"),
                org.springframework.data.domain.Sort.Order.desc("createTime")
        ));
        query.limit(limit);

        try {
            return mongoTemplate.find(query, Need.class);
        } catch (Exception e) {
            log.error("获取紧急需求失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取即将过期的需求
     */
    public List<Need> getExpiringNeeds(int daysThreshold) {
        Date thresholdDate = Date.from(
                LocalDateTime.now().plusDays(daysThreshold)
                        .atZone(ZoneId.systemDefault()).toInstant()
        );

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(NeedStatus.ACTIVE));
        query.addCriteria(Criteria.where("expiryTime").lte(thresholdDate));
        query.addCriteria(Criteria.where("expiryTime").gt(new Date()));
        query.with(org.springframework.data.domain.Sort.by("expiryTime"));

        try {
            return mongoTemplate.find(query, Need.class);
        } catch (Exception e) {
            log.error("获取即将过期需求失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 批量处理过期需求（定时任务）
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional
    public void processExpiredNeeds() {
        log.info("开始处理过期需求...");

        Date now = new Date();
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(NeedStatus.ACTIVE));
        query.addCriteria(Criteria.where("expiryTime").lt(now));

        List<Need> expiredNeeds = mongoTemplate.find(query, Need.class);

        if (CollectionUtils.isEmpty(expiredNeeds)) {
            log.info("没有发现过期需求");
            return;
        }

        expiredNeeds.forEach(need -> {
            need.setStatus(NeedStatus.EXPIRED);
            need.setUpdateTime(now);
            need.addTag("auto-expired");
        });

        needRepository.saveAll(expiredNeeds);

        log.info("已处理{}个过期需求", expiredNeeds.size());

        // 发送过期通知
        expiredNeeds.forEach(notificationService::notifyNeedExpired);
    }

    /**
     * 获取需求统计信息
     */
    public Map<String, Object> getNeedStatistics(String userId) {
        Map<String, Object> stats = new HashMap<>();

        if (StringUtils.hasText(userId)) {
            // 用户维度的统计
            stats.put("total", countUserNeeds(userId));
            stats.put("active", needRepository.countByUserIdAndStatus(userId, NeedStatus.ACTIVE));
            stats.put("fulfilled", needRepository.countByUserIdAndStatus(userId, NeedStatus.FULFILLED));
            stats.put("expired", needRepository.countByUserIdAndStatus(userId, NeedStatus.EXPIRED));
            stats.put("cancelled", needRepository.countByUserIdAndStatus(userId, NeedStatus.CANCELLED));
        } else {
            // 系统维度的统计
            stats.put("total", needRepository.count());
            stats.put("active", needRepository.countByStatus(NeedStatus.ACTIVE));
            stats.put("fulfilled", needRepository.countByStatus(NeedStatus.FULFILLED));
            stats.put("expired", needRepository.countByStatus(NeedStatus.EXPIRED));
        }

        return stats;
    }

    /**
     * 导出需求数据
     */
    public List<Need> exportNeeds(Date startDate, Date endDate, NeedStatus status) {
        Query query = new Query();

        if (startDate != null) {
            query.addCriteria(Criteria.where("createTime").gte(startDate));
        }

        if (endDate != null) {
            query.addCriteria(Criteria.where("createTime").lte(endDate));
        }

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        query.addCriteria(Criteria.where("deleted").is(false));

        return mongoTemplate.find(query, Need.class);
    }

    /**
     * 获取安全的副本（用于公开API）
     */
    public Optional<Need> getSafeNeedById(String id) {
        return getNeedById(id).map(Need::getSafeCopy);
    }

    private void incrementViewCount(Need need) {
        need.incrementViewCount();
        needRepository.save(need);
    }
}