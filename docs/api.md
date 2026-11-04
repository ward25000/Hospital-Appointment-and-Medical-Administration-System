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
Конфликт слота/бизнес-правила — `409 Conflict`, недостаточно прав — `403 Forbidden`,
объект не найден — `404 Not Found`.

## Матрица доступа

| Ресурс | ADMIN | RECEPTIONIST | DOCTOR |
|---|:---:|:---:|:---:|
| Дашборд | да | да | да, только свои данные |
| Пациенты (чтение/запись) | да | да | нет |
| Врачи и специализации (чтение) | да | да | да |
| Врачи и специализации (запись) | да | нет | нет |
| Услуги (чтение) | да | да | да |
| Услуги (запись) | да | нет | нет |
| Расписание: чтение | да | да* | только своё |
| Расписание: запись | да | нет | нет |
| Записи: чтение | все | все | только свои |
| Записи: создание/перенос | да | да | нет |
| Записи: завершение | да | нет | только свои |
| Записи: отмена | да | да | только свои |
| Отчёты | да | да | нет |

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
| GET | `/api/auth/me` | авторизован | текущий пользователь |
| POST | `/api/auth/logout` | авторизован | выход |

`GET /api/auth/me`:

```json
{
  "id": 1,
  "username": "admin",
  "fullName": "Анна Викторовна Смирнова",
  "role": "ADMIN",
  "roleLabel": "Администратор",
  "doctorId": null
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
  "firstName": "Анна",
  "lastName": "Соколова",
  "birthDate": "1990-04-12",
  "gender": "FEMALE",
  "phone": "+7 (916) 100-10-01",
  "email": "a.sokolova@example.com",
  "address": "Москва, ул. Лесная, 10"
}
```

Ответ содержит `id`, `fullName`, `age`, `genderLabel` и контактные данные.
Удаление пациента с историей приёмов возвращает `409 Conflict`.

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
| POST | `/api/appointments/{id}/cancel` | `ADMIN`, `RECEPTIONIST`, `DOCTOR` (своя) | отменить запись |

Фильтры `GET /api/appointments`:
`from`, `to` (`yyyy-MM-dd`), `doctorId`, `patientId`, `status`
(`SCHEDULED`, `COMPLETED`, `CANCELLED`), `query` (пациент, телефон, врач, услуга).

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
  "statusLabel": "Запланирован",
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
- отменить можно только `SCHEDULED`;
- повторная отмена/завершение возвращает `409 Conflict`.

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
| 409 | конфликт слота, статуса или бизнес-правила |
| 503 | БД недоступна (`/api/health`) |
