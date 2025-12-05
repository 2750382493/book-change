package com.campus.book.service;

import com.campus.book.model.User;
import com.campus.book.repository.UserRepository;
import com.campus.book.util.PasswordUtil;
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
public class UserService {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // ========== 用户注册与登录 ==========

    /**
     * 用户注册
     */
    @Transactional
    public User register(User user) {
        if (user == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        try {
            // 验证用户名格式
            if (!User.isValidUsername(user.getUsername())) {
                throw new IllegalArgumentException("用户名格式不正确");
            }

            // 验证密码复杂度
            if (!User.isValidPassword(user.getRawPassword())) {
                throw new IllegalArgumentException("密码必须包含字母和数字，长度6-20位");
            }

            // 检查用户名是否已存在
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new IllegalArgumentException("用户名已存在");
            }

            // 检查邮箱是否已存在
            if (StringUtils.hasText(user.getEmail()) && 
                userRepository.existsByEmail(user.getEmail())) {
                throw new IllegalArgumentException("邮箱已被注册");
            }

            // 加密密码
            String encodedPassword = PasswordUtil.encodePassword(user.getRawPassword());
            user.setPassword(encodedPassword);
            user.setRawPassword(null); // 清除明文密码

            // 设置默认值
            user.setCreateTime(new Date());
            user.setUpdateTime(new Date());
            user.setStatus(0); // 正常状态
            user.setEmailVerified(false);
            
            // 添加默认角色
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_USER");
            user.setRoles(roles);

            // 保存用户
            User savedUser = userRepository.save(user);
            
            log.info("用户注册成功，用户名: {}, 用户ID: {}", 
                savedUser.getUsername(), savedUser.getId());
            
            // 发送欢迎邮件
            if (StringUtils.hasText(savedUser.getEmail())) {
                emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername());
            }
            
            // 发送验证邮件
            if (StringUtils.hasText(savedUser.getEmail())) {
                emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getId());
            }
            
