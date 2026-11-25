-- Translations of the seeded directories and demo data for Kazakhstan.
-- Every statement touches only untouched demo rows, so edited data stays as is.

-- 1. Translations of the seeded directories -------------------------------------
UPDATE specializations SET name_kk = 'Кардиология', name_en = 'Cardiology',
    description_kk = 'Жүрек және қан тамырлары ауруларын диагностикалау және емдеу',
    description_en = 'Diagnosis and treatment of heart and vascular diseases' WHERE name = 'Кардиология';
UPDATE specializations SET name_kk = 'Неврология', name_en = 'Neurology',
    description_kk = 'Жүйке жүйесі ауруларын диагностикалау және емдеу',
    description_en = 'Diagnosis and treatment of nervous system diseases' WHERE name = 'Неврология';
UPDATE specializations SET name_kk = 'Педиатрия', name_en = 'Pediatrics',
    description_kk = 'Балалар мен жасөспірімдерге медициналық көмек',
    description_en = 'Medical care for children and adolescents' WHERE name = 'Педиатрия';
UPDATE specializations SET name_kk = 'Терапия', name_en = 'Internal medicine',
    description_kk = 'Алғашқы медициналық көмек және профилактика',
    description_en = 'Primary care and prevention' WHERE name = 'Терапия';
UPDATE specializations SET name_kk = 'Ортопедия', name_en = 'Orthopedics',
    description_kk = 'Тірек-қимыл аппаратының аурулары мен жарақаттары',
    description_en = 'Diseases and injuries of the musculoskeletal system' WHERE name = 'Ортопедия';

UPDATE doctors SET middle_name = 'Сергеевич',
    bio_kk = 'Жоғары санатты кардиолог. Еңбек өтілі 18 жыл. Жүректің ишемиялық ауруын және ырғақ бұзылыстарын диагностикалауға маманданған.',
    bio_en = 'Cardiologist of the highest category with 18 years of experience. Specialises in coronary heart disease and rhythm disorders.'
    WHERE id = 1 AND last_name = 'Иванов';
UPDATE doctors SET middle_name = 'Викторовна',
    bio_kk = 'Невролог, медицина ғылымдарының кандидаты. Бас сақинасы, нейропатия және жарақат салдары бар пациенттерді қабылдайды.',
    bio_en = 'Neurologist, PhD. Treats migraine, neuropathies and the consequences of injuries.'
    WHERE id = 2 AND last_name = 'Петрова';
UPDATE doctors SET middle_name = 'Андреевич',
    bio_kk = 'Педиатр. Балаларды туғаннан 18 жасқа дейін бақылайды, вакцинация және профилактикалық тексерулер бойынша кеңес береді.',
    bio_en = 'Pediatrician. Follows children from birth to 18, advises on vaccination and preventive check-ups.'
    WHERE id = 3 AND last_name = 'Смирнов';
UPDATE doctors SET middle_name = 'Игоревна',
    bio_kk = 'Терапевт. Алғашқы диагностика, созылмалы ауруларды бақылау, профилактикалық бағдарламалар.',
    bio_en = 'Physician. Primary diagnosis, management of chronic diseases, prevention programmes.'
    WHERE id = 4 AND last_name = 'Кузнецова';
UPDATE doctors SET phone = REPLACE(phone, '+7 (495) 120-20-', '+7 (727) 120-20-')
    WHERE phone LIKE '+7 (495) 120-20-%';
UPDATE doctors SET email = REPLACE(email, '@medcenter.ru', '@medflow.kz') WHERE email LIKE '%@medcenter.ru';

-- Prices in tenge. Appointments keep their own snapshot, so both are converted together.
UPDATE appointments a JOIN medical_services s ON s.id = a.service_id AND s.price = a.price
SET a.price = CASE s.name
        WHEN 'Первичная консультация врача' THEN 9000
        WHEN 'Повторная консультация' THEN 6000
        WHEN 'ЭКГ' THEN 4500
        WHEN 'УЗИ сердца' THEN 14000
        WHEN 'Общий анализ крови' THEN 2800
        WHEN 'Неврологический осмотр' THEN 8000
        WHEN 'Детский профилактический осмотр' THEN 7500
        WHEN 'Внутрисуставная инъекция' THEN 12000
        ELSE a.price END
WHERE (s.name, s.price) IN (('Первичная консультация врача', 2500), ('Повторная консультация', 1800),
    ('ЭКГ', 1200), ('УЗИ сердца', 3200), ('Общий анализ крови', 900), ('Неврологический осмотр', 2200),
    ('Детский профилактический осмотр', 2000), ('Внутрисуставная инъекция', 3500));

