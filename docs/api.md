# REST API

Базовый URL: `http://localhost:8080`
Формат данных: `application/json`, даты — ISO 8601 (`2026-11-05`, `2026-11-05T10:00:00`),
денежные значения — `decimal`.

Интерактивная документация: `http://localhost:8080/swagger-ui/index.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Аутентификация и CSRF

Используется сессионная аутентификация Spring Security (`JSESSIONID`) и BCrypt.
CSRF-токен хранится в cookie `XSRF-TOKEN` и передаётся заголовком `X-XSRF-TOKEN`
для всех `POST`, `PUT`, `PATCH`, `DELETE`.

Порядок входа:

1. `GET /api/auth/csrf` — получить токен и cookie.
2. `POST /api/auth/login` с заголовком `X-XSRF-TOKEN` и телом:

```json
{
  "username": "admin",
  "password": "Admin123!"
}
```

3. Все дальнейшие запросы выполняются с cookie сессии и CSRF-заголовком.
4. `POST /api/auth/logout` — завершить сессию (`204 No Content`).

Пример ошибки авторизации:

```json
{
  "timestamp": "2026-11-04T11:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Требуется авторизация",
  "path": "/api/patients",
  "fieldErrors": null
}
```

Ошибки валидации возвращаются со статусом `400` и заполненным `fieldErrors`.
Конфликт слота или дубль уникального поля — `409 Conflict`, нарушение
бизнес-правила (например, недопустимый переход статуса) — `422 Unprocessable
Entity`, недостаточно прав — `403 Forbidden`, объект не найден — `404 Not Found`.

## Матрица доступа

| Ресурс | ADMIN | RECEPTIONIST | DOCTOR | PATIENT |
|---|:---:|:---:|:---:|:---:|
| Дашборд | да | да | да, только свои данные | нет |
| Пациенты (чтение/запись) | да | да | нет | только свой профиль через `/api/me` |
| Врачи и специализации (чтение) | да | да | да | да (авторизован) |
| Врачи и специализации (запись) | да | нет | нет | нет |
| Услуги (чтение) | да | да | да | да (авторизован) |
| Услуги (запись) | да | нет | нет | нет |
| Расписание: чтение | да | да* | только своё | нет |
| Расписание: запись | да | нет | нет | нет |
| Записи: чтение | все | все | только свои | только свои |
| Записи: создание/перенос | да | да | нет | нет |
| Записи: завершение | да | нет | только свои | нет |
| Записи: отмена | да | да | только свои | нет |
| Записи: «Не явился» | да | нет | только свои прошедшие | нет |
| Направления: создание/статус | да | нет | да | нет |
| Направления: входящие | нет | нет | только себе | нет |
| Направления: свои | нет | нет | нет | да |
| Отчёты | да | да | нет | нет |

\* Раздел расписания скрыт в интерфейсе регистратора, но API чтения доступен
авторизованным сотрудникам; изменения расписания — только администратор.

## Служебные эндпоинты

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| GET | `/api/health` | публично | статус приложения и БД |
| GET | `/actuator/health` | публично | health-check Spring Boot Actuator |
| GET | `/actuator/info` | публично | информация о приложении |
| GET | `/swagger-ui/index.html` | публично | Swagger UI |
| GET | `/v3/api-docs` | публично | OpenAPI 3 JSON |

`GET /api/health` возвращает:

```json
{
  "status": "UP",
  "database": "UP",
  "application": "hospital-appointment-system",
  "version": "1.0.0",
  "timestamp": "2026-11-04T11:00:00Z"
}
```

## Аутентификация

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| GET | `/api/auth/csrf` | публично | CSRF-токен |
| POST | `/api/auth/login` | публично | вход по логину и паролю |
| POST | `/api/auth/register` | публично | саморегистрация пациента и автоматический вход |
| GET | `/api/auth/me` | авторизован | текущий пользователь |
| POST | `/api/auth/logout` | авторизован | выход |

### Регистрация пациента

`POST /api/auth/register` (публично, CSRF обязателен):

```json
{
  "username": "anna.patient",
  "password": "Patient123!",
  "iin": "333333333333",
  "firstName": "Анна",
  "lastName": "Соколова",
  "birthDate": "1991-03-04",
  "gender": "FEMALE",
  "phone": "+7 (916) 333-33-33",
  "email": "anna@example.com",
  "address": "Москва, ул. Новая, 3"
}
```

Обязательные поля: `username`, `password`, `iin`, `firstName`, `lastName`,
`birthDate`, `gender`, `phone`, `address`. `iin` — ровно 12 цифр и уникален
среди всех пациентов; `address` обязателен. Пароль — 8–100 символов.
Ответ `201 Created` с тем же телом, что и `/api/auth/me`, при этом сессия уже
аутентифицирована как `PATIENT`:

```json
{
  "id": 8,
  "username": "anna.patient",
  "fullName": "Анна Соколова",
  "role": "PATIENT",
  "roleLabel": "Пациент",
  "doctorId": null,
  "patientId": 13
}
```

Повторный ИИН или логин возвращает `409 Conflict`, ошибки полей — `400` с
заполненным `fieldErrors`.

`GET /api/auth/me`:

```json
{
  "id": 1,
  "username": "admin",
  "fullName": "Анна Викторовна Смирнова",
  "role": "ADMIN",
  "roleLabel": "Администратор",
  "doctorId": null,
  "patientId": null
}
```

## Дашборд

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| GET | `/api/dashboard` | авторизован | KPI, приёмы на сегодня и ближайшие визиты |

Для врача дашборд автоматически ограничивается его приёмами.

## Пациенты

Доступ: `ADMIN`, `RECEPTIONIST`.

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/patients?query=Анна` | список/поиск по ФИО, телефону, email |
| GET | `/api/patients/{id}` | карточка пациента |
| POST | `/api/patients` | создать пациента |
| PUT | `/api/patients/{id}` | обновить пациента |
| DELETE | `/api/patients/{id}` | удалить пациента, если нет записей |

