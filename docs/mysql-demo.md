# MySQL на защите без Docker

На машине нет Docker Desktop, а виртуализация в BIOS выключена, поэтому
Docker Desktop и `docker compose` здесь не запускаются. Для защиты развёрнут
**портативный MySQL 8.4.11**: обычный ZIP-архив, без установки, без службы
Windows и без прав администратора.

| Параметр | Значение |
|---|---|
| Каталог MySQL | `%USERPROFILE%\mysql84` |
| Версия | MySQL 8.4.11 |
| Порт | 3306 |
| База | `hospital_db` |
| Пользователь приложения | `hospital` / `hospital_pass` |
| root | `root` / `root_pass` |

Проект уже настроен на эти значения в
`src/main/resources/application-mysql.yml`, профиль `mysql` используется по
умолчанию. Docker-вариант из `docker-compose.yml` остаётся штатным и
эквивалентным: те же база, пользователь и пароль.

## 1. Запуск MySQL

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\mysql84\mysql-start.ps1"
```

Ожидаемый результат:

```text
MySQL is already running on port 3306 and ready.
```

или, при первом запуске:

```text
Initializing data directory (first run only)...
Starting MySQL on port 3306 ...
MySQL is ready: database hospital_db, user hospital
```

Проверка, что порт слушается:

```powershell
Get-NetTCPConnection -LocalPort 3306 -State Listen
```

## 2. Свежие демо-данные (по желанию)

Сид `V2__seed_data.sql` строит даты относительно дня миграции. Если база
создавалась давно, перед защитой её можно пересоздать. Приложение при этом
должно быть остановлено.

```powershell
powershell -ExecutionPolicy Bypass -File .\reset-mysql-demo.ps1
```

Скрипт удаляет и заново создаёт `hospital_db`. При следующем запуске Flyway
применит `V1` и `V2`, и даты записей снова будут вокруг сегодняшнего дня.

## 3. Запуск приложения на MySQL

```powershell
powershell -ExecutionPolicy Bypass -File .\run-demo.ps1
```

Скрипт сам:

1. поднимает MySQL, если он ещё не запущен;
2. освобождает порт 8080, если там остался старый экземпляр MedFlow;
3. запускает Spring Boot с часовым поясом `Europe/Moscow`, чтобы время в UI и
   в SQL совпадало.

В логе должны быть видны ключевые строки:

```text
HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@...
Database: jdbc:mysql://localhost:3306/hospital_db (MySQL 8.4)
Migrating schema `hospital_db` to version "1 - create schema"
Migrating schema `hospital_db` to version "2 - seed data"
Successfully applied 2 migrations to schema `hospital_db`, now at version v2
Tomcat started on port 8080 (http)
```

Предупреждение Flyway `MySQL 8.4 is newer than this version of Flyway` —
безобидное: Flyway 10.10 официально знает MySQL до 8.1, но миграции на 8.4
применяются успешно.

Приложение: <http://localhost:8080>

## 4. Автоматические проверки

В отдельном окне PowerShell, пока приложение работает:

```powershell
powershell -ExecutionPolicy Bypass -File .\verify-system.ps1
powershell -ExecutionPolicy Bypass -File .\verify-mysql.ps1
```

`verify-system.ps1` — 31 сквозная проверка REST API: роли, 401/403, CSRF,
слоты, создание записи, 409 при дубле, отмена, отчёты.

`verify-mysql.ps1` — 13 проверок MySQL: версия сервера, InnoDB/utf8mb4,
миграции Flyway, 8 таблиц, уникальный индекс
`doctor_id, start_time, status`, демо-данные, отсутствие пересечений,
отказ БД на дубль (`ERROR 1062`) и статус БД глазами приложения.

Ожидаемый итог:

```text
Passed: 31  Failed: 0
Passed: 13  Failed: 0
```

## 5. Как посмотреть саму базу данных

База данных — это сервер MySQL, а не файл, который открывается двойным
кликом. Смотреть её можно тремя способами.

### 5.1. HeidiSQL — окно с таблицами (проще всего)

Портативный HeidiSQL лежит в `%USERPROFILE%\HeidiSQL`, установка не нужна.
Открыть:

```powershell
.\open-db-gui.cmd
```

Слева дерево `hospital_db` → таблицы. Выберите `appointments` и откройте
вкладку **Данные** (Data) — это и есть строки записей. Вкладка **Структура**
(Structure) показывает колонки и индексы. Во вкладке **Запрос** (Query) можно
писать SQL и выполнять его клавишей `F9`.

Параметры подключения, если HeidiSQL спросит их вручную:

| Поле | Значение |
|---|---|
| Сеть / Network | MySQL (TCP/IP) |
| Хост / Host | `127.0.0.1` |
| Порт / Port | `3306` |
| Пользователь / User | `hospital` |
| Пароль / Password | `hospital_pass` |
| База / Database | `hospital_db` |

### 5.2. `show-db.cmd` — готовый обзор базы

Двойной клик по `show-db.cmd` в папке проекта. Скрипт показывает версию
MySQL, список таблиц, количество строк, записи по статусам, первые 10
приёмов, врачей и уникальный индекс. Окно остаётся открытым.

### 5.3. `open-mysql.cmd` — консоль MySQL

Двойной клик по `open-mysql.cmd`. Откроется приглашение `mysql>`, где можно
вводить SQL:

```sql
SHOW TABLES;
SELECT * FROM doctors;
SELECT * FROM patients;
SELECT * FROM appointments;
DESCRIBE appointments;
exit
```

`exit` или `Ctrl+D` — выйти.

## 6. Что показать в MySQL вживую

Версия, база и таблицы:

```powershell
& "$env:USERPROFILE\mysql84\bin\mysql.exe" --protocol=tcp --host=127.0.0.1 `
  --port=3306 -uroot -proot_pass --database=hospital_db `
  -e "SELECT VERSION() AS mysql_version; SHOW TABLES;"
