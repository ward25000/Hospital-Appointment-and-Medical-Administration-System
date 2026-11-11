-- Demo data for the MySQL profile.
-- Passwords are BCrypt hashes for:
--   admin / Admin123!
--   reception / Reception123!
--   doctor.* / Doctor123!
-- Dates are relative to the day the migration is executed, so the calendar
-- and the dashboard stay meaningful in a freshly deployed demo.

INSERT INTO users (id, username, password, full_name, role_name, enabled, created_at, updated_at) VALUES
    (1, 'admin', '$2a$10$cb3.P3EkcHBOqpugPDSgqesOxtEEq3PqDx8OWPzboXIOASfsOHOi2',
     'Анна Викторовна Смирнова', 'ADMIN', TRUE, NOW(6), NOW(6)),
    (2, 'reception', '$2a$10$fVaYEeQtENkOdrmKJAAITeL80sGiMB6vd/m/NGcaj81DYsazg6bmO',
     'Ирина Александровна Ковалёва', 'RECEPTIONIST', TRUE, NOW(6), NOW(6)),
    (3, 'doctor.ivanov', '$2a$10$Kqy574mveuv4ufEe5RBs7u3EG9RoF2KX7KnDzcMpGelrBF7QUl/Gu',
     'Иванов Алексей Сергеевич', 'DOCTOR', TRUE, NOW(6), NOW(6)),
    (4, 'doctor.petrova', '$2a$10$Kqy574mveuv4ufEe5RBs7u3EG9RoF2KX7KnDzcMpGelrBF7QUl/Gu',
     'Петрова Мария Викторовна', 'DOCTOR', TRUE, NOW(6), NOW(6)),
    (5, 'doctor.smirnov', '$2a$10$Kqy574mveuv4ufEe5RBs7u3EG9RoF2KX7KnDzcMpGelrBF7QUl/Gu',
     'Смирнов Дмитрий Андреевич', 'DOCTOR', TRUE, NOW(6), NOW(6)),
    (6, 'doctor.kuznetsova', '$2a$10$Kqy574mveuv4ufEe5RBs7u3EG9RoF2KX7KnDzcMpGelrBF7QUl/Gu',
     'Кузнецова Ольга Игоревна', 'DOCTOR', TRUE, NOW(6), NOW(6));

INSERT INTO specializations (id, name, description, active, created_at, updated_at) VALUES
    (1, 'Кардиология', 'Диагностика и лечение заболеваний сердца и сосудов', TRUE, NOW(6), NOW(6)),
    (2, 'Неврология', 'Диагностика и лечение заболеваний нервной системы', TRUE, NOW(6), NOW(6)),
    (3, 'Педиатрия', 'Медицинская помощь детям и подросткам', TRUE, NOW(6), NOW(6)),
    (4, 'Терапия', 'Первичная медицинская помощь и профилактика', TRUE, NOW(6), NOW(6)),
    (5, 'Ортопедия', 'Заболевания и травмы опорно-двигательного аппарата', TRUE, NOW(6), NOW(6));

INSERT INTO doctors (id, user_id, specialization_id, first_name, last_name, email, phone, office, bio,
                     active, created_at, updated_at) VALUES
    (1, 3, 1, 'Алексей', 'Иванов', 'a.ivanov@medcenter.ru', '+7 (495) 120-20-01', '201',
     'Кардиолог высшей категории. Стаж 18 лет. Специализируется на диагностике ишемической болезни сердца и нарушениях ритма.',
     TRUE, NOW(6), NOW(6)),
    (2, 4, 2, 'Мария', 'Петрова', 'm.petrova@medcenter.ru', '+7 (495) 120-20-02', '305',
     'Невролог, кандидат медицинских наук. Ведёт пациентов с мигренью, нейропатиями и последствиями травм.',
     TRUE, NOW(6), NOW(6)),
    (3, 5, 3, 'Дмитрий', 'Смирнов', 'd.smirnov@medcenter.ru', '+7 (495) 120-20-03', '102',
     'Педиатр. Наблюдает детей с рождения до 18 лет, консультирует по вопросам вакцинации и профилактических осмотров.',
     TRUE, NOW(6), NOW(6)),
    (4, 6, 4, 'Ольга', 'Кузнецова', 'o.kuznetsova@medcenter.ru', '+7 (495) 120-20-04', '118',
     'Терапевт. Первичная диагностика, ведение хронических заболеваний, профилактические программы.',
     TRUE, NOW(6), NOW(6));

