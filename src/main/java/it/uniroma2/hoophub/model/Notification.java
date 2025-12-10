package it.uniroma2.hoophub.model;

import it.uniroma2.hoophub.enums.NotificationType;
import it.uniroma2.hoophub.enums.UserType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a Notification entity.
 * Immutable object representing a message for a user.
 *
 * @author Elia Cinti
 */
public class Notification {

    private final int id;
    private final String username;
    private final UserType userType;
    private final NotificationType type;
    private final String message;
    private final Integer bookingId;   // CAMBIATO DA int A Integer (Nullable)
    private final boolean isRead;
    private final LocalDateTime createdAt;

    private Notification(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.userType = builder.userType;
        this.type = builder.type;
        this.message = builder.message;
        this.bookingId = builder.bookingId;
        this.isRead = builder.isRead;
        this.createdAt = builder.createdAt;
    }

    // ========================================================================
    // PUBLIC BUSINESS OPERATIONS
    // ========================================================================

    public Notification markAsRead() {
        return new Builder()
                .from(this)
                .isRead(true)
                .build();
    }

    public boolean isUnread() {
        return !isRead;
    }

    // ========================================================================
    // PUBLIC GETTERS
    // ========================================================================

    public int getId() { return id; }
    public String getUsername() { return username; }
    public UserType getUserType() { return userType; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public Integer getBookingId() { return bookingId; } // Ritorna Integer
    public boolean isRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ========================================================================
    // BUILDER CLASS
    // ========================================================================

    public static class Builder {
        private int id;
        private String username;
        private UserType userType;
        private NotificationType type;
        private String message;
        private Integer bookingId;
        private boolean isRead = false;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
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

        public Builder bookingId(Integer bookingId) { // Accetta Integer
            this.bookingId = bookingId;
            return this;
        }

        // Metodo legacy per compatibilità (se serve ancora relatedBookingId)
        public Builder relatedBookingId(int bookingId) {
            this.bookingId = bookingId;
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

        public Builder from(Notification notification) {
            this.id = notification.id;
            this.username = notification.username;
            this.userType = notification.userType;
            this.type = notification.type;
            this.message = notification.message;
            this.bookingId = notification.bookingId;
            this.isRead = notification.isRead;
            this.createdAt = notification.createdAt;
            return this;
        }

        public Notification build() {
            validateRequiredFields();
            return new Notification(this);
        }

        private void validateRequiredFields() {
            if (username == null || username.trim().isEmpty()) throw new IllegalStateException("username required");
            if (userType == null) throw new IllegalStateException("userType required");
            if (type == null) throw new IllegalStateException("type required");
            if (message == null || message.trim().isEmpty()) throw new IllegalStateException("message required");
            if (createdAt == null) throw new IllegalStateException("createdAt required");
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", recipient='" + username + '\'' + // Destinatario (username)
                ", type=" + type +                  // Utile per capire la fonte/motivo
                ", message='" + message + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}