```

История миграций Flyway:

```powershell
& "$env:USERPROFILE\mysql84\bin\mysql.exe" --protocol=tcp --host=127.0.0.1 `
  --port=3306 -uroot -proot_pass --database=hospital_db `
  -e "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Записи и статусы:

```powershell
& "$env:USERPROFILE\mysql84\bin\mysql.exe" --protocol=tcp --host=127.0.0.1 `
  --port=3306 -uroot -proot_pass --database=hospital_db `
  -e "SELECT status, COUNT(*) FROM appointments GROUP BY status; SELECT id, doctor_id, start_time, end_time, status, price FROM appointments ORDER BY start_time LIMIT 10;"
```

Уникальный индекс, который не даёт записать двух записей на один слот:

```powershell
& "$env:USERPROFILE\mysql84\bin\mysql.exe" --protocol=tcp --host=127.0.0.1 `
  --port=3306 -uroot -proot_pass --database=hospital_db `
  -e "SHOW INDEX FROM appointments WHERE Key_name = 'uk_appointments_doctor_slot_status';"
```

Если в UI запись стоит на 09:00, то и в SQL `start_time` будет `09:00:00`:
`run-demo.ps1` запускает приложение в `Europe/Moscow`, как и контейнер MySQL
в `docker-compose.yml`.

## 7. Демонстрация в браузере

1. Открыть <http://localhost:8080>.
2. Войти как `reception` / `Reception123!`.
3. Открыть календарь, выбрать врача и дату — свободные слоты приходят с
   сервера.
4. Записать пациента на свободный слот.
5. Попробовать записать второго пациента на то же время — интерфейс/API
   вернут конфликт `409`, запись не создастся.
6. Войти как `doctor.ivanov` / `Doctor123!` — доступ только к своим приёмам,
   реестр пациентов и отчёты запрещены (`403`).
7. Войти как `admin` / `Admin123!` — отчёты, врачи, расписания.
8. Параллельно показать в MySQL таблицу `appointments`: новая строка
   появилась в базе, статус `SCHEDULED`, цена зафиксирована.

## 8. Остановка

Приложение: `Ctrl+C` в его окне.

MySQL:

```powershell
powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\mysql84\mysql-stop.ps1"
```

## Если что-то занято

```powershell
# кто слушает порт 8080
Get-NetTCPConnection -LocalPort 8080 -State Listen |
    Select-Object LocalAddress, LocalPort, OwningProcess

# кто слушает порт 3306
Get-NetTCPConnection -LocalPort 3306 -State Listen |
    Select-Object LocalAddress, LocalPort, OwningProcess
```

`run-demo.ps1` сам останавливает только старый процесс MedFlow. Чужую
программу на 8080 он не трогает — тогда запустите на другом порту:

```powershell
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Duser.timezone=Europe/Moscow"
```
