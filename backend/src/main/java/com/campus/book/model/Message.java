package com.campus.book.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;

/**
 * 消息实体类
 * 支持多种消息类型：文本、图片、文件、系统通知等
 * 支持消息状态：发送中、已发送、已送达、已读、撤回、删除等
 */
@Document(collection = "messages")
@CompoundIndexes({
        @CompoundIndex(name = "conversation_idx",
                def = "{'fromUser.$id': 1, 'toUser.$id': 1, 'createTime': -1}"),
        @CompoundIndex(name = "thread_idx",
                def = "{'threadId': 1, 'createTime': 1}"),
        @CompoundIndex(name = "user_status_idx",
                def = "{'toUser.$id': 1, 'status': 1, 'createTime': -1}"),
        @CompoundIndex(name = "book_conversation_idx",
                def = "{'book.$id': 1, 'fromUser.$id': 1, 'toUser.$id': 1}")
})
public class Message {

    // ========== 消息标识 ==========

    @Id
    private String id;

    @Indexed
    private String messageId;  // 业务消息ID，可用于去重

    @Indexed
    private String threadId;   // 消息线程ID，用于组织对话

    // ========== 参与者信息 ==========

    @NotNull(message = "发送者不能为空")
    @DBRef
    private User fromUser;

    @NotNull(message = "接收者不能为空")
    @DBRef
    private User toUser;

    @DBRef
    private Book book;  // 关联的书籍（可为空，用于商品咨询）

    // ========== 消息内容 ==========

    @TextIndexed
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字符")
    private String content;

    // 消息类型
    @Indexed
    private MessageType type = MessageType.TEXT;

    // 富文本内容
    private RichContent richContent;

    // 附件列表
    private List<Attachment> attachments = new ArrayList<>();

    // 消息元数据
    private Map<String, Object> metadata = new HashMap<>();

    // ========== 消息状态 ==========

    @Indexed
    private MessageStatus status = MessageStatus.SENT;

    private boolean read = false;
    private Date readTime;      // 阅读时间
    private Date deliveredTime; // 送达时间

    // 消息撤回信息
    private boolean withdrawn = false;
    private Date withdrawTime;
    private String withdrawReason;

    // 消息删除状态（对发送方和接收方分别记录）
    private boolean deletedBySender = false;
    private boolean deletedByReceiver = false;

    // ========== 消息关系 ==========

    @DBRef
    private Message replyTo;  // 回复的消息

    @DBRef
    private List<Message> replies = new ArrayList<>();  // 回复此消息的列表

    private String quoteText;  // 引用的文本（可能已删除的消息）

    // ========== 时间戳 ==========

    @Indexed
    private Date createTime;

    private Date updateTime;
    private Date deleteTime;  // 删除时间（逻辑删除）

    // ========== 系统字段 ==========

    @Transient
    private boolean isNew = false;  // 是否为新消息（前端使用）

    @Transient
    private Long unreadCount;  // 对话未读数（查询时填充）

    // ========== 枚举定义 ==========

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        TEXT("文本消息"),
        IMAGE("图片消息"),
        FILE("文件消息"),
        VOICE("语音消息"),
        LOCATION("位置消息"),
        SYSTEM("系统通知"),
        TRANSACTION("交易消息"),
        REMINDER("提醒消息"),
        CUSTOM("自定义消息");

        private final String description;

        MessageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 消息状态枚举
     */
    public enum MessageStatus {
        SENDING("发送中"),      // 客户端发送中
        SENT("已发送"),        // 服务器已接收
        DELIVERED("已送达"),   // 对方设备已收到
        READ("已读"),          // 对方已阅读
        WITHDRAWN("已撤回"),   // 已撤回
        FAILED("发送失败"),     // 发送失败
        EXPIRED("已过期");      // 消息已过期

        private final String description;

        MessageStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========== 内部类定义 ==========

    /**
     * 富文本内容
     */
    public static class RichContent {
        private String htmlContent;      // HTML内容
        private List<Mention> mentions;  // @提及的用户
        private List<String> emojis;     // 表情符号
        private Formatting formatting;   // 格式设置

        public static class Mention {
            private String userId;
            private String username;
            private int startIndex;
            private int endIndex;

            // getters and setters
        }

        public static class Formatting {
            private boolean bold;
            private boolean italic;
            private boolean underline;
            private String textColor;
            private String backgroundColor;

            // getters and setters
        }

        // getters and setters
    }

