-- MIS terms of reference: three languages, structured address, doctor's conclusion,
-- diagnostic referrals, payments and receipts. Demo data follows in V5.

-- 1. Patients -----------------------------------------------------------------
ALTER TABLE patients
    ADD COLUMN middle_name VARCHAR(80) NULL AFTER last_name,
    ADD COLUMN address_city VARCHAR(100) NULL AFTER address,
    ADD COLUMN address_street VARCHAR(120) NULL AFTER address_city,
    ADD COLUMN address_house VARCHAR(20) NULL AFTER address_street,
    ADD COLUMN address_apartment VARCHAR(20) NULL AFTER address_house,
    ADD COLUMN preferred_lang VARCHAR(2) NOT NULL DEFAULT 'ru' AFTER address_apartment;

-- 2. Doctors ------------------------------------------------------------------
ALTER TABLE doctors
    ADD COLUMN middle_name VARCHAR(80) NULL AFTER last_name,
    ADD COLUMN bio_kk VARCHAR(1000) NULL AFTER bio,
    ADD COLUMN bio_en VARCHAR(1000) NULL AFTER bio_kk;

-- 3. Directories --------------------------------------------------------------
ALTER TABLE specializations
    ADD COLUMN name_kk VARCHAR(120) NULL AFTER name,
    ADD COLUMN name_en VARCHAR(120) NULL AFTER name_kk,
    ADD COLUMN description_kk VARCHAR(500) NULL AFTER description,
    ADD COLUMN description_en VARCHAR(500) NULL AFTER description_kk;

ALTER TABLE medical_services
    ADD COLUMN name_kk VARCHAR(150) NULL AFTER name,
    ADD COLUMN name_en VARCHAR(150) NULL AFTER name_kk,
    ADD COLUMN description_kk VARCHAR(500) NULL AFTER description,
    ADD COLUMN description_en VARCHAR(500) NULL AFTER description_kk,
    ADD COLUMN diagnostic BOOLEAN NOT NULL DEFAULT FALSE AFTER duration_minutes,
    ADD COLUMN office VARCHAR(30) NULL AFTER diagnostic,
    ADD COLUMN specialization_id BIGINT NULL AFTER office,
    ADD CONSTRAINT fk_medical_services_specialization
        FOREIGN KEY (specialization_id) REFERENCES specializations (id);

-- 4. Visits -------------------------------------------------------------------
ALTER TABLE appointments
    ADD COLUMN diagnosis VARCHAR(500) NULL AFTER notes,
    ADD COLUMN conclusion VARCHAR(2000) NULL AFTER diagnosis,
    ADD COLUMN recommendations VARCHAR(2000) NULL AFTER conclusion,
    ADD COLUMN payment_status VARCHAR(10) NOT NULL DEFAULT 'UNPAID' AFTER recommendations,
    ADD COLUMN paid_at DATETIME(6) NULL AFTER payment_status;

-- 5. Referrals ----------------------------------------------------------------
ALTER TABLE referrals
    ADD COLUMN service_id BIGINT NULL AFTER appointment_id,
    ADD COLUMN price DECIMAL(10,2) NULL AFTER service_id,
    ADD COLUMN payment_status VARCHAR(10) NOT NULL DEFAULT 'UNPAID' AFTER price,
    ADD COLUMN paid_at DATETIME(6) NULL AFTER payment_status,
    ADD CONSTRAINT fk_referrals_service FOREIGN KEY (service_id) REFERENCES medical_services (id);

-- 6. Payments -----------------------------------------------------------------
CREATE TABLE payments (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    number            VARCHAR(30)   NOT NULL,
    patient_id        BIGINT        NOT NULL,
    amount            DECIMAL(10,2) NOT NULL,
    method            VARCHAR(20)   NOT NULL,
    card_last4        VARCHAR(4)    NULL,
    gateway_reference VARCHAR(60)   NULL,
    paid_at           DATETIME(6)   NOT NULL,
    created_by        BIGINT        NOT NULL,
    email             VARCHAR(160)  NULL,
    email_status      VARCHAR(20)   NOT NULL,
    lang              VARCHAR(2)    NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_number UNIQUE (number),
    CONSTRAINT fk_payments_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    INDEX idx_payments_patient (patient_id, paid_at),
    INDEX idx_payments_paid_at (paid_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE payment_items (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    payment_id     BIGINT        NOT NULL,
    appointment_id BIGINT        NULL,
    referral_id    BIGINT        NULL,
    service_id     BIGINT        NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_items_appointment UNIQUE (appointment_id),
    CONSTRAINT uk_payment_items_referral UNIQUE (referral_id),
    CONSTRAINT fk_payment_items_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_payment_items_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_payment_items_referral FOREIGN KEY (referral_id) REFERENCES referrals (id),
    CONSTRAINT fk_payment_items_service FOREIGN KEY (service_id) REFERENCES medical_services (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
