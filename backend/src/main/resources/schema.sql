CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(100) UNIQUE,
    avatar VARCHAR(255),
    role TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    breed VARCHAR(50),
    age INT,
    gender TINYINT,
    weight DECIMAL(5,2),
    color VARCHAR(30),
    description TEXT,
    image VARCHAR(255),
    status TINYINT DEFAULT 0,
    health_status VARCHAR(50) DEFAULT 'healthy',
    adopt_fee DECIMAL(10,2) DEFAULT 0,
    shelter VARCHAR(100),
    source_name VARCHAR(100),
    source_url VARCHAR(500),
    source_checked_at DATE,
    image_source_name VARCHAR(100),
    image_source_url VARCHAR(500),
    source_note VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS adoption_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(200) NOT NULL,
    work_status VARCHAR(50),
    housing_type VARCHAR(50),
    has_other_pet TINYINT DEFAULT 0,
    experience VARCHAR(200),
    reason TEXT,
    status TINYINT DEFAULT 0,
    reject_reason TEXT,
    adopted_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (pet_id) REFERENCES pet(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS health_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    weight DECIMAL(5,2),
    temperature DECIMAL(4,1),
    symptom VARCHAR(200),
    diagnosis VARCHAR(200),
    treatment VARCHAR(500),
    veterinarian VARCHAR(50),
    hospital VARCHAR(100),
    cost DECIMAL(10,2),
    notes TEXT,
    source_name VARCHAR(100),
    source_url VARCHAR(500),
    source_note VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pet(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS vaccine_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id BIGINT NOT NULL,
    vaccine_name VARCHAR(100) NOT NULL,
    vaccine_type VARCHAR(50),
    dose VARCHAR(20),
    vaccinate_date DATE NOT NULL,
    next_date DATE,
    hospital VARCHAR(100),
    doctor VARCHAR(50),
    notes TEXT,
    source_name VARCHAR(100),
    source_url VARCHAR(500),
    source_note VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pet_id) REFERENCES pet(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(50),
    source_name VARCHAR(100),
    source_url VARCHAR(500),
    type VARCHAR(20) DEFAULT 'notice',
    status TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