INSERT INTO doctor_schedules (id, doctor_id, day_of_week, start_time, end_time, slot_minutes, active,
                              created_at, updated_at) VALUES
    (1, 1, 'MONDAY', '09:00:00', '17:00:00', 30, TRUE, NOW(6), NOW(6)),
    (2, 1, 'TUESDAY', '09:00:00', '17:00:00', 30, TRUE, NOW(6), NOW(6)),
    (3, 1, 'WEDNESDAY', '09:00:00', '17:00:00', 30, TRUE, NOW(6), NOW(6)),
    (4, 1, 'THURSDAY', '09:00:00', '17:00:00', 30, TRUE, NOW(6), NOW(6)),
    (5, 1, 'FRIDAY', '09:00:00', '17:00:00', 30, TRUE, NOW(6), NOW(6)),
    (6, 2, 'MONDAY', '08:00:00', '14:00:00', 30, TRUE, NOW(6), NOW(6)),
    (7, 2, 'WEDNESDAY', '08:00:00', '14:00:00', 30, TRUE, NOW(6), NOW(6)),
    (8, 2, 'FRIDAY', '08:00:00', '14:00:00', 30, TRUE, NOW(6), NOW(6)),
    (9, 2, 'SATURDAY', '08:00:00', '14:00:00', 30, TRUE, NOW(6), NOW(6)),
    (10, 3, 'TUESDAY', '10:00:00', '18:00:00', 30, TRUE, NOW(6), NOW(6)),
    (11, 3, 'WEDNESDAY', '10:00:00', '18:00:00', 30, TRUE, NOW(6), NOW(6)),
    (12, 3, 'THURSDAY', '10:00:00', '18:00:00', 30, TRUE, NOW(6), NOW(6)),
    (13, 3, 'FRIDAY', '10:00:00', '18:00:00', 30, TRUE, NOW(6), NOW(6)),
    (14, 3, 'SATURDAY', '10:00:00', '18:00:00', 30, TRUE, NOW(6), NOW(6)),
    (15, 3, 'SUNDAY', '10:00:00', '18:00:00', 30, TRUE, NOW(6), NOW(6)),
    (16, 4, 'MONDAY', '08:00:00', '16:00:00', 30, TRUE, NOW(6), NOW(6)),
    (17, 4, 'TUESDAY', '08:00:00', '16:00:00', 30, TRUE, NOW(6), NOW(6)),
    (18, 4, 'WEDNESDAY', '08:00:00', '16:00:00', 30, TRUE, NOW(6), NOW(6)),
    (19, 4, 'THURSDAY', '08:00:00', '16:00:00', 30, TRUE, NOW(6), NOW(6));

INSERT INTO patients (id, first_name, last_name, birth_date, gender, phone, email, address,
                      created_at, updated_at) VALUES
    (1, 'Анна', 'Соколова', '1990-04-12', 'FEMALE', '+7 (916) 100-10-01', 'a.sokolova@example.com', 'Москва, ул. Лесная, 10', NOW(6), NOW(6)),
    (2, 'Михаил', 'Орлов', '1985-09-23', 'MALE', '+7 (916) 100-10-02', 'm.orlov@example.com', 'Москва, ул. Мира, 24', NOW(6), NOW(6)),
    (3, 'Елена', 'Волкова', '1978-01-30', 'FEMALE', '+7 (916) 100-10-03', 'e.volkova@example.com', 'Москва, ул. Садовая, 7', NOW(6), NOW(6)),
    (4, 'Артём', 'Лебедев', '1998-06-17', 'MALE', '+7 (916) 100-10-04', 'a.lebedev@example.com', 'Москва, пр-т Вернадского, 41', NOW(6), NOW(6)),
    (5, 'Ольга', 'Морозова', '1992-12-05', 'FEMALE', '+7 (916) 100-10-05', 'o.morozova@example.com', 'Москва, ул. Полярная, 15', NOW(6), NOW(6)),
    (6, 'Сергей', 'Новиков', '1969-03-14', 'MALE', '+7 (916) 100-10-06', 's.novikov@example.com', 'Москва, ул. Академика Королёва, 8', NOW(6), NOW(6)),
    (7, 'Дарья', 'Зайцева', '2001-07-21', 'FEMALE', '+7 (916) 100-10-07', 'd.zaitseva@example.com', 'Москва, ул. Тверская, 19', NOW(6), NOW(6)),
    (8, 'Павел', 'Егоров', '1988-11-02', 'MALE', '+7 (916) 100-10-08', 'p.egorov@example.com', 'Москва, ул. Гагарина, 33', NOW(6), NOW(6)),
    (9, 'Ирина', 'Павлова', '1975-05-09', 'FEMALE', '+7 (916) 100-10-09', 'i.pavlova@example.com', 'Москва, ул. Никольская, 5', NOW(6), NOW(6)),
    (10, 'Никита', 'Козлов', '2014-08-19', 'MALE', '+7 (916) 100-10-10', 'n.kozlov@example.com', 'Москва, ул. Строителей, 12', NOW(6), NOW(6)),
    (11, 'София', 'Белова', '2018-02-27', 'FEMALE', '+7 (916) 100-10-11', 's.belova@example.com', 'Москва, ул. Молодёжная, 3', NOW(6), NOW(6)),
    (12, 'Виктор', 'Громов', '1962-10-11', 'MALE', '+7 (916) 100-10-12', 'v.gromov@example.com', 'Москва, ул. Ветеранская, 21', NOW(6), NOW(6));

