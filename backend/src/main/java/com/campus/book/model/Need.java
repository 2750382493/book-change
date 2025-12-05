package com.campus.book.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.util.StringUtils;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;

/**
 * 书籍求购需求实体类
 * 与Book类形成供需对称，支持精确匹配与智能推荐
 */
@Document(collection = "needs")
@CompoundIndexes({
        @CompoundIndex(name = "need_search_idx",
                def = "{'title': 'text', 'author': 'text', 'description': 'text'}"),
        @CompoundIndex(name = "need_status_idx",
                def = "{'status': 1, 'createTime': -1}"),
        @CompoundIndex(name = "user_needs_idx",
                def = "{'user.$id': 1, 'status': 1, 'updateTime': -1}"),
        @CompoundIndex(name = "category_price_idx",
                def = "{'category': 1, 'maxPrice': 1, 'status': 1}"),
        @CompoundIndex(name = "isbn_status_idx",
                def = "{'isbn': 1, 'status': 1}",
                unique = false, sparse = true),
        @CompoundIndex(name = "urgency_expiry_idx",
                def = "{'urgency': 1, 'expiryTime': 1, 'status': 1}")
})
public class Need {

    // ========== 核心标识 ==========

    @Id
    private String id;

    @Indexed
    private String needId;  // 业务需求ID，用于去重和外部引用

    // ========== 书籍信息 ==========

    @NotBlank(message = "书籍标题不能为空")
    @TextIndexed
    @Size(max = 200, message = "标题不能超过200字符")
    private String title;

    @Size(max = 100, message = "作者名称不能超过100字符")
    @TextIndexed
    private String author;

    @Indexed(sparse = true)
    @Size(min = 10, max = 13, message = "ISBN格式不正确")
    private String isbn;

    @Size(max = 2000, message = "描述不能超过2000字符")
    @TextIndexed
    private String description;

    // ========== 求购条件 ==========

    @Min(value = 0, message = "最高价格必须大于等于0")
    private Double maxPrice;

    private Double minPrice;  // 最低可接受价格（可选）
    private Boolean negotiable = true;  // 是否可议价

    @Indexed
    @NotBlank(message = "书籍分类不能为空")
    private String category;

    // 书籍状态要求
    private String conditionRequirement = "良好及以上";  // 新旧程度要求
    private Boolean requireOriginal = false;  // 是否要求正版
    private Boolean requireInvoice = false;   // 是否要求发票
    private Boolean requireDelivery = false;  // 是否要求配送

    // ========== 发布者信息 ==========

    @NotNull(message = "发布者不能为空")
    @DBRef
    private User user;

    private String contactMethod = "站内信";  // 联系方式：站内信、微信、电话等
    private String contactInfo;               // 具体联系方式

    // ========== 需求状态 ==========

    @Indexed
    private NeedStatus status = NeedStatus.ACTIVE;

    private Boolean fulfilled = false;  // 是否已满足（兼容字段）

    // 匹配信息
    @DBRef
    private Book matchedBook;           // 匹配到的书籍
    private Date matchTime;             // 匹配时间
    private Integer matchScore;         // 匹配度评分（0-100）

    // 收藏/关注计数
    private Integer favoriteCount = 0;  // 被收藏次数
    private Integer viewCount = 0;      // 浏览次数
    private Integer offerCount = 0;     // 收到的报价次数

    // ========== 时间管理 ==========

    @Indexed
    private Date createTime;
    private Date updateTime;
    private Date expiryTime;            // 需求过期时间
    private Date fulfillTime;           // 满足时间

    // ========== 需求优先级 ==========

    @Indexed
    private Integer urgency = 5;        // 紧急程度 1-10（10最紧急）
    private Priority priority = Priority.NORMAL;  // 优先级

    // 扩展标签
    private Set<String> tags = new HashSet<>();

    // ========== 系统字段 ==========

    @Transient
    private List<Book> matchedBooks;    // 匹配到的书籍列表（查询时填充）

