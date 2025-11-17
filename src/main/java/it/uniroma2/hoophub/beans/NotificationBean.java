package it.uniroma2.hoophub.beans;

import it.uniroma2.hoophub.model.NotificationType;
import it.uniroma2.hoophub.model.UserType;
import java.time.LocalDateTime;

/**
 * Bean for notification data transfer.
 * <p>
 * This class is a Data Transfer Object (DTO) used to transfer notification data
 * between the boundary layer (UI), controller layer, and persistence layer.
 * It contains only data fields with no business logic, following the Bean pattern.
 * </p>
 *
 * @author Elia Cinti
 */
public class NotificationBean {
    private Long id;
    private Long userId;
    private UserType userType;
    private NotificationType type;
    private String message;
    private Long relatedBookingId;
    private boolean isRead;
    private LocalDateTime createdAt;

    /**
     * Private constructor for use by the Builder.
     */
    private NotificationBean(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.userType = builder.userType;
        this.type = builder.type;
        this.message = builder.message;
        this.relatedBookingId = builder.relatedBookingId;
        this.isRead = builder.isRead;
        this.createdAt = builder.createdAt;
    }

    /**
     * Builder class for constructing NotificationBean instances.
     */
    public static class Builder {
        private Long id;
        private Long userId;
        private UserType userType;
        private NotificationType type;
        private String message;
        private Long relatedBookingId;
        private boolean isRead = false;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder userType(UserType userType) {
            this.userType = userType;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder relatedBookingId(Long relatedBookingId) {
            this.relatedBookingId = relatedBookingId;
            return this;
        }

        public Builder isRead(boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NotificationBean build() {
            return new NotificationBean(this);
        }
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public UserType getUserType() {
        return userType;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Long getRelatedBookingId() {
        return relatedBookingId;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ========================================================================
    // SETTERS
    // ========================================================================

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRelatedBookingId(Long relatedBookingId) {
        this.relatedBookingId = relatedBookingId;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