INSERT INTO medical_services (id, name, description, price, duration_minutes, active,
                              created_at, updated_at) VALUES
    (1, 'Первичная консультация врача', 'Осмотр, сбор анамнеза и план обследования', 2500.00, 30, TRUE, NOW(6), NOW(6)),
    (2, 'Повторная консультация', 'Контрольный приём и корректировка лечения', 1800.00, 30, TRUE, NOW(6), NOW(6)),
    (3, 'ЭКГ', 'Электрокардиография в 12 отведениях с расшифровкой', 1200.00, 30, TRUE, NOW(6), NOW(6)),
    (4, 'УЗИ сердца', 'Эхокардиография с допплерографией', 3200.00, 45, TRUE, NOW(6), NOW(6)),
    (5, 'Общий анализ крови', 'Клинический анализ крови с лейкоцитарной формулой', 900.00, 15, TRUE, NOW(6), NOW(6)),
    (6, 'Неврологический осмотр', 'Оценка рефлексов, чувствительности и неврологического статуса', 2200.00, 30, TRUE, NOW(6), NOW(6)),
    (7, 'Детский профилактический осмотр', 'Комплексный осмотр ребёнка и рекомендации по развитию', 2000.00, 30, TRUE, NOW(6), NOW(6)),
    (8, 'Внутрисуставная инъекция', 'Лечебная инъекция под контролем специалиста', 3500.00, 45, TRUE, NOW(6), NOW(6));

-- Appointments are generated from a compact list of desired slots.
-- For every slot the query picks the first doctor whose active weekly
-- schedule fully covers the service duration. This keeps the demo valid
-- on any weekday, including weekends, and avoids appointments outside
-- working hours. Dates are relative to the migration date.
INSERT INTO appointments (doctor_id, patient_id, service_id, start_time, end_time, status, price, notes,
                          completed_at, cancelled_at, cancellation_reason, created_by, version,
                          created_at, updated_at)
SELECT
    d.id,
    s.patient_id,
    s.service_id,
    s.start_dt,
    DATE_ADD(s.start_dt, INTERVAL s.duration_minutes MINUTE) AS end_dt,
    s.status,
    s.price,
    s.notes,
    CASE WHEN s.status = 'COMPLETED'
         THEN DATE_ADD(s.start_dt, INTERVAL s.duration_minutes MINUTE) END,
    CASE WHEN s.status = 'CANCELLED' THEN NOW(6) END,
    s.cancellation_reason,
    1,
    0,
    NOW(6),
    NOW(6)