    /**
     * 附件
     */
    public static class Attachment {
        private String id;
        private String fileName;
        private String fileUrl;
        private String fileType;      // image, pdf, word, excel, etc.
        private Long fileSize;        // 字节数
        private Integer width;        // 图片宽度
        private Integer height;       // 图片高度
        private Integer duration;     // 音频/视频时长（秒）
        private String thumbnailUrl;  // 缩略图URL

        // getters and setters
    }

    // ========== 构造器 ==========

    /**
     * 无参构造器
     */
    public Message() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.messageId = UUID.randomUUID().toString();
    }

    /**
     * 文本消息构造器
     */
    public Message(User fromUser, User toUser, String content) {
        this();
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.content = content;
        this.type = MessageType.TEXT;
    }

    /**
     * 书籍咨询消息构造器
     */
    public Message(User fromUser, User toUser, Book book, String content) {
        this(fromUser, toUser, content);
        this.book = book;
    }

    /**
     * 回复消息构造器
     */
    public Message(User fromUser, User toUser, String content, Message replyTo) {
        this(fromUser, toUser, content);
        this.replyTo = replyTo;
        if (replyTo != null) {
            this.quoteText = replyTo.getContent();
        }
    }

    /**
     * 图片消息构造器
     */
    public static Message createImageMessage(User fromUser, User toUser,
                                             String imageUrl, String caption) {
        Message message = new Message(fromUser, toUser, caption != null ? caption : "");
        message.type = MessageType.IMAGE;

        Attachment attachment = new Attachment();
        attachment.setFileUrl(imageUrl);
        attachment.setFileType("image");
        attachment.setFileName("image_" + System.currentTimeMillis());

        message.getAttachments().add(attachment);
        return message;
    }

    /**
     * 系统通知构造器
     */
    public static Message createSystemMessage(User toUser, String content,
                                              Map<String, Object> metadata) {
        Message message = new Message();
        message.toUser = toUser;
        message.content = content;
        message.type = MessageType.SYSTEM;

        if (metadata != null) {
            message.metadata = new HashMap<>(metadata);
        }

        // 系统消息的特殊处理
        message.status = MessageStatus.DELIVERED;  // 系统消息直接标记为已送达
        return message;
    }

    /**
     * 交易消息构造器
     */
    public static Message createTransactionMessage(User fromUser, User toUser,
                                                   Book book, String action,
                                                   Map<String, Object> transactionData) {
        Message message = new Message(fromUser, toUser, book, "");
        message.type = MessageType.TRANSACTION;

        // 构建交易消息内容
        StringBuilder content = new StringBuilder();
        content.append("交易").append(action).append("：");
        content.append(book != null ? book.getTitle() : "");

        message.content = content.toString();

        if (transactionData != null) {
            message.metadata.putAll(transactionData);
            message.metadata.put("action", action);
            message.metadata.put("timestamp", new Date());
        }

        return message;
    }

    // ========== Builder 模式 ==========

    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    public static class MessageBuilder {
        private Message message;

        public MessageBuilder() {
            this.message = new Message();
        }

        public MessageBuilder fromUser(User fromUser) {
            message.setFromUser(fromUser);
            return this;
        }

        public MessageBuilder toUser(User toUser) {
            message.setToUser(toUser);
            return this;
        }

        public MessageBuilder book(Book book) {
            message.setBook(book);
            return this;
        }

        public MessageBuilder content(String content) {
            message.setContent(content);
            return this;
        }

        public MessageBuilder type(MessageType type) {
            message.setType(type);
            return this;
        }

        public MessageBuilder replyTo(Message replyTo) {
            message.setReplyTo(replyTo);
            return this;
        }

        public MessageBuilder attachment(Attachment attachment) {
            message.getAttachments().add(attachment);
            return this;
        }

        public MessageBuilder metadata(String key, Object value) {
            message.getMetadata().put(key, value);
            return this;
        }

        public Message build() {
            // 验证必要字段
            if (message.getFromUser() == null) {
                throw new IllegalArgumentException("发送者不能为空");
            }
            if (message.getToUser() == null) {
                throw new IllegalArgumentException("接收者不能为空");
            }
            if (message.getContent() == null && message.getAttachments().isEmpty()) {
                throw new IllegalArgumentException("消息内容或附件不能为空");
            }

            // 自动生成线程ID（如果未设置）
            if (message.getThreadId() == null) {
                message.generateThreadId();
            }

            return message;
        }
    }

    // ========== 业务方法 ==========

