package com.campus.book.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.util.StringUtils;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;  // 存储加密后的密码

    @Transient  // 不持久化到数据库
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String rawPassword;  // 明文密码（仅用于接收输入）

    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Indexed(unique = true, sparse = true)  // 允许空值
    private String email;

    private String avatar;

    private Date createTime;
    private Date updateTime;
    private Date lastLoginTime;

    // 用户角色
    private Set<String> roles = new HashSet<>();

    // 用户状态：0-正常，1-禁用，2-未激活
    private Integer status = 0;

    // 邮箱验证状态
    private Boolean emailVerified = false;

    // 个人简介
    private String bio;

    // 统计信息
    private Integer bookCount = 0;      // 发布的书籍数量
    private Integer needCount = 0;      // 发布的需求数量
    private Integer soldCount = 0;      // 已售书籍数量

    // 评分信息
    private Double rating = 5.0;        // 用户评分（1-5）
    private Integer ratingCount = 0;    // 评分次数

    // 社交信息
    private String wechat;
    private String qq;

    // 隐私设置
    private Boolean showPhone = false;
    private Boolean showEmail = false;

    // ========== 构造器 ==========

    /**
     * 无参构造器 - 用于框架反射
     */
    public User() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.roles.add("ROLE_USER");  // 默认角色
        this.status = 0;
        this.emailVerified = false;
    }

    /**
     * 注册用构造器
     */
    public User(String username, String rawPassword, String email) {
        this();
        this.username = username;
        this.rawPassword = rawPassword;  // Service层会加密
        this.email = email;
        this.nickname = username;  // 默认昵称等于用户名
    }

    /**
     * 完整构造器
     */
    public User(String username, String rawPassword, String email,
                String phone, String nickname) {
        this(username, rawPassword, email);
        this.phone = phone;
        this.nickname = nickname != null ? nickname : username;
    }

    // ========== Builder 模式 ==========

    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private User user;

        public UserBuilder() {
            this.user = new User();
        }

        public UserBuilder username(String username) {
            user.setUsername(username);
            return this;
        }

        public UserBuilder password(String rawPassword) {
            user.setRawPassword(rawPassword);
            return this;
        }

        public UserBuilder email(String email) {
            user.setEmail(email);
            return this;
        }

        public UserBuilder phone(String phone) {
            user.setPhone(phone);
            return this;
        }

        public UserBuilder nickname(String nickname) {
            user.setNickname(nickname);
            return this;
        }

        public UserBuilder avatar(String avatar) {
            user.setAvatar(avatar);
            return this;
        }

        public UserBuilder bio(String bio) {
            user.setBio(bio);
            return this;
        }

        public UserBuilder role(String role) {
            user.getRoles().add(role);
            return this;
        }

        public UserBuilder wechat(String wechat) {
            user.setWechat(wechat);
            return this;
        }

        public UserBuilder qq(String qq) {
            user.setQq(qq);
            return this;
        }

        public User build() {
            // 验证必要字段
            if (!StringUtils.hasText(user.getUsername())) {
                throw new IllegalArgumentException("用户名不能为空");
            }
            if (!StringUtils.hasText(user.getRawPassword()) &&
                    !StringUtils.hasText(user.getPassword())) {
                throw new IllegalArgumentException("密码不能为空");
            }
            return user;
        }
    }

    // ========== 业务方法 ==========

    /**
     * 验证用户信息是否有效
     */
    public boolean isValid() {
        return StringUtils.hasText(username)
                && (StringUtils.hasText(password) || StringUtils.hasText(rawPassword))
                && status != null
                && createTime != null;
    }

    /**
     * 用户是否有某个角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 添加角色
     */
    public void addRole(String role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    /**
     * 移除角色
     */
    public void removeRole(String role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * 是否管理员
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * 用户是否被禁用
     */
    public boolean isDisabled() {
        return status != null && status == 1;
    }

    /**
     * 用户是否正常
     */
    public boolean isActive() {
        return status != null && status == 0;
    }

    /**
     * 禁用用户
     */
    public void disable() {
        this.status = 1;
        this.updateTime = new Date();
    }

    /**
     * 启用用户
     */
    public void enable() {
        this.status = 0;
        this.updateTime = new Date();
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLogin() {
        this.lastLoginTime = new Date();
        this.updateTime = new Date();
    }

    /**
     * 验证邮箱
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.updateTime = new Date();
    }

    /**
     * 增加发布书籍计数
     */
    public void incrementBookCount() {
        this.bookCount = (bookCount == null ? 1 : bookCount + 1);
        this.updateTime = new Date();
    }

    /**
     * 增加售出书籍计数
     */
    public void incrementSoldCount() {
        this.soldCount = (soldCount == null ? 1 : soldCount + 1);
        this.updateTime = new Date();
    }

    /**
     * 增加评分
     */
    public void addRating(Integer newRating) {
        if (newRating != null && newRating >= 1 && newRating <= 5) {
            int totalCount = (ratingCount == null ? 0 : ratingCount);
            double totalScore = (rating == null ? 0 : rating * totalCount);

            ratingCount = totalCount + 1;
            rating = (totalScore + newRating) / ratingCount;
            updateTime = new Date();
        }
    }

    /**
     * 更新用户信息
     */
    public void updateProfile(String nickname, String phone, String email,
                              String avatar, String bio) {
        if (nickname != null) this.nickname = nickname;
        if (phone != null) this.phone = phone;
        if (email != null) this.email = email;
        if (avatar != null) this.avatar = avatar;
        if (bio != null) this.bio = bio;
        this.updateTime = new Date();
    }

    /**
     * 更新隐私设置
     */
    public void updatePrivacySettings(Boolean showPhone, Boolean showEmail) {
        if (showPhone != null) this.showPhone = showPhone;
        if (showEmail != null) this.showEmail = showEmail;
        this.updateTime = new Date();
    }

    /**
     * 获取显示名称（优先显示昵称）
     */
    public String getDisplayName() {
        return StringUtils.hasText(nickname) ? nickname : username;
    }

    /**
     * 获取安全的用户信息（用于公开接口）
     */
    public User getSafeCopy() {
        User safeUser = new User();
        safeUser.setId(this.id);
        safeUser.setUsername(this.username);
        safeUser.setNickname(this.nickname);
        safeUser.setAvatar(this.avatar);
        safeUser.setBio(this.bio);
        safeUser.setBookCount(this.bookCount);
        safeUser.setSoldCount(this.soldCount);
        safeUser.setRating(this.rating);
        safeUser.setRatingCount(this.ratingCount);
        // 不包含敏感信息：密码、邮箱、手机号等
        return safeUser;
    }

    // ========== Getter/Setter ==========

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username;
        this.updateTime = new Date();
    }

    public String getPassword() { return password; }
    public void setPassword(String password) {
        this.password = password;
        this.updateTime = new Date();
    }

    public String getRawPassword() { return rawPassword; }
    public void setRawPassword(String rawPassword) {
        this.rawPassword = rawPassword;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) {
        this.nickname = nickname;
        this.updateTime = new Date();
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        this.phone = phone;
        this.updateTime = new Date();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        this.updateTime = new Date();
    }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) {
        this.avatar = avatar;
        this.updateTime = new Date();
    }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public Date getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
        this.updateTime = new Date();
    }

    public Set<String> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        return roles;
    }
    public void setRoles(Set<String> roles) {
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.updateTime = new Date();
    }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) {
        this.status = status;
        this.updateTime = new Date();
    }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
        this.updateTime = new Date();
    }

    public String getBio() { return bio; }
    public void setBio(String bio) {
        this.bio = bio;
        this.updateTime = new Date();
    }

    public Integer getBookCount() { return bookCount != null ? bookCount : 0; }
    public void setBookCount(Integer bookCount) {
        this.bookCount = bookCount;
        this.updateTime = new Date();
    }

    public Integer getNeedCount() { return needCount != null ? needCount : 0; }
    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
        this.updateTime = new Date();
    }

    public Integer getSoldCount() { return soldCount != null ? soldCount : 0; }
    public void setSoldCount(Integer soldCount) {
        this.soldCount = soldCount;
        this.updateTime = new Date();
    }

    public Double getRating() { return rating != null ? rating : 5.0; }
    public void setRating(Double rating) {
        this.rating = rating;
        this.updateTime = new Date();
    }

    public Integer getRatingCount() { return ratingCount != null ? ratingCount : 0; }
    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
        this.updateTime = new Date();
    }

    public String getWechat() { return wechat; }
    public void setWechat(String wechat) {
        this.wechat = wechat;
        this.updateTime = new Date();
    }

    public String getQq() { return qq; }
    public void setQq(String qq) {
        this.qq = qq;
        this.updateTime = new Date();
    }

    public Boolean getShowPhone() { return showPhone != null ? showPhone : false; }
    public void setShowPhone(Boolean showPhone) {
        this.showPhone = showPhone;
        this.updateTime = new Date();
    }

    public Boolean getShowEmail() { return showEmail != null ? showEmail : false; }
    public void setShowEmail(Boolean showEmail) {
        this.showEmail = showEmail;
        this.updateTime = new Date();
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", status=" + status +
                ", roles=" + roles +
                ", bookCount=" + bookCount +
                ", soldCount=" + soldCount +
                ", rating=" + rating +
                ", createTime=" + createTime +
                '}';
    }

    // ========== 验证方法 ==========

    /**
     * 验证手机号格式
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * 验证用户名格式
     */
    public static boolean isValidUsername(String username) {
        return username != null &&
                username.length() >= 4 &&
                username.length() <= 20 &&
                username.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * 验证密码复杂度
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 20) {
            return false;
        }
        // 至少包含字母和数字
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        return hasLetter && hasDigit;
    }
}