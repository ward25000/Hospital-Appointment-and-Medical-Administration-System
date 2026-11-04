-- Hospital Appointment and Medical Administration System
-- Core schema: 7 related tables.
-- MySQL 8.0+, InnoDB, utf8mb4.

CREATE TABLE users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(60)  NOT NULL,
    password     VARCHAR(100) NOT NULL,
    full_name    VARCHAR(120) NOT NULL,
    role_name    VARCHAR(20)  NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE specializations (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_specializations_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE doctors (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    specialization_id BIGINT       NOT NULL,
    first_name        VARCHAR(80)  NOT NULL,
    last_name         VARCHAR(80)  NOT NULL,
    email             VARCHAR(160) NOT NULL,
    phone             VARCHAR(30)  NOT NULL,
    office            VARCHAR(30)  NOT NULL,
    bio               VARCHAR(1000) NULL,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_doctors_user UNIQUE (user_id),
    CONSTRAINT uk_doctors_email UNIQUE (email),
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_doctors_specialization FOREIGN KEY (specialization_id) REFERENCES specializations (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE doctor_schedules (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    doctor_id    BIGINT      NOT NULL,
    day_of_week  VARCHAR(10) NOT NULL,
    start_time   TIME(6)     NOT NULL,
    end_time     TIME(6)     NOT NULL,
    slot_minutes INT         NOT NULL DEFAULT 30,
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_doctor_schedules_day UNIQUE (doctor_id, day_of_week),
    CONSTRAINT fk_doctor_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE patients (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(80)  NOT NULL,
    last_name  VARCHAR(80)  NOT NULL,
    birth_date DATE         NOT NULL,
    gender     VARCHAR(10)  NOT NULL,
    phone      VARCHAR(30)  NOT NULL,
    email      VARCHAR(160) NULL,
    address    VARCHAR(300) NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_patients_name (last_name, first_name),
    INDEX idx_patients_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE medical_services (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    name             VARCHAR(150)  NOT NULL,
    description      VARCHAR(500)  NULL,
    price            DECIMAL(10,2) NOT NULL,
    duration_minutes INT           NOT NULL,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_medical_services_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE appointments (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    doctor_id           BIGINT        NOT NULL,
    patient_id          BIGINT        NOT NULL,
    service_id          BIGINT        NOT NULL,
    start_time          DATETIME(6)   NOT NULL,
    end_time            DATETIME(6)   NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    price               DECIMAL(10,2) NOT NULL,
    notes               VARCHAR(500)  NULL,
    cancellation_reason VARCHAR(500)  NULL,
    completed_at        DATETIME(6)   NULL,
    cancelled_at        DATETIME(6)   NULL,
    created_by          BIGINT        NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_appointments_doctor_slot_status UNIQUE (doctor_id, start_time, status),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES medical_services (id),
    CONSTRAINT fk_appointments_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_appointments_doctor_start (doctor_id, start_time),
    INDEX idx_appointments_patient_start (patient_id, start_time),
    INDEX idx_appointments_status_start (status, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
