-- Smart Emergency Command Center - database schema (Microsoft SQL Server / T-SQL)
-- Executed automatically by Flyway at the first start of the backend.

CREATE TABLE hospital (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(150) NOT NULL,
    lat         FLOAT NOT NULL,
    lng         FLOAT NOT NULL
);

CREATE TABLE app_user (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    username      NVARCHAR(50)  NOT NULL UNIQUE,
    password_hash NVARCHAR(100) NOT NULL,
    full_name     NVARCHAR(120) NOT NULL,
    role          NVARCHAR(20)  NOT NULL CHECK (role IN ('PATIENT','DISPATCHER','CREW','DOCTOR','ADMIN')),
    enabled       BIT           NOT NULL DEFAULT 1,
    hospital_id   BIGINT NULL REFERENCES hospital(id),          -- only for DOCTOR
    last_login    DATETIMEOFFSET(6) NULL,
    created_at    DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET()
);

-- Medical Card (one per patient)
CREATE TABLE medical_card (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    patient_id         BIGINT NOT NULL UNIQUE REFERENCES app_user(id),
    date_of_birth      DATE NULL,
    blood_type         NVARCHAR(5) NULL,
    allergies          NVARCHAR(MAX) NULL,
    chronic_conditions NVARCHAR(MAX) NULL,
    current_medication NVARCHAR(MAX) NULL,
    medical_notes      NVARCHAR(MAX) NULL,
    home_lat           FLOAT NULL,
    home_lng           FLOAT NULL
);

CREATE TABLE emergency_contact (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    patient_id   BIGINT NOT NULL REFERENCES app_user(id),
    name         NVARCHAR(120) NOT NULL,
    relationship NVARCHAR(60) NULL,
    phone        NVARCHAR(20) NOT NULL
);

-- Ambulances, fire teams, fire trucks, police units
CREATE TABLE resource (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    code         NVARCHAR(20) NOT NULL UNIQUE,
    type         NVARCHAR(20) NOT NULL CHECK (type IN ('AMBULANCE','FIRE_TEAM','FIRE_TRUCK','POLICE_UNIT')),
    status       NVARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE','BUSY','OUT_OF_SERVICE')),
    lat          FLOAT NOT NULL,
    lng          FLOAT NOT NULL,
    crew_user_id BIGINT NULL REFERENCES app_user(id)            -- only for AMBULANCE
);
-- one crew account per ambulance (filtered index: SQL Server UNIQUE would allow only one NULL)
CREATE UNIQUE INDEX ux_resource_crew ON resource(crew_user_id) WHERE crew_user_id IS NOT NULL;

CREATE TABLE incident (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    type                NVARCHAR(30) NOT NULL,
    severity            NVARCHAR(10) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    affected_people     INT NOT NULL CHECK (affected_people >= 1),
    description         NVARCHAR(1000) NULL,
    lat                 FLOAT NOT NULL,
    lng                 FLOAT NOT NULL,
    location_approx     BIT NOT NULL DEFAULT 0,
    source              NVARCHAR(10) NOT NULL CHECK (source IN ('SOS','PHONE','SIMULATED')),
    status              NVARCHAR(15) NOT NULL CHECK (status IN ('NEW','DISPATCHED','IN_PROGRESS','AT_HOSPITAL','CLOSED','CANCELLED')),
    patient_id          BIGINT NULL REFERENCES app_user(id),
    created_at          DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    first_dispatched_at DATETIMEOFFSET(6) NULL,
    closed_at           DATETIMEOFFSET(6) NULL,
    closing_note        NVARCHAR(1000) NULL,
    cancel_reason       NVARCHAR(100) NULL
);
CREATE INDEX ix_incident_status ON incident(status);