    @Transient
    private Double matchConfidence;     // 匹配置信度

    @Transient
    private Boolean isExpired;          // 是否已过期（计算字段）

    // ========== 枚举定义 ==========

    /**
     * 需求状态枚举
     */
    public enum NeedStatus {
        DRAFT("草稿"),           // 0: 草稿状态
        ACTIVE("活跃"),          // 1: 活跃，可匹配
        MATCHED("已匹配"),       // 2: 已匹配到书籍
        NEGOTIATING("议价中"),   // 3: 正在议价
        PENDING("待确认"),       // 4: 等待确认
        FULFILLED("已满足"),     // 5: 需求已满足
        EXPIRED("已过期"),       // 6: 需求已过期
        CANCELLED("已取消"),     // 7: 用户取消
        ARCHIVED("已归档");      // 8: 已归档

        private final String description;

        NeedStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static NeedStatus fromFulfilled(Boolean fulfilled) {
            return fulfilled != null && fulfilled ? FULFILLED : ACTIVE;
        }
    }

    /**
     * 优先级枚举
     */
    public enum Priority {
        LOW("低"),
        NORMAL("中"),
        HIGH("高"),
        URGENT("紧急");

        private final String description;

        Priority(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========== 内部类：匹配条件 ==========

    /**
     * 匹配条件配置
     */
    public static class MatchCondition {
        private boolean matchByIsbn = true;      // 按ISBN精确匹配
        private boolean matchByTitle = true;     // 按标题模糊匹配
        private boolean matchByAuthor = true;    // 按作者匹配
        private boolean matchByCategory = true;  // 按分类匹配

        private double titleSimilarity = 0.8;    // 标题相似度阈值
        private double authorSimilarity = 0.7;   // 作者相似度阈值
        private double priceRatio = 1.2;         // 价格比例阈值（卖家价格/需求最高价）

        // getters and setters
    }

    // ========== 构造器 ==========

    /**
     * 无参构造器
     */
    public Need() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.needId = UUID.randomUUID().toString();
        this.status = NeedStatus.ACTIVE;
        this.fulfilled = false;
        this.negotiable = true;
        this.urgency = 5;
        this.priority = Priority.NORMAL;

        // 默认30天后过期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        this.expiryTime = calendar.getTime();
    }

    /**
     * 快速创建构造器
     */
    public Need(String title, String category, Double maxPrice, User user) {
        this();
        this.title = title;
        this.category = category;
        this.maxPrice = maxPrice;
        this.user = user;
    }

    /**
     * 完整信息构造器
     */
    public Need(String title, String author, String isbn, String description,
                Double maxPrice, String category, User user,
                Integer urgency, Priority priority) {
        this(title, category, maxPrice, user);
        this.author = author;
        this.isbn = isbn;
        this.description = description;
        this.urgency = urgency != null ? urgency : 5;
        this.priority = priority != null ? priority : Priority.NORMAL;
    }

    // ========== Builder 模式 ==========

    public static NeedBuilder builder() {
        return new NeedBuilder();
    }

    public static class NeedBuilder {
        private Need need;

        public NeedBuilder() {
            this.need = new Need();
        }

        public NeedBuilder title(String title) {
            need.setTitle(title);
            return this;
        }

        public NeedBuilder author(String author) {
            need.setAuthor(author);
            return this;
        }

        public NeedBuilder isbn(String isbn) {
            need.setIsbn(isbn);
            return this;
        }

        public NeedBuilder description(String description) {
            need.setDescription(description);
            return this;
        }

        public NeedBuilder maxPrice(Double maxPrice) {
            need.setMaxPrice(maxPrice);
            return this;
        }

        public NeedBuilder minPrice(Double minPrice) {
            need.setMinPrice(minPrice);
            return this;
        }

        public NeedBuilder negotiable(Boolean negotiable) {
            need.setNegotiable(negotiable);
            return this;
        }

