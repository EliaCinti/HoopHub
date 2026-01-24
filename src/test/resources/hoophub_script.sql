-- ============================================================================
-- HoopHub Database - SCRIPT
-- ============================================================================

-- Cleans and recreates the database
DROP DATABASE IF EXISTS hoophub;
CREATE DATABASE hoophub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hoophub;

-- ============================================================================
-- TABLE: users (base table for Fan and VenueManager)
-- ============================================================================
CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    user_type ENUM('FAN', 'VENUE_MANAGER') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_type (user_type)
) ENGINE=InnoDB;

-- ============================================================================
-- TABLE: fans (extends users)
-- ============================================================================
CREATE TABLE fans (
    username VARCHAR(50) PRIMARY KEY,
    fav_team VARCHAR(50) NOT NULL,
    birthday DATE NOT NULL,
    CONSTRAINT fk_fan_user
        FOREIGN KEY (username) REFERENCES users(username)
        ON DELETE CASCADE,
    INDEX idx_fav_team (fav_team)
) ENGINE=InnoDB;

-- ============================================================================
-- TABLE: venue_managers (extends users)
-- ============================================================================
CREATE TABLE venue_managers (
    username VARCHAR(50) PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    CONSTRAINT fk_venue_manager_user
        FOREIGN KEY (username) REFERENCES users(username)
        ON DELETE CASCADE,
    INDEX idx_company_name (company_name)
) ENGINE=InnoDB;

-- ============================================================================
-- TABLE: venues
-- ============================================================================
CREATE TABLE venues (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type ENUM('PUB', 'BAR', 'SPORTS_BAR', 'FAN_CLUB', 'RESTAURANT', 'LOUNGE', 'PRIVATE_CLUB') NOT NULL,
    address VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    max_capacity INT NOT NULL,
    venue_manager_username VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_venue_manager
        FOREIGN KEY (venue_manager_username) REFERENCES venue_managers(username)
        ON DELETE CASCADE,
    CONSTRAINT chk_max_capacity_positive CHECK (max_capacity > 0),
    INDEX idx_venue_manager (venue_manager_username),
    INDEX idx_city (city)
) ENGINE=InnoDB;

-- ============================================================================
-- TABLE: venue_teams
-- ============================================================================
CREATE TABLE venue_teams (
    venue_id INT NOT NULL,
    team_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (venue_id, team_name),
    CONSTRAINT fk_venue_team_venue
        FOREIGN KEY (venue_id) REFERENCES venues(id)
        ON DELETE CASCADE,
    INDEX idx_team (team_name)
) ENGINE=InnoDB;

-- ============================================================================
-- TABLE: bookings
-- ============================================================================
CREATE TABLE bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_date DATE NOT NULL,
    game_time TIME NOT NULL,
    home_team VARCHAR(50) NOT NULL,
    away_team VARCHAR(50) NOT NULL,
    venue_id INT NOT NULL,
    fan_username VARCHAR(50) NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    notified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_venue
        FOREIGN KEY (venue_id) REFERENCES venues(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_booking_fan
        FOREIGN KEY (fan_username) REFERENCES fans(username)
        ON DELETE CASCADE,
    CONSTRAINT chk_different_teams CHECK (home_team != away_team),
    CONSTRAINT uq_fan_game UNIQUE (fan_username, game_date, home_team, away_team),
    INDEX idx_fan (fan_username),
    INDEX idx_venue (venue_id),
    INDEX idx_status (status),
    INDEX idx_game_date (game_date),
    INDEX idx_venue_date (venue_id, game_date)
) ENGINE=InnoDB;

-- ============================================================================
-- TABLE: notifications
-- ============================================================================
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    user_type ENUM('FAN', 'VENUE_MANAGER') NOT NULL,
    type ENUM('BOOKING_REQUESTED', 'BOOKING_APPROVED', 'BOOKING_REJECTED', 'BOOKING_CANCELLED') NOT NULL,
    message TEXT NOT NULL,
    related_booking_id INT DEFAULT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id) REFERENCES users(username)
        ON DELETE CASCADE,
    CONSTRAINT fk_notification_booking
        FOREIGN KEY (related_booking_id) REFERENCES bookings(id)
        ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_user_unread (user_id, is_read)
) ENGINE=InnoDB;

-- ============================================================================
-- END OF SCRIPT
-- ============================================================================