-- A resource assigned to an incident. For ambulances it also holds the transport.
CREATE TABLE assignment (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    incident_id             BIGINT NOT NULL REFERENCES incident(id),
    resource_id             BIGINT NOT NULL REFERENCES resource(id),
    active                  BIT NOT NULL DEFAULT 1,
    assigned_at             DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    released_at             DATETIMEOFFSET(6) NULL,
    transport_status        NVARCHAR(15) NULL,
    destination_hospital_id BIGINT NULL REFERENCES hospital(id),
    handover_note           NVARCHAR(1000) NULL
);
CREATE INDEX ix_assignment_incident ON assignment(incident_id);
CREATE INDEX ix_assignment_resource ON assignment(resource_id);

-- Transport status timeline
CREATE TABLE transport_event (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    assignment_id BIGINT NOT NULL REFERENCES assignment(id),
    status        NVARCHAR(15) NOT NULL,
    event_at      DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET()
);

-- Emergency Transport Record: one row per measurement
CREATE TABLE vital_sign (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    assignment_id     BIGINT NOT NULL REFERENCES assignment(id),
    recorded_at       DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    pulse             INT NULL,
    spo2              INT NULL,
    systolic          INT NULL,
    diastolic         INT NULL,
    temperature       NUMERIC(4,1) NULL,
    oxygen_lpm        NUMERIC(4,1) NULL,
    iv                BIT NOT NULL DEFAULT 0,
    medication        NVARCHAR(500) NULL,
    interventions     NVARCHAR(500) NULL,
    patient_condition NVARCHAR(10) NOT NULL CHECK (patient_condition IN ('STABLE','SERIOUS','CRITICAL')),
    observations      NVARCHAR(1000) NULL
);

CREATE TABLE medical_case (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    assignment_id   BIGINT NOT NULL UNIQUE REFERENCES assignment(id),
    incident_id     BIGINT NOT NULL REFERENCES incident(id),
    patient_id      BIGINT NULL REFERENCES app_user(id),
    hospital_id     BIGINT NOT NULL REFERENCES hospital(id),
    doctor_id       BIGINT NOT NULL REFERENCES app_user(id),
    diagnosis       NVARCHAR(1000) NOT NULL,
    treatment       NVARCHAR(1000) NULL,
    medication      NVARCHAR(1000) NULL,
    procedures      NVARCHAR(1000) NULL,
    notes           NVARCHAR(2000) NULL,
    patient_status  NVARCHAR(15) NOT NULL CHECK (patient_status IN ('ADMITTED','DISCHARGED','TRANSFERRED','DECEASED')),
    finalized       BIT NOT NULL DEFAULT 0,
    finalized_at    DATETIMEOFFSET(6) NULL,
    updated_at      DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET()
);

CREATE TABLE medical_history (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    patient_id  BIGINT NOT NULL REFERENCES app_user(id),
    event_date  DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    hospital_id BIGINT NULL REFERENCES hospital(id),
    doctor_id   BIGINT NULL REFERENCES app_user(id),
    incident_id BIGINT NULL REFERENCES incident(id),
    diagnosis   NVARCHAR(1000) NOT NULL,
    treatment   NVARCHAR(1000) NULL
);

CREATE TABLE audit_log (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    event_at    DATETIMEOFFSET(6) NOT NULL DEFAULT SYSDATETIMEOFFSET(),
    username    NVARCHAR(50) NULL,
    ip          NVARCHAR(45) NULL,
    action      NVARCHAR(15) NOT NULL,      -- LOGIN, LOGIN_FAILED, REQUEST
    method      NVARCHAR(10) NULL,
    path        NVARCHAR(255) NULL,
    status      INT NULL,
    duration_ms BIGINT NULL
);
CREATE INDEX ix_audit_event_at ON audit_log(event_at DESC);

CREATE TABLE app_setting (
    setting_key   NVARCHAR(50) PRIMARY KEY,
    setting_value NVARCHAR(255) NULL
);
INSERT INTO app_setting(setting_key, setting_value) VALUES ('disaster_active', 'false'), ('disaster_scenario', NULL);