UPDATE medical_services SET price = 9000 WHERE name = 'Первичная консультация врача' AND price = 2500;
UPDATE medical_services SET price = 6000 WHERE name = 'Повторная консультация' AND price = 1800;
UPDATE medical_services SET price = 4500 WHERE name = 'ЭКГ' AND price = 1200;
UPDATE medical_services SET price = 14000 WHERE name = 'УЗИ сердца' AND price = 3200;
UPDATE medical_services SET price = 2800 WHERE name = 'Общий анализ крови' AND price = 900;
UPDATE medical_services SET price = 8000 WHERE name = 'Неврологический осмотр' AND price = 2200;
UPDATE medical_services SET price = 7500 WHERE name = 'Детский профилактический осмотр' AND price = 2000;
UPDATE medical_services SET price = 12000 WHERE name = 'Внутрисуставная инъекция' AND price = 3500;

UPDATE medical_services SET name_kk = 'Дәрігердің алғашқы кеңесі', name_en = 'Initial consultation',
    description_kk = 'Қарап-тексеру, анамнез жинау және тексеру жоспары',
    description_en = 'Examination, history taking and a diagnostic plan' WHERE name = 'Первичная консультация врача';
UPDATE medical_services SET name_kk = 'Қайта кеңес алу', name_en = 'Follow-up consultation',
    description_kk = 'Бақылау қабылдауы және емді түзету',
    description_en = 'Follow-up visit and treatment adjustment' WHERE name = 'Повторная консультация';
UPDATE medical_services SET name_kk = 'ЭКГ', name_en = 'ECG',
    description_kk = '12 тармақтағы электрокардиография, қорытындысымен',
    description_en = '12-lead electrocardiography with interpretation',
    diagnostic = TRUE, office = '210',
    specialization_id = (SELECT id FROM specializations WHERE name = 'Кардиология') WHERE name = 'ЭКГ';
UPDATE medical_services SET name_kk = 'Жүректің УДЗ', name_en = 'Echocardiography',
    description_kk = 'Допплерографиямен эхокардиография',
    description_en = 'Heart ultrasound with Doppler imaging',
    diagnostic = TRUE, office = '212',
    specialization_id = (SELECT id FROM specializations WHERE name = 'Кардиология') WHERE name = 'УЗИ сердца';
UPDATE medical_services SET name_kk = 'Жалпы қан талдауы', name_en = 'Complete blood count',
    description_kk = 'Лейкоцитарлық формуласымен клиникалық қан талдауы',
    description_en = 'Clinical blood test with white cell differential',
    diagnostic = TRUE, office = '115',
    specialization_id = (SELECT id FROM specializations WHERE name = 'Терапия') WHERE name = 'Общий анализ крови';
UPDATE medical_services SET name_kk = 'Неврологиялық тексеру', name_en = 'Neurological examination',
    description_kk = 'Рефлекстерді, сезімталдықты және неврологиялық мәртебені бағалау',
    description_en = 'Assessment of reflexes, sensitivity and neurological status' WHERE name = 'Неврологический осмотр';
UPDATE medical_services SET name_kk = 'Балаларды профилактикалық тексеру', name_en = 'Child preventive check-up',
    description_kk = 'Баланы кешенді тексеру және даму бойынша ұсыныстар',
    description_en = 'Comprehensive examination of a child and development advice' WHERE name = 'Детский профилактический осмотр';
UPDATE medical_services SET name_kk = 'Буынішілік инъекция', name_en = 'Intra-articular injection',
    description_kk = 'Маманның бақылауымен емдік инъекция',
    description_en = 'Therapeutic injection under specialist control' WHERE name = 'Внутрисуставная инъекция';

-- 2. Demo patients: valid IIN, Kazakhstan phones and addresses -------------------
UPDATE patients SET iin = CASE id
        WHEN 1 THEN '900412400015' WHEN 2 THEN '850923300027' WHEN 3 THEN '780130400038'
        WHEN 4 THEN '980617300049' WHEN 5 THEN '921205400050' WHEN 6 THEN '690314300061'
        WHEN 7 THEN '010721600072' WHEN 8 THEN '881102300083' WHEN 9 THEN '750509400094'
        WHEN 10 THEN '140819500105' WHEN 11 THEN '180227600116' WHEN 12 THEN '621011300127'
        ELSE iin END
WHERE id <= 12 AND CAST(iin AS UNSIGNED) = id;

UPDATE patients SET phone = REPLACE(phone, '+7 (916) 100-10-', '+7 (701) 100-10-')
    WHERE phone LIKE '+7 (916) 100-10-%';

UPDATE patients SET
    address_city = 'Алматы',
    address_street = CASE id WHEN 1 THEN 'ул. Абая' WHEN 2 THEN 'ул. Толе би' WHEN 3 THEN 'пр. Назарбаева'
        WHEN 4 THEN 'ул. Сатпаева' WHEN 5 THEN 'ул. Жандосова' WHEN 6 THEN 'ул. Тимирязева'
        WHEN 7 THEN 'пр. Достык' WHEN 8 THEN 'ул. Розыбакиева' WHEN 9 THEN 'ул. Байтурсынова'
        WHEN 10 THEN 'ул. Гоголя' WHEN 11 THEN 'ул. Кабанбай батыра' ELSE 'пр. Аль-Фараби' END,
    address_house = CASE id WHEN 1 THEN '10' WHEN 2 THEN '24' WHEN 3 THEN '7' WHEN 4 THEN '41'
        WHEN 5 THEN '15' WHEN 6 THEN '8' WHEN 7 THEN '19' WHEN 8 THEN '33' WHEN 9 THEN '5'
        WHEN 10 THEN '12' WHEN 11 THEN '3' ELSE '21' END,
    address_apartment = CASE WHEN id % 3 = 0 THEN NULL ELSE CAST(id * 4 AS CHAR) END
