CREATE TABLE user_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    imap_host VARCHAR(255) NOT NULL,
    imap_port INT NOT NULL,
    imap_username VARCHAR(255) NOT NULL,
    imap_password VARCHAR(255) NOT NULL,
    last_fetch_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE email (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_config_id BIGINT NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    from_address VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    sent_date TIMESTAMP NOT NULL,
    content TEXT,
    cleaned_content TEXT,
    category VARCHAR(50),
    summary TEXT,
    is_processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_config_id) REFERENCES user_config(id)
);

CREATE TABLE process_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_config_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    processed_count INT DEFAULT 0,
    FOREIGN KEY (user_config_id) REFERENCES user_config(id)
);

CREATE TABLE process_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_config_id BIGINT NOT NULL,
    current_state VARCHAR(50) NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    lock_owner VARCHAR(255),
    lock_time TIMESTAMP,
    FOREIGN KEY (user_config_id) REFERENCES user_config(id)
);