    /**
     * 生成对话线程ID
     */
    public void generateThreadId() {
        if (fromUser != null && toUser != null) {
            List<String> ids = Arrays.asList(
                    fromUser.getId(),
                    toUser.getId()
            );
            Collections.sort(ids);  // 排序保证一致性

            if (book != null) {
                this.threadId = String.format("thread_%s_%s_%s",
                        ids.get(0), ids.get(1), book.getId());
            } else {
                this.threadId = String.format("thread_%s_%s",
                        ids.get(0), ids.get(1));
            }
        }
    }

    /**
     * 标记为已送达
     */
    public void markAsDelivered() {
        if (this.status != MessageStatus.DELIVERED) {
            this.status = MessageStatus.DELIVERED;
            this.deliveredTime = new Date();
            this.updateTime = new Date();
        }
    }

    /**
     * 标记为已读
     */
    public void markAsRead() {
        if (!this.read) {
            this.read = true;
            this.status = MessageStatus.READ;
            this.readTime = new Date();
            this.updateTime = new Date();
        }
    }

    /**
     * 撤回消息
     * @param reason 撤回原因
     */
    public void withdraw(String reason) {
        if (!this.withdrawn) {
            this.withdrawn = true;
            this.withdrawTime = new Date();
            this.withdrawReason = reason;
            this.status = MessageStatus.WITHDRAWN;
            this.updateTime = new Date();

            // 清空敏感内容
            this.content = "[消息已撤回]";
            if (this.richContent != null) {
                this.richContent = null;
            }
            if (!this.attachments.isEmpty()) {
                this.attachments.clear();
            }
        }
    }

    /**
     * 删除消息（对当前用户）
     * @param userId 用户ID
     */
    public void deleteForUser(String userId) {
        if (fromUser != null && fromUser.getId().equals(userId)) {
            this.deletedBySender = true;
        } else if (toUser != null && toUser.getId().equals(userId)) {
            this.deletedByReceiver = true;
        }
        this.deleteTime = new Date();
        this.updateTime = new Date();
    }

    /**
     * 检查消息对用户是否可见
     */
    public boolean isVisibleToUser(String userId) {
        // 如果用户是发送方且没有删除
        boolean senderVisible = fromUser != null &&
                fromUser.getId().equals(userId) &&
                !deletedBySender;

        // 如果用户是接收方且没有删除
        boolean receiverVisible = toUser != null &&
                toUser.getId().equals(userId) &&
                !deletedByReceiver;

        return senderVisible || receiverVisible;
    }

    /**
     * 是否为系统消息
     */
    public boolean isSystemMessage() {
        return type == MessageType.SYSTEM;
    }

    /**
     * 是否为交易相关消息
     */
    public boolean isTransactionMessage() {
        return type == MessageType.TRANSACTION;
    }