Тело запроса:

```json
{
  "iin": "900412100001",
  "firstName": "Анна",
  "lastName": "Соколова",
  "birthDate": "1990-04-12",
  "gender": "FEMALE",
  "phone": "+7 (916) 100-10-01",
  "email": "a.sokolova@example.com",
  "address": "Москва, ул. Лесная, 10"
}
```

`iin` (12 цифр, уникален) и `address` обязательны. Ответ содержит `id`, `iin`,
`userId`, `fullName`, `age`, `genderLabel` и контактные данные. Удаление пациента
с историей приёмов возвращает `409 Conflict`. Поле `userId` заполнено, если к
карточке привязан личный кабинет пациента.

## Личный кабинет пациента

Доступ: только `PATIENT`. Все данные ограничены пациентом текущей сессии.

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/me/profile` | свой профиль с ИИН и адресом |
| PUT | `/api/me/profile` | обновить имя, фамилию, дату рождения, пол, телефон, email, адрес |
| GET | `/api/me/visits` | полная история своих приёмов |

`GET /api/me/profile`:

```json
{
  "id": 1,
  "iin": "900412100001",
  "userId": 7,
  "firstName": "Анна",
  "lastName": "Соколова",
  "fullName": "Анна Соколова",
  "birthDate": "1990-04-12",
  "age": 36,
  "gender": "FEMALE",
  "genderLabel": "Женский",
  "phone": "+7 (916) 100-10-01",
  "email": "a.sokolova@example.com",
  "address": "Москва, ул. Лесная, 10"
}
```

Тело `PUT /api/me/profile` не содержит `iin` — ИИН неизменяем через API
пациента. `address` обязателен. Попытка обратиться к `/api/patients` с ролью
`PATIENT` возвращает `403 Forbidden`.

`GET /api/me/visits` возвращает массив `AppointmentResponse` (см. ниже), где
всегда заполнены `doctorName`, `doctorSpecialization`, `startTime`, `status` и
`statusLabel`. Статусы приёма: `SCHEDULED` («Ожидает»), `COMPLETED`
(«Приём проведён»), `CANCELLED` («Отменён»), `NO_SHOW` («Неявился»).

## Электронные направления

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| POST | `/api/referrals` | `ADMIN`, `DOCTOR` | выписать направление |
| GET | `/api/referrals/incoming?status=PENDING` | `DOCTOR` | входящие направления врача |
| GET | `/api/referrals/mine` | `PATIENT` | направления пациента |
| PUT | `/api/referrals/{id}/status` | `ADMIN`, `DOCTOR` | изменить статус |

Создание направления по конкретному врачу:

```json
{
  "appointmentId": 11,
  "toDoctorId": 2,
  "department": "Неврология",
  "office": "305",
  "reason": "Головные боли, нужна консультация невролога"
}
```

Создание направления по специальности:

```json
{
  "appointmentId": 11,
  "toSpecializationId": 1,
  "department": "Кардиология",
  "office": "201",
  "reason": "Повышенное артериальное давление"
}
```

Должен быть указан ровно один адресат: `toDoctorId` или `toSpecializationId`.
Пациент и врач-автор берутся из приёма `appointmentId`, чужой приём отклоняется
с `403 Forbidden`. Если `department`/`office` не переданы, они подставляются из
профиля врача либо из названия специальности.

Ответ `201 Created`:

```json
{
  "id": 1,
  "patientId": 1,
  "patientName": "Анна Соколова",
  "fromDoctorId": 1,
  "fromDoctorName": "Алексей Иванов",
  "fromSpecialization": "Кардиология",
  "toDoctorId": 2,
  "toDoctorName": "Пётр Петров",
  "toSpecializationId": null,
  "toSpecializationName": "Неврология",
  "appointmentId": 11,
  "visitStartTime": "2026-11-18T10:00:00",
  "department": "Неврология",
  "office": "305",
  "reason": "Головные боли, нужна консультация невролога",
  "status": "PENDING",
  "statusLabel": "Ожидает",
  "decidedAt": null,
  "createdAt": "2026-11-18T11:00:00"
}
```

Маршрутизация: направление с `toDoctorId` видно только этому врачу; направление
с `toSpecializationId` и пустым `toDoctorId` — всем врачам данной специальности.
Фильтр `status` принимает `PENDING`, `ACCEPTED`, `COMPLETED`, `CANCELLED`.

Смена статуса (`PUT /api/referrals/{id}/status`, тело
`{"status":"ACCEPTED"}`) доступна врачу-получателю. Переходы:
`PENDING → ACCEPTED | CANCELLED`, `ACCEPTED → COMPLETED | CANCELLED`.
Терминальные статусы `COMPLETED` и `CANCELLED` неизменяемы (`422` при
недопустимом переходе). Статус сразу виден пациенту в `/api/referrals/mine`.

## Врачи и специализации

Чтение — все авторизованные, запись — `ADMIN`.

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/doctors?specializationId=1&query=Иванов` | список врачей с фильтрами |
| GET | `/api/doctors/{id}` | карточка врача |
| POST | `/api/doctors` | создать врача и учётную запись |
| PUT | `/api/doctors/{id}` | обновить профиль |
| DELETE | `/api/doctors/{id}` | удалить врача без записей |
| GET | `/api/specializations` | список специализаций |
| GET | `/api/specializations/{id}` | специализация |
| POST | `/api/specializations` | создать специализацию |
| PUT | `/api/specializations/{id}` | обновить специализацию |
| DELETE | `/api/specializations/{id}` | удалить специализацию |

