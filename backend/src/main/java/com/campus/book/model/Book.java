package com.campus.book.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "books")
public class Book {
    @Id
    private String id;
    private String title;
    private String author;
    private String publisher;
    private String isbn;
    private String description;
    private Double price;
    private Double originalPrice;
    private String category;
    private String condition = "良好";
    private List<String> images; // 移除字段初始化，在构造器中统一处理

    @DBRef
    private User seller;

    private Boolean sold = false;
    private Date createTime;
    private Date updateTime;

    // ========== 构造器 ==========

    /**
     * 无参构造器 - 用于框架反射创建对象
     */
    public Book() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.sold = false;
        this.condition = "良好";
        this.images = new ArrayList<>();
    }

    /**
     * 最小信息构造器 - 创建书籍时使用
     * @param title 书名
     * @param price 价格
     * @param seller 卖家
     */
    public Book(String title, Double price, User seller) {
        this();
        this.title = title;
        this.price = price;
        this.seller = seller;
        this.category = "其他";
        this.condition = "良好";
    }

    /**
     * 完整信息构造器
     * @param title 书名
     * @param author 作者
     * @param price 价格
     * @param seller 卖家
     * @param category 分类
     * @param condition 新旧程度
     */
    public Book(String title, String author, Double price, User seller,
                String category, String condition) {
        this();
        this.title = title;
        this.author = author;
        this.price = price;
        this.seller = seller;
        this.category = (category != null && !category.trim().isEmpty())
                ? category : "其他";
        this.condition = (condition != null && !condition.trim().isEmpty())
                ? condition : "良好";
    }

    /**
     * 拷贝构造器（部分字段）
     * @param source 源书籍对象
     */
    public Book(Book source) {
        this();
        this.title = source.getTitle();
        this.author = source.getAuthor();
        this.price = source.getPrice();
        this.category = source.getCategory();
        this.condition = source.getCondition();
        this.description = source.getDescription();
        // 注意：不拷贝id、seller、createTime等字段
    }

    // ========== Builder 模式构造器 ==========

    /**
     * 静态Builder方法
     */
    public static BookBuilder builder() {
        return new BookBuilder();
    }

    /**
     * Builder内部类
     */
    public static class BookBuilder {
        private Book book;

        public BookBuilder() {
            this.book = new Book();
        }

        public BookBuilder title(String title) {
            book.setTitle(title);
            return this;
        }

        public BookBuilder author(String author) {
            book.setAuthor(author);
            return this;
        }

        public BookBuilder price(Double price) {
            book.setPrice(price);
            return this;
        }

        public BookBuilder seller(User seller) {
            book.setSeller(seller);
            return this;
        }

        public BookBuilder category(String category) {
            book.setCategory(category);
            return this;
        }

        public BookBuilder condition(String condition) {
            book.setCondition(condition);
            return this;
        }

        public BookBuilder description(String description) {
            book.setDescription(description);
            return this;
        }

        public BookBuilder image(String imageUrl) {
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                book.getImages().add(imageUrl);
            }
            return this;
        }

        public BookBuilder images(List<String> images) {
            if (images != null) {
                book.setImages(new ArrayList<>(images));
            }
            return this;
        }

        public Book build() {
            // 验证必要字段
            if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("书名不能为空");
            }
            if (book.getPrice() == null || book.getPrice() <= 0) {
                throw new IllegalArgumentException("价格必须大于0");
            }
            if (book.getSeller() == null) {
                throw new IllegalArgumentException("卖家不能为空");
            }

            // 设置默认值
            if (book.getCategory() == null) {
                book.setCategory("其他");
            }
            if (book.getCondition() == null) {
                book.setCondition("良好");
            }

            return book;
        }
    }

    // ========== 业务方法 ==========

    /**
     * 添加图片
     * @param imageUrl 图片URL
     */
    public void addImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            this.getImages().add(imageUrl);
        }
    }

    /**
     * 添加多张图片
     * @param imageUrls 图片URL列表
     */
    public void addImages(List<String> imageUrls) {
        if (imageUrls != null) {
            this.getImages().addAll(imageUrls);
        }
    }

    /**
     * 标记为已售出
     */
    public void markAsSold() {
        this.sold = true;
        this.updateTime = new Date();
    }

    /**
     * 更新书籍信息
     * @param updates 更新后的书籍信息（部分字段）
     */
    public void updateFrom(Book updates) {
        if (updates.getTitle() != null) {
            this.title = updates.getTitle();
        }
        if (updates.getAuthor() != null) {
            this.author = updates.getAuthor();
        }
        if (updates.getPrice() != null) {
            this.price = updates.getPrice();
        }
        if (updates.getDescription() != null) {
            this.description = updates.getDescription();
        }
        if (updates.getCategory() != null) {
            this.category = updates.getCategory();
        }
        if (updates.getCondition() != null) {
            this.condition = updates.getCondition();
        }
        if (updates.getImages() != null) {
            this.images = new ArrayList<>(updates.getImages());
        }
        this.updateTime = new Date();
    }

    /**
     * 验证书籍信息是否完整
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty()
                && price != null && price > 0
                && seller != null;
    }

    /**
     * 获取书籍简要信息
     */
    public String getSummary() {
        return String.format("%s - %s (¥%.2f)",
                title,
                author != null ? author : "未知作者",
                price);
    }

    // ========== Getter/Setter 改进 ==========

    public List<String> getImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
    }

    // 其他Getter/Setter保持原样...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Double originalPrice) { this.originalPrice = originalPrice; }
    public String getCategory() { return category; }
    public void setCategory(String category) {
        this.category = (category != null && !category.trim().isEmpty())
                ? category : "其他";
    }
    public String getCondition() { return condition; }
    public void setCondition(String condition) {
        this.condition = (condition != null && !condition.trim().isEmpty())
                ? condition : "良好";
    }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    public Boolean getSold() { return sold; }
    public void setSold(Boolean sold) {
        this.sold = sold;
        if (sold) {
            this.updateTime = new Date();
        }
    }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "Book{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", condition='" + condition + '\'' +
                ", sold=" + sold +
                ", images=" + (images != null ? images.size() : 0) + "张" +
                ", seller=" + (seller != null ? seller.getId() : "null") +
                ", createTime=" + createTime +
                '}';
    }
}