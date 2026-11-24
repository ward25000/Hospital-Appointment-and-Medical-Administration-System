# MedFlow — шпаргалка по MySQL

Один файл со всеми командами, которые реально нужны: посмотреть базу, найти пользователей,
сбросить пароль, проверить записи, сделать бэкап. Всё можно копировать как есть.

---

## 0. Главное правило (из-за него была ошибка `Unknown MySQL server host '127'`)

В PowerShell **нельзя** писать `-h127.0.0.1` — PowerShell «откусывает» часть и получается `-h127`.
Пиши длинно: `--host=127.0.0.1`.

| Неправильно | Правильно |
|---|---|
| `-h127.0.0.1` | `--host=127.0.0.1` |
| `-uroot` | `--user=root` |
| `-p` (спросит пароль) | `--password=...` или `$env:MYSQL_PWD` |

---

## 1. Как подключиться

### Вариант А — двойной клик (самый простой)

- `show-db.cmd` — готовый обзор базы (таблицы, строки, миграции, индексы)
- `open-mysql.cmd` — живая консоль `mysql>`, где можно вводить любые команды из этого файла
- `open-db-gui.cmd` — HeidiSQL (красивый вид, если установлен)

### Вариант Б — одна команда в PowerShell

```powershell
& "$env:USERPROFILE\mysql84\bin\mysql.exe" --protocol=tcp --host=127.0.0.1 --port=3306 --user=hospital --password=hospital_pass --default-character-set=utf8mb4 --database=hospital_db -e "SHOW TABLES;"
```

### Вариант В — через пользователя приложения (не root)

```powershell
$env:MYSQL_PWD = "hospital_pass"
& "$env:USERPROFILE\mysql84\bin\mysql.exe" --protocol=tcp --host=127.0.0.1 --port=3306 --user=hospital --default-character-set=utf8mb4 --database=hospital_db -e "SELECT COUNT(*) FROM appointments;"
Remove-Item Env:\MYSQL_PWD
```

> Чтобы не повторять длинный путь каждый раз, можно один раз сделать функцию:
>
> ```powershell
> function sql { & "$env:USERPROFILE\mysql84\bin\mysql.exe" --protocol=tcp --host=127.0.0.1 --port=3306 --user=hospital --password=hospital_pass --default-character-set=utf8mb4 --database=hospital_db -e $args[0] }
> ```
>
> Дальше просто: `sql "SHOW TABLES;"`.

### Данные для подключения (для HeidiSQL / DBeaver / другого клиента)

```text
Хост:         127.0.0.1
Порт:         3306
Пользователь: hospital
Пароль:       hospital_pass
База:         hospital_db
```

---

## 2. Быстрый осмотр базы

```sql
SHOW DATABASES;
SHOW TABLES;
SELECT COUNT(*) AS tables FROM information_schema.tables WHERE table_schema = 'hospital_db';

-- сколько строк в каждой таблице (одним запросом)
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = 'hospital_db'
ORDER BY table_name;

-- структура конкретной таблицы
DESCRIBE appointments;

-- индексы и уникальные ключи
SHOW INDEX FROM appointments;
```

---

## 3. Пользователи, роли и пароли

### Кто вообще есть в системе

```sql
SELECT id, username, role_name, full_name, enabled
FROM users
ORDER BY id;
```

### Пароли: почему их «не видно»

В таблице `users` в колонке `password` лежит **BCrypt-хеш**, а не пароль:

```text
$2a$10$cb3.P3EkcHBOqpugPDSgqesOxtEEq3PqDx8OWPzboXIOASfsOHOi2
```

Это одностороннее шифрование: из хеша пароль восстановить нельзя (в этом и смысл).
Поэтому «узнать» пароль нельзя, но **можно сбросить** на новый — см. раздел 4.

### Демо-аккаунты (пароли заданы в миграции V2/V3)

| Логин | Пароль | Роль |
|---|---|---|
| `admin` | `Admin123!` | ADMIN |
| `reception` | `Reception123!` | RECEPTIONIST |
| `doctor.ivanov` | `Doctor123!` | DOCTOR |
| `doctor.petrova` | `Doctor123!` | DOCTOR |
| `doctor.smirnov` | `Doctor123!` | DOCTOR |
| `doctor.kuznetsova` | `Doctor123!` | DOCTOR |
| `patient.demo` | `Patient123!` | PATIENT |