Создание врача:

```json
{
  "firstName": "Алексей",
  "lastName": "Иванов",
  "specializationId": 1,
  "email": "a.ivanov@medcenter.ru",
  "phone": "+7 (495) 120-20-01",
  "office": "201",
  "bio": "Кардиолог высшей категории",
  "username": "doctor.ivanov",
  "password": "Doctor123!"
}
```

Создание специализации:

```json
{
  "name": "Ортопедия",
  "description": "Заболевания и травмы опорно-двигательного аппарата",
  "active": true
}
```

## Медицинские услуги

Чтение — все авторизованные, запись — `ADMIN`.

| Метод | Путь | Описание |
|---|---|---|
| GET | `/api/services` | прайс услуг |
| GET | `/api/services/{id}` | услуга |
| POST | `/api/services` | создать услугу |
| PUT | `/api/services/{id}` | обновить услугу |
| DELETE | `/api/services/{id}` | удалить услугу без записей |

```json
{
  "name": "УЗИ сердца",
  "description": "Эхокардиография с допплерографией",
  "price": 3200.00,
  "durationMinutes": 45,
  "active": true
}
```

## Расписание врача

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| GET | `/api/doctors/{doctorId}/schedules` | авторизован; врач — только своё | недельные смены |
| POST | `/api/doctors/{doctorId}/schedules` | `ADMIN` | добавить смену |
| PUT | `/api/schedules/{id}` | `ADMIN` | изменить смену |
| DELETE | `/api/schedules/{id}` | `ADMIN` | удалить смену |