WHERE id <= 12 AND address LIKE 'Москва, %';

UPDATE patients SET address = CONCAT(address_city, ', ', address_street, ', ', address_house,
        CASE WHEN address_apartment IS NULL THEN '' ELSE CONCAT(', кв. ', address_apartment) END)
WHERE id <= 12 AND address LIKE 'Москва, %' AND address_city IS NOT NULL;

-- Other cards written as "city, street, house" become structured too.
UPDATE patients SET
    address_city = TRIM(SUBSTRING_INDEX(address, ',', 1)),
    address_street = TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(address, ',', 2), ',', -1)),
    address_house = TRIM(SUBSTRING_INDEX(address, ',', -1))
WHERE address_city IS NULL
  AND CHAR_LENGTH(address) - CHAR_LENGTH(REPLACE(address, ',', '')) = 2
  AND CHAR_LENGTH(TRIM(SUBSTRING_INDEX(address, ',', -1))) BETWEEN 1 AND 20
  AND CHAR_LENGTH(TRIM(SUBSTRING_INDEX(address, ',', 1))) BETWEEN 1 AND 100;

-- 3. Demo visits ----------------------------------------------------------------
UPDATE appointments SET
    diagnosis = 'Z00.0 Общий медицинский осмотр',
    conclusion = 'Состояние удовлетворительное. Жалоб на момент осмотра нет, показатели в пределах возрастной нормы.',
    recommendations = 'Соблюдать режим труда и отдыха, повторный осмотр через 6 месяцев.'
WHERE status = 'COMPLETED' AND conclusion IS NULL;

-- Friday of the previous week: a finished visit and one still waiting for the doctor's mark.
INSERT INTO appointments (doctor_id, patient_id, service_id, start_time, end_time, status, price, notes,
                          diagnosis, conclusion, recommendations, completed_at, created_by, version,
                          created_at, updated_at)
SELECT 1, 1, 1,
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) + 3 DAY), '09:30:00'),
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) + 3 DAY), '10:00:00'),
       'COMPLETED', 9000, 'Жалобы на головную боль и повышение давления',
       'I10 Артериальная гипертензия, I степень',
       'АД 145/90 мм рт. ст., пульс 78 в минуту. Тоны сердца ясные, ритм правильный. Очаговой неврологической симптоматики нет.',
       'Контроль АД два раза в день, ограничение соли, консультация невролога.',
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) + 3 DAY), '10:00:00'),
       1, 0, NOW(6), NOW(6)
FROM patients p
WHERE p.id = 1 AND EXISTS (SELECT 1 FROM doctors WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM appointments x WHERE x.doctor_id = 1 AND x.status <> 'CANCELLED'
      AND x.start_time < TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) + 3 DAY), '10:00:00')
      AND x.end_time > TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) + 3 DAY), '09:30:00'));

INSERT INTO appointments (doctor_id, patient_id, service_id, start_time, end_time, status, price, notes,
                          created_by, version, created_at, updated_at)
SELECT 1, 9, 2,
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) + 3 DAY), '16:00:00'),
       TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) + 3 DAY), '16:30:00'),
       'SCHEDULED', 6000, 'Контрольный приём', 1, 0, NOW(6), NOW(6)
FROM patients p
WHERE p.id = 9 AND EXISTS (SELECT 1 FROM doctors WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM appointments x WHERE x.doctor_id = 1 AND x.status <> 'CANCELLED'
      AND x.start_time < TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) + 3 DAY), '16:30:00')
      AND x.end_time > TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) + 3 DAY), '16:00:00'));

-- Seeded referrals get a service so that they can be paid.
UPDATE referrals r
    JOIN medical_services s ON s.name = 'Неврологический осмотр'
SET r.service_id = s.id, r.price = s.price,
    r.appointment_id = COALESCE((SELECT MIN(a.id) FROM appointments a
        WHERE a.patient_id = 1 AND a.doctor_id = 1 AND a.status = 'COMPLETED'), r.appointment_id)
WHERE r.patient_id = 1 AND r.to_doctor_id = 2 AND r.service_id IS NULL;

UPDATE referrals r
    JOIN medical_services s ON s.name = 'ЭКГ'
SET r.service_id = s.id, r.price = s.price, r.office = s.office
WHERE r.patient_id = 2 AND r.from_doctor_id = 4 AND r.service_id IS NULL;

UPDATE users u JOIN patients p ON p.user_id = u.id
SET u.full_name = CONCAT(p.last_name, ' ', p.first_name);