Все врачские аккаунты используют **один и тот же** пароль `Doctor123!` — в базе у них
одинаковый хеш (`$2a$10$Kqy574mveuv4ufEe5RBs7u3EG9RoF2KX7KnDzcMpGelrBF7QUl/Gu`).

### Аккаунты, созданные через сайт (регистрация пациента / добавление врача)

Их пароль знает только тот, кто создавал: при регистрации пароль вводится вручную и сразу
превращается в хеш. Пример — пользователь `Arshagul`: пароль неизвестен, его нужно сбросить.

Проверить, какие аккаунты появились после миграций (созданы руками):

```sql
SELECT id, username, role_name, created_at
FROM users
WHERE id > 7
ORDER BY id;
```

### Какой врач к какому логину привязан

```sql
SELECT d.id AS doctor_id,
       u.username,
       CONCAT(d.last_name, ' ', d.first_name, ' ', d.middle_name) AS fio,
       s.name AS specialization
FROM doctors d
JOIN users u ON u.id = d.user_id
JOIN specializations s ON s.id = d.specialization_id
ORDER BY d.id;
```

### Какие пациенты имеют логин в систему

```sql
SELECT p.id AS patient_id,
       p.iin,
       CONCAT(p.last_name, ' ', p.first_name) AS fio,
       u.username
FROM patients p
LEFT JOIN users u ON u.id = p.user_id
ORDER BY p.id;
```

`username = NULL` значит, что у пациента есть карточка, но нет входа в личный кабинет.

---

## 4. Сброс пароля

### Способ А — готовый скрипт (рекомендую)

```powershell
.\set-password.cmd doctor.petrova NewPass123!
```

Или без `.cmd`:

```powershell
powershell -ExecutionPolicy Bypass -File .\set-password.ps1 -Username doctor.petrova -Password "NewPass123!"
```

Скрипт сам сделает BCrypt-хеш (тем же алгоритмом, что и приложение) и обновит запись в базе.
Потом просто войди в браузере с новым паролем.

**Проверить, что пароль подходит:**

```powershell
powershell -ExecutionPolicy Bypass -File .\set-password.ps1 -Username doctor.petrova -Password "NewPass123!" -VerifyOnly
```

**Посмотреть список всех пользователей:**

```powershell
.\set-password.cmd -List
```

> ⚠️ Если после смены пароля вход всё равно даёт `401`, причина не в пароле, а в профиле:
> у пользователя с ролью `PATIENT` должна быть карточка в `patients` (колонка `user_id`),
> у роли `DOCTOR` — запись в `doctors`. Без профиля приложение не пускает в систему.
> Проверить: `SELECT * FROM patients WHERE user_id = 8;`

### Способ Б — скопировать хеш от известного аккаунта (чистый SQL)

Например, сделать всем врачам пароль `Doctor123!`:

```sql
UPDATE users
SET password = (SELECT p FROM (SELECT password AS p FROM users WHERE username = 'doctor.ivanov') t),
    updated_at = NOW(6)
WHERE role_name = 'DOCTOR';
```

Всем вообще — пароль как у `admin`:

```sql
UPDATE users
SET password = (SELECT p FROM (SELECT password AS p FROM users WHERE username = 'admin') t),
    updated_at = NOW(6)
WHERE username <> 'admin';
```

> Вложенный `SELECT ... FROM (SELECT ...) t` нужен потому, что MySQL не разрешает
> читать из той же таблицы, которую обновляешь, в простом подзапросе.

Пароли, которые дают эти хеши:

| Хеш взят у | Получившийся пароль |
|---|---|
| `admin` | `Admin123!` |
| `reception` | `Reception123!` |
| `doctor.ivanov` | `Doctor123!` |
| `patient.demo` | `Patient123!` |

### Способ В — вручную сгенерировать хеш

Нужен только JAR из локального Maven-репозитория (он уже скачан):

```powershell
$crypto = "$env:USERPROFILE\.m2\repository\org\springframework\security\spring-security-crypto\6.3.4\spring-security-crypto-6.3.4.jar"
$jcl    = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\springframework\spring-jcl" -Recurse -Filter *.jar | Select-Object -First 1
$js = "C:\Users\User\AppData\Local\Temp\opencode\hash.jsh"
Set-Content $js 'System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("MyNewPass1!"));' -Encoding ASCII
& "$env:LOCALAPPDATA\Programs\jdk-17\jdk-17.0.21+9\bin\jshell.exe" --class-path "$crypto;$($jcl.FullName)" -q $js
```