        public NeedBuilder category(String category) {
            need.setCategory(category);
            return this;
        }

        public NeedBuilder user(User user) {
            need.setUser(user);
            return this;
        }

        public NeedBuilder conditionRequirement(String condition) {
            need.setConditionRequirement(condition);
            return this;
        }

        public NeedBuilder requireOriginal(Boolean require) {
            need.setRequireOriginal(require);
            return this;
        }

        public NeedBuilder urgency(Integer urgency) {
            need.setUrgency(urgency);
            return this;
        }

        public NeedBuilder priority(Priority priority) {
            need.setPriority(priority);
            return this;
        }

        public NeedBuilder tag(String tag) {
            need.getTags().add(tag);
            return this;
        }

        public NeedBuilder expiryDays(Integer days) {
            if (days != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, days);
                need.setExpiryTime(calendar.getTime());
            }
            return this;
        }

        public Need build() {
            // 验证必要字段
            if (!StringUtils.hasText(need.getTitle())) {
                throw new IllegalArgumentException("书籍标题不能为空");
            }
            if (need.getUser() == null) {
                throw new IllegalArgumentException("发布者不能为空");
            }
            if (need.getCategory() == null) {
                need.setCategory("其他");
            }

            // 设置默认值
            if (need.getMaxPrice() == null) {
                need.setMaxPrice(0.0);  // 面议
            }
            if (need.getUrgency() == null) {
                need.setUrgency(5);
            }
            if (need.getPriority() == null) {
                need.setPriority(Priority.NORMAL);
            }

            return need;
        }
    }

    // ========== 核心业务方法 ==========

    /**
     * 计算需求匹配度
     * @param book 待匹配的书籍
     * @return 匹配度评分 (0-100)
     */
    public Integer calculateMatchScore(Book book) {
        if (book == null) return 0;

        int score = 0;
        int maxScore = 100;

        // 1. ISBN精确匹配 (30分)
        if (StringUtils.hasText(this.isbn) && StringUtils.hasText(book.getIsbn())) {
            if (this.isbn.equals(book.getIsbn())) {
                score += 30;
            }
        }

        // 2. 标题相似度 (25分)
        if (StringUtils.hasText(this.title) && StringUtils.hasText(book.getTitle())) {
            double similarity = calculateStringSimilarity(this.title, book.getTitle());
            if (similarity > 0.9) score += 25;
            else if (similarity > 0.7) score += 20;
            else if (similarity > 0.5) score += 15;
            else if (similarity > 0.3) score += 10;
        }

        // 3. 作者匹配 (20分)
        if (StringUtils.hasText(this.author) && StringUtils.hasText(book.getAuthor())) {
            double similarity = calculateStringSimilarity(this.author, book.getAuthor());
            if (similarity > 0.8) score += 20;
            else if (similarity > 0.6) score += 15;
            else if (similarity > 0.4) score += 10;
        }

        // 4. 价格匹配 (15分)
        if (this.maxPrice != null && this.maxPrice > 0 && book.getPrice() != null) {
            double priceRatio = book.getPrice() / this.maxPrice;
            if (priceRatio <= 1.0) {
                score += 15;  // 低于或等于期望价格
            } else if (priceRatio <= 1.2) {
                score += 10;  // 价格超出20%以内
            } else if (priceRatio <= 1.5) {
                score += 5;   // 价格超出50%以内
            }
        }

        // 5. 分类匹配 (10分)
        if (StringUtils.hasText(this.category) && StringUtils.hasText(book.getCategory())) {
            if (this.category.equals(book.getCategory())) {
                score += 10;
            }
        }

        return Math.min(score, maxScore);
    }

    /**
     * 检查是否与书籍匹配
     */
    public boolean isMatchedWith(Book book, int threshold) {
        return calculateMatchScore(book) >= threshold;
    }

