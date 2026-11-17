# RefCard 03 – Spring Boot Backend + MariaDB

REST-API-Backend für die BusinessTrips-Applikation (Mitarbeitende, Business
Trips, Flüge, Meetings). Teil der CI/CD-Übungssequenz **RefCard 02–05**
(siehe [`docs/future/RefCard-03-RefCard-05-own-repos.md`](docs/future/RefCard-03-RefCard-05-own-repos.md))
— dieses Repository ist **RefCard 03**: Spring Boot Backend mit MariaDB.

## Tech-Stack

- Java 21, Spring Boot 4.1 (Web, Data JPA)
- MariaDB (lokal via Docker Compose) / H2 (Default, für Tests und schnellen lokalen Start)
- Maven (Maven Wrapper, kein lokales Maven nötig)

## Schnellstart

### Ohne Datenbank-Setup (H2, Default)

```bash
./mvnw spring-boot:run
```

Startet auf Port 8080 mit einer flüchtigen In-Memory-H2-Datenbank (Daten sind nach jedem Neustart weg, werden aber automatisch neu geseedet).

### Mit MariaDB (persistent, wie in Produktion)

```bash
cp .env.example .env
# DB_PASSWORD in .env durch ein lokales Passwort ersetzen
docker compose -f docker/docker-compose.yml up -d
```

- MariaDB läuft dann auf `localhost:3306` (Datenbank `db_biztrips`, User `root`)
- Das Backend läuft auf `http://localhost:8080` und speichert Fotos lokal im Compose-Volume
- phpMyAdmin ist unter [http://localhost:8082](http://localhost:8082) erreichbar (gleiche Zugangsdaten)
- Die Demo-Daten werden nur beim allerersten Start eingefügt — ein Neustart der Applikation dupliziert sie nicht

## Profilbilder und AWS-Konfiguration

`POST /v1/employees/{id}/photo` erwartet ein Multipart-Feld `file`; JPEG, PNG und WebP sind erlaubt (max. 5 MB). `GET /v1/employees/{id}/photo` liefert das Bild zurück. Lokal verwendet das Backend `APP_STORAGE_LOCATION` (standardmässig `./uploads`); im Compose-Stack liegt es im persistenten Volume `uploads`.

Für ECS wird das Profil `s3` aktiviert. Der Bucket-Name kommt aus `APP_S3_BUCKET`, die Region aus `APP_AWS_REGION` (Standard `us-east-1`). Der S3-Client nutzt die AWS Default Credentials Chain; auf ECS stammen die temporären Credentials aus der Task-Rolle. RDS-Verbindungseinstellungen werden über `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` und `DB_PASSWORD` gesetzt. Benutzername und Passwort werden in ECS aus AWS Secrets Manager injiziert; es gibt keine produktiven DB-Zugangsdaten in den Spring-Konfigurationsdateien.

Die ECS-Task-Rolle benötigt nur die Rechte aus [`aws/s3-profile-photos-policy.json`](aws/s3-profile-photos-policy.json); vor dem Anhängen `REPLACE_WITH_BUCKET_NAME` durch den konkreten Bucket-Namen ersetzen. Die Policy gilt nur für `employee-photos/*`. Die Pipeline hat getrennte Variablen für Task- und Execution-Rolle. Im Learner Lab wird mangels eigener Rollen die vorgegebene `LabRole` für beide verwendet; die Bucket-Policy muss der Rolle im Lab hinzugefügt werden. In einem regulären AWS-Account sollten es zwei Rollen sein, wobei nur die Task-Rolle die S3-Policy erhält.

Die Backend-Pipeline testet und baut bei Pull Requests und Pushes. Pushes auf `main` und Versionstags (`v*`) bauen ein Image, pushen es nach ECR und warten auf ein stabiles ECS-Deployment. Für das Learner Lab die Actions-Secrets `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` und `AWS_SESSION_TOKEN` anlegen. Diese temporären Werte nach jeder neuen Lab-Sitzung aktualisieren; in einem produktiven AWS-Account wäre OIDC mit kurzlebigen Role-Credentials vorzuziehen.

Als GitHub-Repository-Variablen werden benötigt: `AWS_REGION`, `ECS_EXECUTION_ROLE_ARN`, `ECS_TASK_ROLE_ARN`, `ECS_CLUSTER`, `ECS_SERVICE`, `RDS_HOST`, `RDS_USERNAME_SECRET_ARN`, `RDS_PASSWORD_SECRET_ARN` und `S3_BUCKET`. Im Learner Lab enthalten beide Rollen-Variablen die ARN der `LabRole`. Die Secret-ARNs müssen von der Execution-Rolle lesbar sein. `RDS_DB_NAME` und `RDS_DB_PORT` sind optional (Standardwerte `db_biztrips` und `3306`).

Die Foto-Endpoints und lokale Entwicklung sind im Backend-Repo enthalten. Da das Frontend in einem separaten Repository liegt, muss dessen nginx-Image separat gebaut und deployed werden; hier wird bewusst keine Frontend-Compose-Konfiguration vorgetäuscht.

### Tests ausführen

```bash
./mvnw test
```

## API

Basis-Pfad: `/v1`

| Endpoint | Beschreibung |
| --- | --- |
| `GET /v1/trips`, `POST /v1/trips`, `GET /v1/trips/{id}`, `DELETE /v1/trips/{id}` | Business Trips |
| `GET /v1/employees`, `POST /v1/employees`, `GET /v1/employees/{id}`, `PUT /v1/employees/{id}`, `DELETE /v1/employees/{id}`, `DELETE /v1/employees` | Mitarbeitende |
| `GET /v1/flights`, `POST /v1/flights`, `GET /v1/flights/{id}`, `PUT /v1/flights/{id}`, `DELETE /v1/flights/{id}`, `DELETE /v1/flights` | Flüge |
| `GET /v1/meetings`, `POST /v1/meetings`, `GET /v1/meetings/{id}`, `PUT /v1/meetings/{id}`, `DELETE /v1/meetings/{id}`, `DELETE /v1/meetings` | Meetings |
| `POST /v1/signIn` | Login (Demo-User: `joe` / `1234`) |
| `GET /actuator/health` | Health-Check (Spring Boot Actuator) — prüft u. a. die Datenbankverbindung |

## Projektstruktur

```
src/main/java/ch/bbw/trips/   Applikationscode (Controller, Entities, Repositories, DTOs)
docker/docker-compose.yml     MariaDB + phpMyAdmin für die lokale Entwicklung
docs/exercises/               CI/CD-Übungen (EX-01 EC2, EX-02 Docker Hub, EX-03 ECS/RDS)
docs/reviews/                 Code-Reviews
docs/future/                  Planungsentscheidungen für RefCard 03–05
```

## Für Studierende

Der Tag [`v1-start`](../../releases/tag/v1-start) markiert den Ausgangspunkt für die Übungen in `docs/exercises/` — bewusst **ohne** `Dockerfile`, `.dockerignore`, `.github/workflows/deploy.yml` und `task-definition.json`, da ihr genau diese Dateien in EX-01/EX-02/EX-03 selbst erstellt.