Скопируй строку `$2a$10$...` и подставь в SQL:

```sql
UPDATE users SET password = '$2a$10$ВСТАВЬ_СЮДА_ХЕШ', updated_at = NOW(6)
WHERE username = 'doctor.petrova';
```

### Забыл пароль root от MySQL

Для работы root не нужен: скрипты заходят под `hospital`. Пароль root пустой или `root_pass` (зависит от установки). Не подходит ни один — запусти `full-reset.cmd`. Если пароль когда-то задали:

```powershell
& "$env:USERPROFILE\mysql84\bin\mysql.exe" --protocol=tcp --host=127.0.0.1 --port=3306 --user=root --password -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '';"
```

(сработает, только если ты уже подключён без пароля).

---

## 5. Записи на приём (`appointments`)

```sql
-- сколько всего и по статусам
SELECT status, COUNT(*) AS cnt FROM appointments GROUP BY status;

-- последние 10 записей по-человечески
SELECT a.id,
       CONCAT(p.last_name, ' ', p.first_name)  AS patient,
       CONCAT(d.last_name, ' ', d.first_name)  AS doctor,
       a.start_time,
       a.end_time,
       a.status,
       a.price
FROM appointments a
JOIN patients p ON p.id = a.patient_id
JOIN doctors  d ON d.id = a.doctor_id
ORDER BY a.id DESC
LIMIT 10;

-- записи на сегодня
SELECT a.id, a.start_time, a.status, p.last_name AS patient, d.last_name AS doctor
FROM appointments a
JOIN patients p ON p.id = a.patient_id
JOIN doctors  d ON d.id = a.doctor_id
WHERE DATE(a.start_time) = CURDATE()
ORDER BY a.start_time;

-- записи конкретного врача
SELECT a.id, a.start_time, a.status, p.last_name AS patient
FROM appointments a
JOIN patients p ON p.id = a.patient_id
WHERE a.doctor_id = 1
ORDER BY a.start_time DESC;
```

### Проверить, что двойной записи нет

Уникальный индекс `uk_appointments_doctor_slot_status` не даёт записать двух пациентов
на один слот одного врача:

```sql
-- должно быть 0 строк
SELECT doctor_id, start_time, COUNT(*) AS cnt
FROM appointments
WHERE status = 'SCHEDULED'
GROUP BY doctor_id, start_time
HAVING cnt > 1;
```

```sql
-- посмотреть сам индекс
SHOW INDEX FROM appointments WHERE Key_name = 'uk_appointments_doctor_slot_status';
```

### Проверить, что записи не накладываются по времени

```sql
SELECT a1.id AS id1, a2.id AS id2, a1.doctor_id, a1.start_time, a2.end_time
FROM appointments a1
JOIN appointments a2
  ON a1.doctor_id = a2.doctor_id
 AND a1.id < a2.id
 AND a1.status = 'SCHEDULED' AND a2.status = 'SCHEDULED'
 AND a1.start_time < a2.end_time
 AND a2.start_time < a1.end_time;
```

---

## 6. Врачи, расписание, услуги

```sql
-- все врачи со специализацией
SELECT d.id, d.last_name, d.first_name, s.name AS specialization, d.office, d.active
FROM doctors d
JOIN specializations s ON s.id = d.specialization_id
ORDER BY d.id;

-- расписание по дням недели
SELECT ds.id, d.last_name AS doctor, ds.day_of_week, ds.start_time, ds.end_time,
       ds.slot_minutes, ds.active
FROM doctor_schedules ds
JOIN doctors d ON d.id = ds.doctor_id
ORDER BY d.id, FIELD(ds.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY');

-- услуги и цены
SELECT id, name, price, duration_minutes, active
FROM medical_services
ORDER BY id;

-- специализации
SELECT * FROM specializations ORDER BY id;
```

### Свободные слоты врача на конкретную дату

Занятые слоты врача №1 на 20 ноября 2026:

```sql
SELECT start_time, end_time, status
FROM appointments
WHERE doctor_id = 1
  AND start_time >= '2026-11-20 00:00:00'
  AND start_time <  '2026-11-21 00:00:00'
ORDER BY start_time;
```