    /**
     * 标记为已匹配
     */
    public void markAsMatched(Book book, Integer matchScore) {
        if (book != null) {
            this.matchedBook = book;
            this.matchScore = matchScore != null ? matchScore : calculateMatchScore(book);
            this.matchTime = new Date();
            this.status = NeedStatus.MATCHED;
            this.updateTime = new Date();
        }
    }

    /**
     * 标记为已满足
     */
    public void markAsFulfilled() {
        this.fulfilled = true;
        this.status = NeedStatus.FULFILLED;
        this.fulfillTime = new Date();
        this.updateTime = new Date();

        // 如果用户已登录，更新用户统计
        if (this.user != null) {
            user.incrementNeedFulfilledCount();
        }
    }

    /**
     * 取消需求
     * @param reason 取消原因
     */
    public void cancel(String reason) {
        this.status = NeedStatus.CANCELLED;
        this.updateTime = new Date();

        if (reason != null) {
            this.addTag("cancelled:" + reason);
        }
    }

    /**
     * 重新激活需求
     */
    public void reactivate() {
        if (this.status == NeedStatus.CANCELLED || this.status == NeedStatus.EXPIRED) {
            this.status = NeedStatus.ACTIVE;
            this.updateTime = new Date();

            // 重置匹配信息
            this.matchedBook = null;
            this.matchScore = null;
            this.matchTime = null;
        }
    }

    /**
     * 检查需求是否已过期
     */
    public boolean isExpired() {
        if (this.expiryTime == null) return false;
        if (this.status == NeedStatus.FULFILLED ||
                this.status == NeedStatus.CANCELLED) {
            return false;
        }
        return new Date().after(this.expiryTime);
    }

    /**
     * 更新需求过期状态
     */
    public void updateExpiryStatus() {
        if (isExpired() && this.status == NeedStatus.ACTIVE) {
            this.status = NeedStatus.EXPIRED;
            this.updateTime = new Date();
        }
    }

    /**
     * 获取剩余天数
     */
    public Long getRemainingDays() {
        if (this.expiryTime == null) return null;

        long diff = this.expiryTime.getTime() - new Date().getTime();
        return diff > 0 ? diff / (1000 * 60 * 60 * 24) : 0;
    }

    /**
     * 增加浏览次数
     */
    public void incrementViewCount() {
        this.viewCount = (viewCount == null ? 1 : viewCount + 1);
        this.updateTime = new Date();
    }

    /**
     * 增加收藏次数
     */
    public void incrementFavoriteCount() {
        this.favoriteCount = (favoriteCount == null ? 1 : favoriteCount + 1);
        this.updateTime = new Date();
    }

    /**
     * 增加报价次数
     */
    public void incrementOfferCount() {
        this.offerCount = (offerCount == null ? 1 : offerCount + 1);
        this.updateTime = new Date();
    }