            return savedUser;
        } catch (Exception e) {
            log.error("用户注册失败，用户名: {}", user.getUsername(), e);
            throw new RuntimeException("用户注册失败: " + e.getMessage(), e);
        }
    }

    /**
     * 用户登录
     */
    public Optional<User> login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Optional.empty();
        }

        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // 验证密码
                if (PasswordUtil.matches(password, user.getPassword())) {
                    // 检查用户状态
                    if (user.isDisabled()) {
                        log.warn("用户尝试登录但账户已被禁用，用户名: {}", username);
                        return Optional.empty();
                    }
                    
                    // 更新最后登录时间
                    user.updateLastLogin();
                    userRepository.save(user);
                    
                    log.info("用户登录成功，用户名: {}, 用户ID: {}", username, user.getId());
                    return Optional.of(user);
                }
            }
            
            log.warn("用户登录失败，用户名: {}", username);
            return Optional.empty();
        } catch (Exception e) {
            log.error("用户登录异常，用户名: {}", username, e);
            return Optional.empty();
        }
    }

    /**
     * 邮箱登录
     */
    public Optional<User> loginByEmail(String email, String password) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return Optional.empty();
        }

        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // 验证密码
                if (PasswordUtil.matches(password, user.getPassword())) {
                    // 检查用户状态
                    if (user.isDisabled()) {
                        log.warn("用户尝试登录但账户已被禁用，邮箱: {}", email);
                        return Optional.empty();
                    }
                    
                    // 更新最后登录时间
                    user.updateLastLogin();
                    userRepository.save(user);
                    
                    log.info("用户邮箱登录成功，邮箱: {}, 用户ID: {}", email, user.getId());
                    return Optional.of(user);
                }
            }
            
            log.warn("用户邮箱登录失败，邮箱: {}", email);
            return Optional.empty();
        } catch (Exception e) {
            log.error("用户邮箱登录异常，邮箱: {}", email, e);
            return Optional.empty();
        }
    }

    // ========== 用户信息查询 ==========

    /**
     * 根据ID获取用户
     */
    public Optional<User> findById(String id) {
        if (!StringUtils.hasText(id)) {
            return Optional.empty();
        }

        try {
            return userRepository.findById(id);
        } catch (Exception e) {
            log.error("根据ID获取用户失败，ID: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 根据用户名获取用户
     */
    public Optional<User> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }

        try {
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            log.error("根据用户名获取用户失败，用户名: {}", username, e);
            return Optional.empty();
        }
    }

    /**
     * 根据邮箱获取用户
     */
    public Optional<User> findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }

        try {
            return userRepository.findByEmail(email);
        } catch (Exception e) {
            log.error("根据邮箱获取用户失败，邮箱: {}", email, e);
            return Optional.empty();
        }
    }

    /**
     * 根据手机号获取用户
     */
    public Optional<User> findByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return Optional.empty();
        }

        try {
            return userRepository.findByPhone(phone);
        } catch (Exception e) {
            log.error("根据手机号获取用户失败，手机号: {}", phone, e);
            return Optional.empty();
        }
    }

    /**
     * 获取所有用户（分页）
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 搜索用户
     */
    public List<User> searchUsers(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        try {
            Query query = new Query();
            query.addCriteria(new Criteria().orOperator(
                Criteria.where("username").regex(keyword, "i"),
                Criteria.where("nickname").regex(keyword, "i"),
                Criteria.where("email").regex(keyword, "i"),
                Criteria.where("phone").regex(keyword)
            ));
            query.addCriteria(Criteria.where("deleted").ne(true));
            
            return mongoTemplate.find(query, User.class);
        } catch (Exception e) {
            log.error("搜索用户失败，关键字: {}", keyword, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户列表（按条件筛选）
     */
    public List<User> getUsersByCondition(String username, String nickname, 
                                         String email, Integer status, 
                                         String role) {
        Query query = new Query();
        
        if (StringUtils.hasText(username)) {
            query.addCriteria(Criteria.where("username").regex(username, "i"));
        }
        
        if (StringUtils.hasText(nickname)) {
            query.addCriteria(Criteria.where("nickname").regex(nickname, "i"));
        }
        
        if (StringUtils.hasText(email)) {
            query.addCriteria(Criteria.where("email").regex(email, "i"));
        }
        
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        if (StringUtils.hasText(role)) {
            query.addCriteria(Criteria.where("roles").in(role));
        }
        
        query.addCriteria(Criteria.where("deleted").ne(true));
        
        try {
            return mongoTemplate.find(query, User.class);
        } catch (Exception e) {
            log.error("按条件查询用户失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取在线用户（最近N天内登录）
     */
    public List<User> getOnlineUsers(int days) {
        Date thresholdDate = Date.from(
            LocalDateTime.now().minusDays(days)
                .atZone(ZoneId.systemDefault()).toInstant()
        );
        
        Query query = new Query();
        query.addCriteria(Criteria.where("lastLoginTime").gte(thresholdDate));
        query.addCriteria(Criteria.where("status").is(0));
        query.addCriteria(Criteria.where("deleted").ne(true));
        
        try {
            return mongoTemplate.find(query, User.class);
        } catch (Exception e) {
            log.error("获取在线用户失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取新注册用户（最近N天内）
     */
    public List<User> getNewUsers(int days) {
        Date thresholdDate = Date.from(
            LocalDateTime.now().minusDays(days)
                .atZone(ZoneId.systemDefault()).toInstant()
        );
        
        Query query = new Query();
        query.addCriteria(Criteria.where("createTime").gte(thresholdDate));
        query.addCriteria(Criteria.where("deleted").ne(true));
        query.with(org.springframework.data.domain.Sort.by(
            org.springframework.data.domain.Sort.Order.desc("createTime")
        ));
        
        try {
            return mongoTemplate.find(query, User.class);
        } catch (Exception e) {
            log.error("获取新注册用户失败", e);
            return Collections.emptyList();
        }
    }

    // ========== 用户信息更新 ==========

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("用户信息或用户ID不能为空");
        }

        try {
            Optional<User> existingUserOpt = userRepository.findById(user.getId());
            if (!existingUserOpt.isPresent()) {
                throw new IllegalArgumentException("用户不存在");
            }

            User existingUser = existingUserOpt.get();
            
            // 更新基本信息（不更新密码、角色等敏感信息）
            existingUser.updateProfile(
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatar(),
                user.getBio()
            );
            
            User updatedUser = userRepository.save(existingUser);
            
            log.info("用户信息更新成功，用户ID: {}", updatedUser.getId());
            
            return updatedUser;
        } catch (Exception e) {
            log.error("更新用户信息失败，用户ID: {}", user.getId(), e);
            throw new RuntimeException("更新用户信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新用户资料
     */
    @Transactional
    public User updateProfile(String userId, String nickname, String phone, 
                             String email, String avatar, String bio) {
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        
        // 验证邮箱唯一性（如果变更了邮箱）
        if (StringUtils.hasText(email) && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("邮箱已被注册");
            }
            user.setEmailVerified(false); // 邮箱变更需要重新验证
        }
        
        user.updateProfile(nickname, phone, email, avatar, bio);
        
        User updatedUser = userRepository.save(user);
        
        log.info("用户资料更新成功，用户ID: {}", userId);
        
        return updatedUser;
    }

    /**
     * 更新隐私设置
     */
    @Transactional
    public User updatePrivacySettings(String userId, Boolean showPhone, Boolean showEmail) {
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        user.updatePrivacySettings(showPhone, showEmail);
        
        User updatedUser = userRepository.save(user);
        
        log.info("用户隐私设置更新成功，用户ID: {}", userId);
        
        return updatedUser;
    }

    /**
     * 更新密码
     */
    @Transactional
    public void updatePassword(String userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        
        // 验证旧密码
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        
        // 验证新密码复杂度
        if (!User.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("新密码必须包含字母和数字，长度6-20位");
        }
        
        // 更新密码
        String encodedPassword = PasswordUtil.encodePassword(newPassword);
        user.setPassword(encodedPassword);
        user.setUpdateTime(new Date());
        
        userRepository.save(user);
        
        log.info("用户密码更新成功，用户ID: {}", userId);
        
        // 发送密码变更通知
        notificationService.notifyPasswordChanged(user);
    }

    /**
     * 重置密码（忘记密码）
     */
    @Transactional
    public void resetPassword(String email, String newPassword) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("邮箱或新密码不能为空");
        }

        Optional<User> userOpt = findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("邮箱未注册");
        }

        User user = userOpt.get();
        
        // 验证新密码复杂度
        if (!User.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("新密码必须包含字母和数字，长度6-20位");
        }
        
        // 更新密码
        String encodedPassword = PasswordUtil.encodePassword(newPassword);
        user.setPassword(encodedPassword);
        user.setUpdateTime(new Date());
        
        userRepository.save(user);
        
        log.info("用户密码重置成功，邮箱: {}", email);
        
        // 发送密码重置通知
        notificationService.notifyPasswordReset(user);
    }

    // ========== 用户状态管理 ==========

    /**
     * 禁用用户
     */
    @Transactional
    public User disableUser(String userId) {
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        
        if (user.isAdmin()) {
            throw new IllegalStateException("不能禁用管理员账户");
        }
        
        user.disable();
        
        User updatedUser = userRepository.save(user);
        
        log.info("用户已禁用，用户ID: {}", userId);
        
        // 发送账户禁用通知
        notificationService.notifyAccountDisabled(user);
        
        return updatedUser;
    }

    /**
     * 启用用户
     */
    @Transactional
    public User enableUser(String userId) {
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        user.enable();
        
        User updatedUser = userRepository.save(user);
        
        log.info("用户已启用，用户ID: {}", userId);
        
        // 发送账户启用通知
        notificationService.notifyAccountEnabled(user);
        
        return updatedUser;
    }

    /**
     * 锁定用户（临时禁用）
     */
    @Transactional
    public User lockUser(String userId, int lockHours, String reason) {
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        
        if (user.isAdmin()) {
            throw new IllegalStateException("不能锁定管理员账户");
        }
        
        // 设置锁定状态
        user.setStatus(1); // 禁用状态
        user.setUpdateTime(new Date());
        
        // 添加锁定标签
        user.addTag("locked:" + lockHours + "h");
        if (StringUtils.hasText(reason)) {
            user.addTag("lock-reason:" + reason);
        }
        
        User updatedUser = userRepository.save(user);
        
        log.info("用户已锁定，用户ID: {}, 锁定时长: {}小时, 原因: {}", 
            userId, lockHours, reason);
        
        // 发送账户锁定通知
        notificationService.notifyAccountLocked(user, lockHours, reason);
        
        return updatedUser;
    }

    // ========== 角色管理 ==========

    /**
     * 添加用户角色
     */
    @Transactional
    public User addRole(String userId, String role) {
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("角色不能为空");
        }

        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        user.addRole(role);
        
        User updatedUser = userRepository.save(user);
        
        log.info("用户角色已添加，用户ID: {}, 角色: {}", userId, role);
        
        // 发送角色变更通知
        notificationService.notifyRoleAdded(user, role);
        
        return updatedUser;
    }

    /**
     * 移除用户角色
     */
    @Transactional
    public User removeRole(String userId, String role) {
        if (!StringUtils.hasText(role)) {
            throw new IllegalArgumentException("角色不能为空");
        }

        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        
        // 不能移除管理员的管理员角色（至少保留一个管理员）
        if (role.equals("ROLE_ADMIN") && user.isAdmin() && 
            countAdmins() <= 1 && user.getRoles().contains("ROLE_ADMIN")) {
            throw new IllegalStateException("系统必须至少保留一个管理员");
        }
        
        user.removeRole(role);
        
        User updatedUser = userRepository.save(user);
        
        log.info("用户角色已移除，用户ID: {}, 角色: {}", userId, role);
        
        // 发送角色变更通知
        notificationService.notifyRoleRemoved(user, role);
        
        return updatedUser;
    }

    /**
     * 检查用户是否有角色
     */
    public boolean hasRole(String userId, String role) {
        Optional<User> userOpt = findById(userId);
        return userOpt.isPresent() && userOpt.get().hasRole(role);
    }

    /**
     * 统计管理员数量
     */
    public long countAdmins() {
        Query query = new Query();
        query.addCriteria(Criteria.where("roles").in("ROLE_ADMIN"));
        query.addCriteria(Criteria.where("deleted").ne(true));
        
        return mongoTemplate.count(query, User.class);
    }

    // ========== 邮箱验证 ==========

    /**
     * 验证邮箱
     */
    @Transactional
    public User verifyEmail(String userId) {
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        user.verifyEmail();
        
        User updatedUser = userRepository.save(user);
        
        log.info("用户邮箱已验证，用户ID: {}, 邮箱: {}", userId, user.getEmail());
        
        // 发送邮箱验证成功通知
        notificationService.notifyEmailVerified(user);
        
        return updatedUser;
    }

    /**
     * 重新发送验证邮件
     */
    public void resendVerificationEmail(String email) {
        Optional<User> userOpt = findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("邮箱未注册");
        }

        User user = userOpt.get();
        
        if (user.getEmailVerified()) {
            throw new IllegalStateException("邮箱已验证");
        }
        
        // 发送验证邮件
        emailService.sendVerificationEmail(user.getEmail(), user.getId());
        
        log.info("重新发送验证邮件，邮箱: {}", email);
    }

    // ========== 统计与计数 ==========

    /**
     * 增加用户发布书籍计数
     */
    @Transactional
    public void incrementBookCount(String userId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().inc("bookCount", 1).set("updateTime", new Date());
        
        mongoTemplate.updateFirst(query, update, User.class);
        
        log.debug("用户发布书籍计数增加，用户ID: {}", userId);
    }

    /**
     * 增加用户售出书籍计数
     */
    @Transactional
    public void incrementSoldCount(String userId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().inc("soldCount", 1).set("updateTime", new Date());
        
        mongoTemplate.updateFirst(query, update, User.class);
        
        log.debug("用户售出书籍计数增加，用户ID: {}", userId);
    }

    /**
     * 增加用户评分
     */
    @Transactional
    public void addRating(String userId, Integer rating) {
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        user.addRating(rating);
        
        userRepository.save(user);
        
        log.info("用户评分已更新，用户ID: {}, 新评分: {}", userId, rating);
    }

    /**
     * 获取用户统计信息
     */
    public Map<String, Object> getUserStatistics(String userId) {
        Map<String, Object> stats = new HashMap<>();
        
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            return stats;
        }

        User user = userOpt.get();
        
        stats.put("userId", user.getId());
        stats.put("username", user.getUsername());
        stats.put("bookCount", user.getBookCount());
        stats.put("soldCount", user.getSoldCount());
        stats.put("rating", user.getRating());
        stats.put("ratingCount", user.getRatingCount());
        stats.put("registrationDate", user.getCreateTime());
        stats.put("lastLogin", user.getLastLoginTime());
        
        // 可以添加更多统计信息，如活跃度、交易成功率等
        
        return stats;
    }

    /**
     * 获取系统统计信息
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Query query = new Query();
        query.addCriteria(Criteria.where("deleted").ne(true));
        
        long totalUsers = mongoTemplate.count(query, User.class);
        stats.put("totalUsers", totalUsers);
        
        // 按状态统计
        query.addCriteria(Criteria.where("status").is(0));
        long activeUsers = mongoTemplate.count(query, User.class);
        stats.put("activeUsers", activeUsers);
        
        query = new Query(Criteria.where("status").is(1));
        long disabledUsers = mongoTemplate.count(query, User.class);
        stats.put("disabledUsers", disabledUsers);
        
        // 按角色统计
        query = new Query(Criteria.where("roles").in("ROLE_ADMIN"));
        long adminCount = mongoTemplate.count(query, User.class);
        stats.put("adminCount", adminCount);
        
        // 邮箱验证统计
        query = new Query(Criteria.where("emailVerified").is(true));
        long verifiedUsers = mongoTemplate.count(query, User.class);
        stats.put("verifiedUsers", verifiedUsers);
        
        // 最近7天新用户
        Date sevenDaysAgo = Date.from(
            LocalDateTime.now().minusDays(7)
                .atZone(ZoneId.systemDefault()).toInstant()
        );
        query = new Query(Criteria.where("createTime").gte(sevenDaysAgo));
        long newUsersLast7Days = mongoTemplate.count(query, User.class);
        stats.put("newUsersLast7Days", newUsersLast7Days);
        
        return stats;
    }

    // ========== 用户删除 ==========

    /**
     * 删除用户（逻辑删除）
     */
    @Transactional
    public void deleteUser(String userId) {
        Optional<User> userOpt = findById(userId);
        if (!userOpt.isPresent()) {
            throw new IllegalArgumentException("用户不存在");
        }

        User user = userOpt.get();
        
        if (user.isAdmin() && countAdmins() <= 1) {
            throw new IllegalStateException("不能删除最后一个管理员账户");
        }
        
        // 标记为删除
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update()
            .set("deleted", true)
            .set("username", user.getUsername() + "_deleted_" + System.currentTimeMillis())
            .set("email", user.getEmail() + "_deleted_" + System.currentTimeMillis())
            .set("phone", null)
            .set("updateTime", new Date());
        
        mongoTemplate.updateFirst(query, update, User.class);
        
        log.info("用户已逻辑删除，用户ID: {}", userId);
        
        // 发送账户删除通知
        notificationService.notifyAccountDeleted(user);
    }

    /**
     * 批量删除用户
     */
    @Transactional
    public int batchDeleteUsers(List<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return 0;
        }

        // 检查是否包含最后一个管理员
        long adminCount = countAdmins();
        List<String> adminIds = new ArrayList<>();
        
        for (String userId : userIds) {
            Optional<User> userOpt = findById(userId);
            if (userOpt.isPresent() && userOpt.get().isAdmin()) {
                adminIds.add(userId);
            }
        }
        
        if (adminIds.size() >= adminCount) {
            throw new IllegalStateException("不能删除所有管理员账户");
        }
        
        Query query = new Query(Criteria.where("_id").in(userIds));
        Update update = new Update()
            .set("deleted", true)
            .set("updateTime", new Date());
        
        try {
            int updatedCount = mongoTemplate.updateMulti(query, update, User.class).getModifiedCount();
            log.info("批量删除用户完成，数量: {}", updatedCount);
            return updatedCount;
        } catch (Exception e) {
            log.error("批量删除用户失败，用户ID列表: {}", userIds, e);
            return 0;
        }
    }

    // ========== 导出与备份 ==========

    /**
     * 导出用户数据
     */
    public List<User> exportUsers(Date startDate, Date endDate, Integer status) {
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
        
        query.addCriteria(Criteria.where("deleted").ne(true));
        
        return mongoTemplate.find(query, User.class);
    }

    /**
     * 获取用户安全的副本（用于公开API）
     */
    public Optional<User> getSafeUserById(String userId) {
        return findById(userId).map(User::getSafeCopy);
    }

    /**
     * 批量获取用户安全的副本
     */
    public List<User> getSafeUsers(List<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }

        Query query = new Query(Criteria.where("_id").in(userIds));
        query.addCriteria(Criteria.where("deleted").ne(true));
        
        List<User> users = mongoTemplate.find(query, User.class);
        
        return users.stream()
            .map(User::getSafeCopy)
            .collect(Collectors.toList());
    }

    // ========== 定时任务 ==========

    /**
     * 清理未激活用户（注册超过30天未验证邮箱）
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    @Transactional
    public void cleanupInactiveUsers() {
        log.info("开始清理未激活用户...");
        
        Date thirtyDaysAgo = Date.from(
            LocalDateTime.now().minusDays(30)
                .atZone(ZoneId.systemDefault()).toInstant()
        );
        
        Query query = new Query();
        query.addCriteria(Criteria.where("emailVerified").is(false));
        query.addCriteria(Criteria.where("createTime").lt(thirtyDaysAgo));
        query.addCriteria(Criteria.where("status").is(0));
        query.addCriteria(Criteria.where("deleted").ne(true));
        
        List<User> inactiveUsers = mongoTemplate.find(query, User.class);
        
        if (CollectionUtils.isEmpty(inactiveUsers)) {
            log.info("没有发现未激活用户需要清理");
            return;
        }
        
        List<String> userIds = inactiveUsers.stream()
            .map(User::getId)
            .collect(Collectors.toList());
        
        int deletedCount = batchDeleteUsers(userIds);
        
        log.info("已清理{}个未激活用户", deletedCount);
    }

    /**
     * 统计用户活跃度（定时任务）
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void calculateUserActivity() {
        log.info("开始计算用户活跃度...");
        
        // 实现用户活跃度计算逻辑
        // 可以基于登录频率、发布书籍数量、交易数量等指标
        
        log.info("用户活跃度计算完成");
    }
}