Свободные слоты считает само приложение (кнопка «свободные слоты» в календаре),
в SQL это делать не нужно — проще посмотреть в интерфейсе.

---

## 7. Направления (`referrals`)

```sql
SELECT r.id,
       CONCAT(p.last_name, ' ', p.first_name) AS patient,
       r.department, r.office, r.status, r.created_at
FROM referrals r
JOIN patients p ON p.id = r.patient_id
ORDER BY r.id DESC;
```

---

## 8. Миграции Flyway

```sql
SELECT installed_rank, version, description, type, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

Ожидаемо три строки: `1 create schema`, `2 seed data`, `3 patient portal and referrals`.

---

## 9. Здоровье базы и ссылочная целостность

```sql
-- все внешние ключи
SELECT table_name, constraint_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema = 'hospital_db' AND referenced_table_name IS NOT NULL
ORDER BY table_name;

-- «осиротевшие» записи (должно быть 0)
SELECT COUNT(*) AS broken_appointments
FROM appointments a
LEFT JOIN patients p ON p.id = a.patient_id
LEFT JOIN doctors  d ON d.id = a.doctor_id
WHERE p.id IS NULL OR d.id IS NULL;

-- размер базы и таблиц
SELECT table_name,
       ROUND((data_length + index_length) / 1024, 1) AS kb
FROM information_schema.tables
WHERE table_schema = 'hospital_db'
ORDER BY (data_length + index_length) DESC;

-- кодировка (должна быть utf8mb4)
SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'hospital_db';
```

---

## 10. Бэкап и восстановление

### Сделать копию базы

```powershell
& "$env:USERPROFILE\mysql84\bin\mysqldump.exe" --protocol=tcp --host=127.0.0.1 --port=3306 --user=hospital --password=hospital_pass --default-character-set=utf8mb4 --databases hospital_db --result-file="$env:USERPROFILE\Desktop\hospital_db_backup.sql"
```

### Восстановить из копии

```powershell
Get-Content "$env:USERPROFILE\Desktop\hospital_db_backup.sql" -Raw | & "$env:USERPROFILE\mysql84\bin\mysql.exe" --protocol=tcp --host=127.0.0.1 --port=3306 --user=hospital --password=hospital_pass --default-character-set=utf8mb4
```

### Полный сброс демо-данных

Останавливает всё, удаляет `hospital_db`, при следующем запуске Flyway создаёт схему и данные заново
(даты в расписании будут относительно сегодняшнего дня):

```powershell
.\stop.cmd
.\reset-mysql-demo.cmd
.\start.cmd
```

> Внимание: все записи, созданные вручную, и все зарегистрированные пациенты пропадут.

---

## 11. Частые ошибки

| Сообщение | Причина | Решение |
|---|---|---|
| `Unknown MySQL server host '127'` | написано `-h127.0.0.1` | писать `--host=127.0.0.1` |
| `ERROR 2003 (HY000): Can't connect` | MySQL не запущен | `start.cmd` или `docker compose up -d` |
| `Access denied for user 'hospital'` | неверный пароль | пароль `hospital_pass`; если не подходит, `full-reset.cmd` |
| `Unknown database 'hospital_db'` | база ещё не создана | запустить `start.cmd` (Flyway создаст) |
| `Duplicate entry ... uk_appointments_doctor_slot_status` | попытка записать второго пациента на тот же слот | это правильное поведение, выбирай другой слот |
| `Cannot add or update a child row: a foreign key constraint fails` | ссылка на несуществующую строку | сначала создай связанную запись |
| `401` после сброса пароля | у `PATIENT` нет карточки в `patients`, у `DOCTOR` — в `doctors` | создать профиль и привязать `user_id` |
| Кракозябры вместо русского текста | кодировка консоли | добавить `--default-character-set=utf8mb4`, `chcp 65001` |

---

## 12. Проверить, что всё работает (после любых правок в базе)

```powershell
.\verify-all.cmd
```

В конце должно быть:

```text
  1/3  Тесты Java (JUnit / MockMvc)                 33/33  [OK]
  2/3  Проверка программы (API, роли, CSRF, запись)  31/31  [OK]
  3/3  Проверка базы MySQL                          22/22  [OK]
  ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ
```