```json
{
  "dayOfWeek": "MONDAY",
  "startTime": "09:00:00",
  "endTime": "17:00:00",
  "slotMinutes": 30,
  "active": true
}
```

На одного врача — одна смена на день недели. Длительность интервала: 10–120 минут.

## Свободные слоты

`GET /api/doctors/{doctorId}/availability?date=2026-11-05&serviceId=4&excludeAppointmentId=11`

| Параметр | Обязателен | Описание |
|---|:---:|---|
| `date` | да | дата в формате `yyyy-MM-dd` |
| `serviceId` | нет | длительность услуги для расчёта слота |
| `excludeAppointmentId` | нет | исключить текущую запись при переносе |

Ответ:

```json
{
  "doctorId": 1,
  "doctorName": "Алексей Иванов",
  "date": "2026-11-05",
  "dayOfWeek": "THURSDAY",
  "dayLabel": "Четверг",
  "workingDay": true,
  "message": null,
  "slotMinutes": 30,
  "serviceDurationMinutes": 45,
  "slots": [
    {
      "startTime": "2026-11-05T09:00:00",
      "endTime": "2026-11-05T09:45:00",
      "available": true,
      "reason": null
    },
    {
      "startTime": "2026-11-05T10:00:00",
      "endTime": "2026-11-05T10:45:00",
      "available": false,
      "reason": "Занято другим приёмом"
    }
  ]
}
```

Слоты формируются внутри активной смены с шагом `slotMinutes`, с учётом
длительности услуги, текущего времени и занятых интервалов. Выходной день
возвращает `workingDay: false` и пустой список.

## Записи на приём

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| GET | `/api/appointments` | авторизован; врач — только свои | журнал с фильтрами |
| GET | `/api/appointments/{id}` | авторизован; врач — только своя | карточка приёма |
| POST | `/api/appointments` | `ADMIN`, `RECEPTIONIST` | создать запись |
| PUT | `/api/appointments/{id}/reschedule` | `ADMIN`, `RECEPTIONIST` | перенести запись |
| POST | `/api/appointments/{id}/complete` | `ADMIN`, `DOCTOR` (своя) | завершить приём |
| POST | `/api/appointments/{id}/no-show` | `ADMIN`, `DOCTOR` (своя) | отметить неявку |
| POST | `/api/appointments/{id}/cancel` | `ADMIN`, `RECEPTIONIST`, `DOCTOR` (своя) | отменить запись |

Фильтры `GET /api/appointments`:
`from`, `to` (`yyyy-MM-dd`), `doctorId`, `patientId`, `status`
(`SCHEDULED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`), `query` (пациент, телефон,
врач, услуга). Пациент видит в этом списке только свои записи.

Создание:

