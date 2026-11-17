-- Patient portal and electronic referrals.
-- Adds patient identity (IIN) and the link between a patient card and its
-- login, plus the referral table used by doctors during an appointment.

ALTER TABLE patients
    ADD COLUMN iin VARCHAR(12) NULL AFTER id,
    ADD COLUMN user_id BIGINT NULL AFTER iin;

UPDATE patients SET iin = LPAD(CAST(id AS CHAR), 12, '0') WHERE iin IS NULL;

ALTER TABLE patients
    MODIFY COLUMN iin VARCHAR(12) NOT NULL,
    MODIFY COLUMN address VARCHAR(300) NOT NULL;

ALTER TABLE patients
    ADD CONSTRAINT uk_patients_iin UNIQUE (iin),
    ADD CONSTRAINT uk_patients_user UNIQUE (user_id),
    ADD CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users (id);

CREATE TABLE referrals (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    patient_id           BIGINT        NOT NULL,
    from_doctor_id       BIGINT        NOT NULL,
    to_doctor_id         BIGINT        NULL,
    to_specialization_id BIGINT        NULL,
    appointment_id       BIGINT        NULL,
    department           VARCHAR(120)  NULL,
    office               VARCHAR(30)   NULL,
    reason               VARCHAR(500)  NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    decided_at           DATETIME(6)   NULL,
    created_at           DATETIME(6)   NOT NULL,
    updated_at           DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_referrals_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_referrals_from_doctor FOREIGN KEY (from_doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_referrals_to_doctor FOREIGN KEY (to_doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_referrals_to_specialization FOREIGN KEY (to_specialization_id) REFERENCES specializations (id),
    CONSTRAINT fk_referrals_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT ck_referrals_target CHECK (to_doctor_id IS NOT NULL OR to_specialization_id IS NOT NULL),
    INDEX idx_referrals_patient (patient_id),
    INDEX idx_referrals_to_doctor (to_doctor_id, status),
    INDEX idx_referrals_to_specialization (to_specialization_id, status),
    INDEX idx_referrals_from_doctor (from_doctor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Demo patient account: patient.demo / Patient123!
INSERT INTO users (id, username, password, full_name, role_name, enabled, created_at, updated_at) VALUES
    (7, 'patient.demo',
     '$2a$10$JrgxRIxrQNw1xZi4BbkKSO406ZlkTJF8ApK9ePvPwNaPxGxRq9Zwi',
     'Соколова Анна', 'PATIENT', TRUE, NOW(6), NOW(6));

UPDATE patients SET user_id = 7 WHERE id = 1;

-- Two sample referrals so the doctor inbox is not empty after the first run.
INSERT INTO referrals (patient_id, from_doctor_id, to_doctor_id, to_specialization_id,
                       appointment_id, department, office, reason, status, decided_at,
                       created_at, updated_at)
SELECT 2, 4, NULL, 1,
       (SELECT id FROM appointments WHERE patient_id = 2 ORDER BY id LIMIT 1),
       'Кардиология', '201', 'Повышенное артериальное давление, нужна консультация кардиолога',
       'PENDING', NULL, NOW(6), NOW(6);

INSERT INTO referrals (patient_id, from_doctor_id, to_doctor_id, to_specialization_id,
                       appointment_id, department, office, reason, status, decided_at,
                       created_at, updated_at)
SELECT 1, 1, 2, NULL,
       (SELECT id FROM appointments WHERE patient_id = 1 ORDER BY id LIMIT 1),
       'Неврология', '305', 'Головные боли, направление к неврологу',
       'PENDING', NULL, NOW(6), NOW(6);