    /**
     * 添加标签
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            if (this.tags == null) {
                this.tags = new HashSet<>();
            }
            this.tags.add(tag.trim());
            this.updateTime = new Date();
        }
    }

    /**
     * 移除标签
     */
    public void removeTag(String tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
            this.updateTime = new Date();
        }
    }

    /**
     * 检查是否包含标签
     */
    public boolean hasTag(String tag) {
        return this.tags != null && this.tags.contains(tag);
    }

    /**
     * 获取需求摘要
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(title);

        if (author != null && !author.trim().isEmpty()) {
            summary.append(" - ").append(author);
        }

        if (maxPrice != null && maxPrice > 0) {
            summary.append(" (最高¥").append(String.format("%.2f", maxPrice)).append(")");
        } else {
            summary.append(" (面议)");
        }

        return summary.toString();
    }

    /**
     * 验证需求是否有效
     */
    public boolean isValid() {
        return StringUtils.hasText(title) &&
                user != null &&
                category != null &&
                (maxPrice == null || maxPrice >= 0) &&
                (minPrice == null || minPrice >= 0) &&
                (maxPrice == null || minPrice == null || maxPrice >= minPrice);
    }

    /**
     * 获取匹配条件配置
     */
    public MatchCondition getDefaultMatchCondition() {
        MatchCondition condition = new MatchCondition();
        condition.setMatchByIsbn(true);
        condition.setMatchByTitle(true);
        condition.setMatchByAuthor(true);
        condition.setMatchByCategory(true);
        condition.setTitleSimilarity(0.8);
        condition.setAuthorSimilarity(0.7);
        condition.setPriceRatio(1.2);
        return condition;
    }

    /**
     * 计算字符串相似度（简易版）
     */
    private double calculateStringSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) return 0;

        String s1 = str1.toLowerCase().replaceAll("\\s+", "");
        String s2 = str2.toLowerCase().replaceAll("\\s+", "");

        if (s1.equals(s2)) return 1.0;

        // 计算编辑距离相似度
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        return maxLength > 0 ? 1.0 - (double) distance / maxLength : 0;
    }

    /**
     * 计算Levenshtein距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 获取安全的副本（用于公开API）
     */
    public Need getSafeCopy() {
        Need safeCopy = new Need();
        safeCopy.setId(this.id);
        safeCopy.setNeedId(this.needId);
        safeCopy.setTitle(this.title);
        safeCopy.setAuthor(this.author);
        safeCopy.setIsbn(this.isbn);
        safeCopy.setDescription(this.description);
        safeCopy.setMaxPrice(this.maxPrice);
        safeCopy.setCategory(this.category);
        safeCopy.setConditionRequirement(this.conditionRequirement);
        safeCopy.setNegotiable(this.negotiable);
        safeCopy.setStatus(this.status);
        safeCopy.setFulfilled(this.fulfilled);
        safeCopy.setCreateTime(this.createTime);
        safeCopy.setUpdateTime(this.updateTime);
        safeCopy.setUrgency(this.urgency);
        safeCopy.setPriority(this.priority);
        safeCopy.setFavoriteCount(this.favoriteCount);
        safeCopy.setViewCount(this.viewCount);
        safeCopy.setOfferCount(this.offerCount);

        // 用户信息脱敏
        if (this.user != null) {
            safeCopy.setUser(this.user.getSafeCopy());
        }

        // 不包含联系方式等敏感信息
        return safeCopy;
    }

    // ========== Getter/Setter ==========

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNeedId() { return needId; }
    public void setNeedId(String needId) { this.needId = needId; }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.updateTime = new Date();
    }

    public String getAuthor() { return author; }
    public void setAuthor(String author) {
        this.author = author;
        this.updateTime = new Date();
    }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) {
        this.isbn = isbn;
        this.updateTime = new Date();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updateTime = new Date();
    }

    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
        this.updateTime = new Date();
    }

    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
        this.updateTime = new Date();
    }

    public Boolean getNegotiable() { return negotiable != null ? negotiable : true; }
    public void setNegotiable(Boolean negotiable) {
        this.negotiable = negotiable;
        this.updateTime = new Date();
    }

    public String getCategory() { return category; }
    public void setCategory(String category) {
        this.category = category;
        this.updateTime = new Date();
    }

    public String getConditionRequirement() {
        return conditionRequirement != null ? conditionRequirement : "良好及以上";
    }
    public void setConditionRequirement(String conditionRequirement) {
        this.conditionRequirement = conditionRequirement;
        this.updateTime = new Date();
    }

    public Boolean getRequireOriginal() { return requireOriginal != null ? requireOriginal : false; }
    public void setRequireOriginal(Boolean requireOriginal) {
        this.requireOriginal = requireOriginal;
        this.updateTime = new Date();
    }

    public Boolean getRequireInvoice() { return requireInvoice != null ? requireInvoice : false; }
    public void setRequireInvoice(Boolean requireInvoice) {
        this.requireInvoice = requireInvoice;
        this.updateTime = new Date();
    }

    public Boolean getRequireDelivery() { return requireDelivery != null ? requireDelivery : false; }
    public void setRequireDelivery(Boolean requireDelivery) {
        this.requireDelivery = requireDelivery;
        this.updateTime = new Date();
    }

    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
        this.updateTime = new Date();
    }

    public String getContactMethod() { return contactMethod; }
    public void setContactMethod(String contactMethod) {
        this.contactMethod = contactMethod;
        this.updateTime = new Date();
    }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
        this.updateTime = new Date();
    }

    public NeedStatus getStatus() { return status; }
    public void setStatus(NeedStatus status) {
        this.status = status;
        this.updateTime = new Date();

        // 同步fulfilled字段
        if (status == NeedStatus.FULFILLED) {
            this.fulfilled = true;
        } else if (status == NeedStatus.ACTIVE || status == NeedStatus.EXPIRED) {
            this.fulfilled = false;
        }
    }

    public Boolean getFulfilled() { return fulfilled; }
    public void setFulfilled(Boolean fulfilled) {
        this.fulfilled = fulfilled;
        this.updateTime = new Date();

        // 同步status字段
        if (fulfilled != null && fulfilled) {
            this.status = NeedStatus.FULFILLED;
        }
    }

    public Book getMatchedBook() { return matchedBook; }
    public void setMatchedBook(Book matchedBook) {
        this.matchedBook = matchedBook;
        this.updateTime = new Date();
    }

    public Date getMatchTime() { return matchTime; }
    public void setMatchTime(Date matchTime) {
        this.matchTime = matchTime;
        this.updateTime = new Date();
    }

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
        this.updateTime = new Date();
    }

    public Integer getFavoriteCount() { return favoriteCount != null ? favoriteCount : 0; }
    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
        this.updateTime = new Date();
    }

    public Integer getViewCount() { return viewCount != null ? viewCount : 0; }
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
        this.updateTime = new Date();
    }

    public Integer getOfferCount() { return offerCount != null ? offerCount : 0; }
    public void setOfferCount(Integer offerCount) {
        this.offerCount = offerCount;
        this.updateTime = new Date();
    }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public Date getExpiryTime() { return expiryTime; }
    public void setExpiryTime(Date expiryTime) {
        this.expiryTime = expiryTime;
        this.updateTime = new Date();
    }

    public Date getFulfillTime() { return fulfillTime; }
    public void setFulfillTime(Date fulfillTime) {
        this.fulfillTime = fulfillTime;
        this.updateTime = new Date();
    }

    public Integer getUrgency() { return urgency != null ? urgency : 5; }
    public void setUrgency(Integer urgency) {
        this.urgency = urgency;
        this.updateTime = new Date();
    }

    public Priority getPriority() { return priority != null ? priority : Priority.NORMAL; }
    public void setPriority(Priority priority) {
        this.priority = priority;
        this.updateTime = new Date();
    }

    public Set<String> getTags() {
        if (tags == null) {
            tags = new HashSet<>();
        }
        return tags;
    }
    public void setTags(Set<String> tags) {
        this.tags = tags != null ? new HashSet<>(tags) : new HashSet<>();
        this.updateTime = new Date();
    }

    public List<Book> getMatchedBooks() { return matchedBooks; }
    public void setMatchedBooks(List<Book> matchedBooks) { this.matchedBooks = matchedBooks; }

    public Double getMatchConfidence() { return matchConfidence; }
    public void setMatchConfidence(Double matchConfidence) { this.matchConfidence = matchConfidence; }

    public Boolean getIsExpired() {
        if (isExpired == null) {
            isExpired = isExpired();
        }
        return isExpired;
    }
    public void setIsExpired(Boolean isExpired) { this.isExpired = isExpired; }

    @Override
    public String toString() {
        return "Need{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", maxPrice=" + maxPrice +
                ", category='" + category + '\'' +
                ", status=" + status +
                ", fulfilled=" + fulfilled +
                ", urgency=" + urgency +
                ", createTime=" + createTime +
                ", expiryTime=" + expiryTime +
                '}';
    }
}