```json
{
  "doctorId": 1,
  "patientId": 1,
  "serviceId": 4,
  "startTime": "2026-11-05T10:00:00",
  "notes": "Первичная запись"
}
```

Перенос:

```json
{
  "startTime": "2026-11-06T11:30:00",
  "notes": "Перенос по просьбе пациента"
}
```

Отмена (тело необязательно):

```json
{
  "reason": "Пациент не сможет прийти"
}
```

Ответ приёма:

```json
{
  "id": 11,
  "doctorId": 1,
  "doctorName": "Алексей Иванов",
  "doctorSpecialization": "Кардиология",
  "doctorOffice": "201",
  "patientId": 1,
  "patientName": "Анна Соколова",
  "patientPhone": "+7 (916) 100-10-01",
  "serviceId": 4,
  "serviceName": "УЗИ сердца",
  "serviceDurationMinutes": 45,
  "startTime": "2026-11-05T10:00:00",
  "endTime": "2026-11-05T10:45:00",
  "status": "SCHEDULED",
  "statusLabel": "Ожидает",
  "price": 3200.00,
  "notes": "Первичная запись",
  "cancellationReason": null,
  "completedAt": null,
  "cancelledAt": null,
  "createdAt": "2026-11-04T10:00:00"
}
```

Правила:

- запись создаётся только на будущее время;
- время должно попадать в активную смену врача и быть кратно интервалу приёма;
- интервал не должен пересекаться с другим активным приёмом врача;
- отменённые приёмы не блокируют слот;
- завершить можно только запись в статусе `SCHEDULED`, у которой уже наступило время;
- отметить неявку можно только для `SCHEDULED`, у которой время уже прошло;
  статус становится `NO_SHOW` («Не явился»);
- отменить можно только `SCHEDULED`;
- повторная отмена/завершение возвращает `409 Conflict`;
- попытка изменить чужую запись или применить недопустимый переход — `403` или `422`.

## Отчёты

Доступ: `ADMIN`, `RECEPTIONIST`.

`GET /api/reports?from=2026-11-01&to=2026-11-30`

Без параметров отчёт строится за текущий месяц. Ответ содержит:

```json
{
  "from": "2026-11-01",
  "to": "2026-11-30",
  "summary": {
    "from": "2026-11-01",
    "to": "2026-11-30",
    "totalAppointments": 18,
    "scheduled": 7,
    "completed": 9,
    "cancelled": 2,
    "uniquePatients": 12,
    "revenue": 20300.00,
    "averageCheck": 2255.56
  },
  "byDoctor": [
    {
      "doctorId": 1,
      "doctorName": "Алексей Иванов",
      "specialization": "Кардиология",
      "total": 9,
      "completed": 5,
      "cancelled": 0,
      "scheduled": 4,
      "revenue": 11300.00
    }
  ],
  "byService": [
    {
      "serviceId": 4,
      "serviceName": "УЗИ сердца",
      "total": 2,
      "completed": 1,
      "revenue": 3200.00
    }
  ],
  "daily": [
    {
      "date": "2026-11-04",
      "total": 4,
      "scheduled": 1,
      "completed": 3,
      "cancelled": 0,
      "revenue": 5500.00
    }
  ]
}
```

Выручка считается только по завершённым приёмам, по цене, сохранённой в записи
на момент её создания. CSV для отчёта формируется на клиенте из этого ответа.

## Коды ответов

| Код | Значение |
|---|---|
| 200 | успешное чтение/изменение |
| 201 | объект создан, в `Location` — ссылка на него |
| 204 | удаление/выход без тела |
| 400 | ошибка валидации или некорректный параметр |
| 401 | нет аутентификации |
| 403 | роль или владелец операции не подходят |
| 404 | объект не найден |
| 409 | конфликт слота, дубль ИИН/логина или повторное изменение записи |
| 422 | нарушение бизнес-правила, например недопустимый переход статуса направления |
| 503 | БД недоступна (`/api/health`) |
