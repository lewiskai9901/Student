-- 预约座位分配表（物理删除，不使用逻辑删除）
CREATE TABLE IF NOT EXISTS booking_seat_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    position_no VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_booking (booking_id),
    INDEX idx_user (user_id),
    UNIQUE KEY uk_booking_position (booking_id, position_no)
);
