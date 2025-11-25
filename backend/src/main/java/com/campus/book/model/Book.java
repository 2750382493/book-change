package com.campus.book.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

// 添加导入
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
    private List<String> images = new ArrayList<>(); // 确保正确初始化

    @DBRef
    private User seller;

    private Boolean sold = false;
    private Date createTime;
    private Date updateTime;

    public Book() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.sold = false;
        this.images = new ArrayList<>(); // 在构造器中再次确保初始化
    }

    // Getter和Setter方法保持不变...
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
    public void setCategory(String category) { this.category = category; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public List<String> getImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
        return images;
    }
    public void setImages(List<String> images) { this.images = images; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public Boolean getSold() { return sold; }
    public void setSold(Boolean sold) { this.sold = sold; }

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
                ", images=" + images +
                ", seller=" + (seller != null ? seller.getId() : "null") +
                '}';
    }
}