    /**
     * 是否有附件
     */
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    /**
     * 获取第一个图片附件
     */
    public Attachment getFirstImageAttachment() {
        if (attachments != null) {
            return attachments.stream()
                    .filter(att -> "image".equalsIgnoreCase(att.getFileType()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * 获取消息预览（用于列表显示）
     */
    public String getPreview() {
        if (withdrawn) {
            return "[消息已撤回]";
        }

        if (hasAttachments()) {
            Attachment firstAtt = attachments.get(0);
            switch (firstAtt.getFileType()) {
                case "image":
                    return "[图片] " + (content != null ? content : "");
                case "voice":
                    return "[语音] " + (content != null ? content : "");
                default:
                    return "[文件] " + firstAtt.getFileName();
            }
        }

        // 截断过长的内容
        if (content != null && content.length() > 50) {
            return content.substring(0, 47) + "...";
        }

        return content != null ? content : "";
    }

    /**
     * 验证消息是否有效
     */
    public boolean isValid() {
        return fromUser != null &&
                toUser != null &&
                content != null &&
                !content.trim().isEmpty() || hasAttachments();
    }

    /**
     * 获取安全的副本（用于API返回）
     */
    public Message getSafeCopy() {
        Message safeCopy = new Message();
        safeCopy.setId(this.id);
        safeCopy.setMessageId(this.messageId);
        safeCopy.setThreadId(this.threadId);

        // 用户信息脱敏
        if (this.fromUser != null) {
            safeCopy.setFromUser(this.fromUser.getSafeCopy());
        }
        if (this.toUser != null) {
            safeCopy.setToUser(this.toUser.getSafeCopy());
        }

        // 书籍信息
        safeCopy.setBook(this.book);

        // 根据撤回状态返回内容
        if (this.withdrawn) {
            safeCopy.setContent("[消息已撤回]");
            safeCopy.setAttachments(new ArrayList<>());
        } else {
            safeCopy.setContent(this.content);
            safeCopy.setAttachments(this.attachments);
        }

        safeCopy.setType(this.type);
        safeCopy.setStatus(this.status);
        safeCopy.setRead(this.read);
        safeCopy.setWithdrawn(this.withdrawn);

        // 时间信息
        safeCopy.setCreateTime(this.createTime);
        safeCopy.setReadTime(this.readTime);

        // 回复信息
        if (this.replyTo != null) {
            safeCopy.setReplyTo(this.replyTo.getSafeCopy());
        }

        return safeCopy;
    }

    // ========== Getter/Setter ==========

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public User getFromUser() { return fromUser; }
    public void setFromUser(User fromUser) { this.fromUser = fromUser; }

    public User getToUser() { return toUser; }
    public void setToUser(User toUser) { this.toUser = toUser; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        this.updateTime = new Date();
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) {
        this.type = type;
        this.updateTime = new Date();
    }

    public RichContent getRichContent() { return richContent; }
    public void setRichContent(RichContent richContent) {
        this.richContent = richContent;
        this.updateTime = new Date();
    }

    public List<Attachment> getAttachments() {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        return attachments;
    }
    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments != null ?
                new ArrayList<>(attachments) : new ArrayList<>();
        this.updateTime = new Date();
    }

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ?
                new HashMap<>(metadata) : new HashMap<>();
        this.updateTime = new Date();
    }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) {
        this.status = status;
        this.updateTime = new Date();
    }

    public boolean isRead() { return read; }
    public void setRead(boolean read) {
        this.read = read;
        if (read && this.readTime == null) {
            this.readTime = new Date();
        }
        this.updateTime = new Date();
    }

    public Date getReadTime() { return readTime; }
    public void setReadTime(Date readTime) {
        this.readTime = readTime;
        this.updateTime = new Date();
    }

    public Date getDeliveredTime() { return deliveredTime; }
    public void setDeliveredTime(Date deliveredTime) {
        this.deliveredTime = deliveredTime;
        this.updateTime = new Date();
    }

    public boolean isWithdrawn() { return withdrawn; }
    public void setWithdrawn(boolean withdrawn) {
        this.withdrawn = withdrawn;
        if (withdrawn && this.withdrawTime == null) {
            this.withdrawTime = new Date();
        }
        this.updateTime = new Date();
    }

    public Date getWithdrawTime() { return withdrawTime; }
    public void setWithdrawTime(Date withdrawTime) {
        this.withdrawTime = withdrawTime;
        this.updateTime = new Date();
    }

    public String getWithdrawReason() { return withdrawReason; }
    public void setWithdrawReason(String withdrawReason) {
        this.withdrawReason = withdrawReason;
        this.updateTime = new Date();
    }

    public boolean isDeletedBySender() { return deletedBySender; }
    public void setDeletedBySender(boolean deletedBySender) {
        this.deletedBySender = deletedBySender;
        this.updateTime = new Date();
    }

    public boolean isDeletedByReceiver() { return deletedByReceiver; }
    public void setDeletedByReceiver(boolean deletedByReceiver) {
        this.deletedByReceiver = deletedByReceiver;
        this.updateTime = new Date();
    }

    public Message getReplyTo() { return replyTo; }
    public void setReplyTo(Message replyTo) {
        this.replyTo = replyTo;
        this.updateTime = new Date();
    }

    public List<Message> getReplies() {
        if (replies == null) {
            replies = new ArrayList<>();
        }
        return replies;
    }
    public void setReplies(List<Message> replies) {
        this.replies = replies != null ?
                new ArrayList<>(replies) : new ArrayList<>();
        this.updateTime = new Date();
    }

    public String getQuoteText() { return quoteText; }
    public void setQuoteText(String quoteText) {
        this.quoteText = quoteText;
        this.updateTime = new Date();
    }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public Date getDeleteTime() { return deleteTime; }
    public void setDeleteTime(Date deleteTime) {
        this.deleteTime = deleteTime;
        this.updateTime = new Date();
    }

    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

    public Long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", fromUser=" + (fromUser != null ? fromUser.getId() : "null") +
                ", toUser=" + (toUser != null ? toUser.getId() : "null") +
                ", content='" + getPreview() + '\'' +
                ", status=" + status +
                ", read=" + read +
                ", createTime=" + createTime +
                '}';
    }
}