FROM (
    SELECT
        y.day_offset,
        y.patient_id,
        y.service_id,
        y.preferred_doctor_id,
        y.duration_minutes,
        y.price,
        y.notes,
        y.cancellation_reason,
        y.start_dt,
        CASE
            WHEN y.kind = 'TODAY'
                 AND NOW(6) >= DATE_ADD(y.start_dt, INTERVAL y.duration_minutes MINUTE)
                THEN 'COMPLETED'
            WHEN y.kind = 'TODAY' THEN 'SCHEDULED'
            ELSE y.kind
        END AS status
    FROM (
        SELECT
            x.day_offset,
            x.patient_id,
            x.service_id,
            x.preferred_doctor_id,
            x.notes,
            x.cancellation_reason,
            x.kind,
            TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL x.day_offset DAY), x.start_time) AS start_dt,
            ms.duration_minutes,
            ms.price
        FROM (
            -- preferred_doctor_id: 1 cardiologist, 2 neurologist,
            -- 3 pediatrician, 4 therapist. The query falls back to any
            -- other doctor when the preferred one is off that day.
            -- Today: four visits with a mix of statuses depending on the current time.
            SELECT 0 AS day_offset, CAST('10:00:00' AS TIME) AS start_time,
                   1 AS patient_id, 1 AS service_id, 4 AS preferred_doctor_id, 'TODAY' AS kind,
                   'Плановый приём' AS notes, CAST(NULL AS CHAR(500)) AS cancellation_reason
            UNION ALL SELECT 0, '11:30:00', 2, 2, 1, 'TODAY', 'Плановый приём', NULL
            UNION ALL SELECT 0, '13:00:00', 3, 3, 1, 'TODAY', 'Плановый приём', NULL
            UNION ALL SELECT 0, '15:00:00', 4, 4, 1, 'TODAY', 'Плановый приём', NULL

            -- Completed visits from the previous two weeks.
            UNION ALL SELECT -2, '10:00:00', 2, 2, 1, 'COMPLETED',
                   'Приём завершён, выданы рекомендации', NULL
            UNION ALL SELECT -4, '11:30:00', 3, 3, 1, 'COMPLETED',
                   'Приём завершён, выданы рекомендации', NULL
            UNION ALL SELECT -6, '15:00:00', 4, 4, 1, 'COMPLETED',
                   'Приём завершён, выданы рекомендации', NULL
            UNION ALL SELECT -9, '10:30:00', 5, 5, 4, 'COMPLETED',
                   'Приём завершён, выданы рекомендации', NULL
            UNION ALL SELECT -12, '13:00:00', 6, 6, 2, 'COMPLETED',
                   'Приём завершён, выданы рекомендации', NULL
            UNION ALL SELECT -16, '14:00:00', 7, 7, 3, 'COMPLETED',
                   'Приём завершён, выданы рекомендации', NULL

            -- Upcoming visits.
            UNION ALL SELECT 1, '10:00:00', 1, 3, 1, 'SCHEDULED',
                   'Запись создана регистратурой', NULL
            UNION ALL SELECT 2, '11:30:00', 2, 4, 1, 'SCHEDULED',
                   'Запись создана регистратурой', NULL
            UNION ALL SELECT 3, '14:00:00', 3, 5, 4, 'SCHEDULED',
                   'Запись создана регистратурой', NULL
            UNION ALL SELECT 5, '15:30:00', 4, 6, 2, 'SCHEDULED',
                   'Запись создана регистратурой', NULL
            UNION ALL SELECT 7, '10:00:00', 5, 7, 3, 'SCHEDULED',
                   'Запись создана регистратурой', NULL
            UNION ALL SELECT 10, '12:30:00', 6, 8, 4, 'SCHEDULED',
                   'Запись создана регистратурой', NULL

            -- Cancelled visits keep their history but release the time slot.
            UNION ALL SELECT -3, '12:00:00', 7, 3, 1, 'CANCELLED',
                   'Пациент перенёс визит', 'Перенос по инициативе пациента'
            UNION ALL SELECT 4, '13:00:00', 8, 5, 4, 'CANCELLED',
                   NULL, 'Пациент отменил запись'
        ) x
        JOIN medical_services ms ON ms.id = x.service_id
    ) y
) s
JOIN doctors d ON d.id = (
    SELECT ds.doctor_id
    FROM doctor_schedules ds
    WHERE ds.active = TRUE
      AND ds.day_of_week = ELT(WEEKDAY(s.start_dt) + 1,
            'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
      AND ds.start_time <= TIME(s.start_dt)
      AND ds.end_time >= TIME(DATE_ADD(s.start_dt, INTERVAL s.duration_minutes MINUTE))
    ORDER BY CASE WHEN ds.doctor_id = s.preferred_doctor_id THEN 0 ELSE 1 END,
             ds.doctor_id
    LIMIT 1
)
WHERE d.active = TRUE;
