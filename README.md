# Smart Emergency Command Center

A web application that coordinates a medical emergency from the moment a patient presses SOS until the diagnosis is saved in the patient's medical history.

![Dispatcher dashboard](docs/screenshots/04_dispatcher_dashboard.png)

## How it works

1. **Patient** presses SOS. The GPS location is sent and the system calculates a priority score for the case.
2. **System** recommends the nearest free ambulance (and fire or police units when needed). The dispatcher confirms.
3. **Ambulance crew** gets the emergency and checks the patient's Medical Card, including allergies. On the way they record vital signs and choose a hospital.
4. **Doctor** sees the incoming patient before arrival, saves the diagnosis and treatment, then finalizes the case. The case goes into the patient's medical history.
5. **Dispatcher** closes the incident and the resources become available again.

## Roles

| Role | What they can do |
|---|---|
| Patient | SOS, cancel before the crew arrives, Medical Card, emergency contacts, emergency status (if the doctor permits) |
| Dispatcher | Live map, incidents ordered by priority, Smart Dispatch recommendations, resource management |
| Ambulance crew | Assigned emergency, patient Medical Card, transport status, vital signs, hand-over to hospital |
| Doctor | Incoming patients, Medical Card and history, diagnosis and treatment with allergy warning |
| Administrator | User accounts, hospitals, resources, system status, audit log |

## Technologies

| Layer | Stack |
|---|---|
| Frontend | React 18, TypeScript, Vite, React Router, Leaflet / OpenStreetMap |
| Backend | Java 17, Spring Boot 3.3 (Web, Data JPA, Security), JWT, BCrypt, Flyway |
| Database | Microsoft SQL Server (13 tables) |

The app has three tiers: a React client, a Spring Boot REST API, and a SQL Server database. Each role has its own API path (`/api/patient`, `/api/dispatcher`, ...), protected by JWT. Every request is written to an audit log.

## Running locally

Requirements: JDK 17+, Maven, Node.js 18+, SQL Server.

1. In SSMS, run `backend/sql/create_database.sql`. This creates the `secc` database and its login. Make sure TCP/IP is enabled on port 1433.
2. Double-click `start.bat`, or start each part manually:

```bash
cd backend && mvn spring-boot:run      # http://localhost:8080
cd frontend && npm install && npm run dev   # http://localhost:5173
```

The tables and the demo data are created automatically on the first start.

**Demo accounts** (password `password123`): `patient1`, `dispatcher`, `crew1`, `doctor1`, `admin`.

## Documentation

- [`docs/SECC_Documentation.docx`](docs/SECC_Documentation.docx): project documentation (12 pages). It covers the problem, the roles, explained screenshots, use cases, architecture, design, the database and business rules.
- [`docs/diagrams`](docs/diagrams): UML diagrams (use case, class, sequence, state, component, deployment, relational and more).
