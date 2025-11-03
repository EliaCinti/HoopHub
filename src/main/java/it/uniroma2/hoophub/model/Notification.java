package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.utilities.NotificationType;
import it.uniroma2.hoophub.utilities.UserType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a Notification entity.
 * This is an immutable object. Its state cannot be changed after creation.
 * To "modify" a notification (e.g., mark as read), a new instance is
 * created using the markAsRead() business operation.
 * This is a very robust implementation of a BCE Entity.
 *
 * @author Elia Cinti
 */
public class Notification {

    // All fields are final, making the object immutable.
    private final Long id;
    private final Long userId;
    private final UserType userType;
    private final NotificationType type;
    private final String message;
    private final Long relatedBookingId;
    private final boolean isRead;
    private final LocalDateTime createdAt;

    /**
     * Private constructor - use Builder to create instances.
     */
    private Notification(Builder builder) {
        this.id = builder.id;
        this.userId = builder.userId;
        this.userType = builder.userType;
        this.type = builder.type;
        this.message = builder.message;
        this.relatedBookingId = builder.relatedBookingId;
        this.isRead = builder.isRead;
        this.createdAt = builder.createdAt;
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS
    // ========================================================================

    /**
     * Creates a new Notification instance with the same data
     * but marked as read. This is the only "modification" operation.
     * It does not change the state of *this* object.
     *
     * @return new Notification instance marked as read
     */
    public Notification markAsRead() {
        // Does not modify this object; returns a new one.
        return new Builder()
                .from(this)
                .isRead(true)
                .build();
    }

    // ========================================================================
    // PUBLIC QUERIES (Read-Only Access & Business Questions)
    // ========================================================================

    /**
     * Checks if this notification is unread.
     *
     * @return true if unread (isRead == false), false otherwise
     */
    public boolean isUnread() {
        return !isRead;
    }

    // ========================================================================
    // PUBLIC GETTERS (Read-Only Access)
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
    // BUILDER CLASS (For Object Construction)
    // ========================================================================

    /**
     * Builder class for constructing Notification instances.
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

        /**
         * Copy all fields from an existing Notification.
         * Useful for creating modified copies (e.g., in markAsRead).
         */
        public Builder from(Notification notification) {
            this.id = notification.id;
            this.userId = notification.userId;
            this.userType = notification.userType;
            this.type = notification.type;
            this.message = notification.message;
            this.relatedBookingId = notification.relatedBookingId;
            this.isRead = notification.isRead;
            this.createdAt = notification.createdAt;
            return this;
        }

        /**
         * Validates and builds the Notification instance.
         *
         * @return new Notification instance
         * @throws IllegalStateException if required fields are missing
         */
        public Notification build() {
            validateRequiredFields();
            return new Notification(this);
        }

        // ========================================================================
        // PRIVATE VALIDATION HELPERS (Internal Logic)
        // ========================================================================

        /**
         * Validation logic called by the Builder at construction time.
         */
        private void validateRequiredFields() {
            if (userId == null) {
                throw new IllegalStateException("userId is required");
            }
            if (userType == null) {
                throw new IllegalStateException("userType is required");
            }
            if (type == null) {
                throw new IllegalStateException("type is required");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalStateException("message is required and cannot be empty");
            }
            if (createdAt == null) {
                throw new IllegalStateException("createdAt is required");
            }
        }
    }

    // ========================================================================
    // UTILITY METHODS (equals, hashCode, toString)
    // ========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        // Equality is based on the immutable primary key (id)
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", userType=" + userType +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", relatedBookingId=" + relatedBookingId +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}