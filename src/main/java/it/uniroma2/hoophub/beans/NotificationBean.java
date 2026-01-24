package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.enums.UserType;
import java.time.LocalDateTime;

/**
 * DTO for notification data transfer between layers.
 *
 * <p>Pure data carrier for notifications. Defaults: {@code isRead = false},
 * {@code createdAt = now()}.</p>
 *
 * @author Elia Cinti
 * @version 1.0
 */
public class NotificationBean {

    private int id;
    private String username;
    private UserType userType;
    private NotificationType type;
    private String message;
    private int relatedBookingId;
    private boolean isRead;
    private LocalDateTime createdAt;

    private NotificationBean(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.userType = builder.userType;
        this.type = builder.type;
        this.message = builder.message;
        this.relatedBookingId = builder.relatedBookingId;
        this.isRead = builder.isRead;
        this.createdAt = builder.createdAt;
    }

    /**
     * Builder for NotificationBean.
     */
    public static class Builder {
        private int id;
        private String username;
        private UserType userType;
        private NotificationType type;
        private String message;
        private int relatedBookingId;
        private boolean isRead = false;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(int id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder userType(UserType userType) { this.userType = userType; return this; }
        public Builder type(NotificationType type) { this.type = type; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder relatedBookingId(int id) { this.relatedBookingId = id; return this; }
        public Builder isRead(boolean isRead) { this.isRead = isRead; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public NotificationBean build() {
            return new NotificationBean(this);
        }
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public UserType getUserType() { return userType; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public int getRelatedBookingId() { return relatedBookingId; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setUserType(UserType userType) { this.userType = userType; }
    public void setType(NotificationType type) { this.type = type; }
    public void setMessage(String message) { this.message = message; }
    public void setRelatedBookingId(int id) { this.relatedBookingId = id; }
    public void setRead(boolean read) { isRead = read; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}