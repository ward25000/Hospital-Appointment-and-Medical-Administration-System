# ER-диаграмма базы данных

Проект использует 7 связанных таблиц. Схема создаётся миграцией
`src/main/resources/db/migration/V1__create_schema.sql`, демонстрационные данные —
`V2__seed_data.sql`.

```mermaid
erDiagram
    USERS ||--o| DOCTORS : "профиль врача"
    USERS ||--o{ APPOINTMENTS : "создал запись"
    SPECIALIZATIONS ||--o{ DOCTORS : "направление"
    DOCTORS ||--o{ DOCTOR_SCHEDULES : "недельные смены"
    DOCTORS ||--o{ APPOINTMENTS : "ведёт приём"
    PATIENTS ||--o{ APPOINTMENTS : "записан на приём"
    MEDICAL_SERVICES ||--o{ APPOINTMENTS : "услуга приёма"

    USERS {
        BIGINT id PK
        VARCHAR username UK
        VARCHAR password "BCrypt"
        VARCHAR full_name
        VARCHAR role_name "ADMIN | DOCTOR | RECEPTIONIST"
        BOOLEAN enabled
        DATETIME created_at
        DATETIME updated_at
    }

    SPECIALIZATIONS {
        BIGINT id PK
        VARCHAR name UK
        VARCHAR description
        BOOLEAN active
        DATETIME created_at
        DATETIME updated_at
    }

    DOCTORS {
        BIGINT id PK
        BIGINT user_id FK "UK, users.id"
        BIGINT specialization_id FK "specializations.id"
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR email UK
        VARCHAR phone
        VARCHAR office
        VARCHAR bio
        BOOLEAN active
        DATETIME created_at
        DATETIME updated_at
    }

    DOCTOR_SCHEDULES {
        BIGINT id PK
        BIGINT doctor_id FK "doctors.id"
        VARCHAR day_of_week "MONDAY..SUNDAY"
        TIME start_time
        TIME end_time
        INT slot_minutes
        BOOLEAN active
        DATETIME created_at
        DATETIME updated_at
    }

    PATIENTS {
        BIGINT id PK
        VARCHAR first_name
        VARCHAR last_name
        DATE birth_date
        VARCHAR gender "MALE | FEMALE"
        VARCHAR phone
        VARCHAR email
        VARCHAR address
        DATETIME created_at
        DATETIME updated_at
    }

    MEDICAL_SERVICES {
        BIGINT id PK
        VARCHAR name UK
        VARCHAR description
        DECIMAL price
        INT duration_minutes
        BOOLEAN active
        DATETIME created_at
        DATETIME updated_at
    }

    APPOINTMENTS {
        BIGINT id PK
        BIGINT doctor_id FK "doctors.id"
        BIGINT patient_id FK "patients.id"
        BIGINT service_id FK "medical_services.id"
        DATETIME start_time
        DATETIME end_time
        VARCHAR status "SCHEDULED | COMPLETED | CANCELLED"
        DECIMAL price "снимок цены"
        VARCHAR notes
        VARCHAR cancellation_reason
        DATETIME completed_at
        DATETIME cancelled_at
        BIGINT created_by FK "users.id"
        BIGINT version "оптимистическая блокировка"
        DATETIME created_at
        DATETIME updated_at
    }
```

## Ключевые ограничения

| Таблица | Ограничение | Назначение |
|---|---|---|
| `users` | `uk_users_username` | уникальный логин |
| `doctors` | `uk_doctors_user`, `uk_doctors_email` | один профиль на пользователя, уникальный email |
| `doctor_schedules` | `uk_doctor_schedules_day` | одна активная смена врача на день недели |
| `medical_services` | `uk_medical_services_name` | уникальное название услуги |
| `appointments` | `uk_appointments_doctor_slot_status` | защита от дубля слота на уровне БД |
| `appointments` | `version` (`@Version`) | оптимистическая блокировка при изменении |
| `appointments` | индексы по врачу, пациенту и статусу | быстрые выборки календаря и отчётов |

## Почему защита от двойной записи работает

1. При создании или переносе приёма сервис блокирует строку врача:
   `DoctorRepository.findByIdForUpdate` (`SELECT ... FOR UPDATE`).
2. Под блокировкой выполняется проверка пересечения интервала с существующими
   приёмами врача, кроме `CANCELLED` и, при переносе, кроме самой записи.
3. Дополнительно уникальный индекс `(doctor_id, start_time, status)` не даёт
   сохранить два одинаковых слота даже при ошибке в прикладной логике.
4. Транзакция создания/переноса работает на уровне `READ_COMMITTED`, чтобы
   проверка видела только что зафиксированные записи других транзакций.

Отменённые приёмы сохраняются в истории, но не занимают слот: слот снова
становится доступным для записи.
