# Efficientia API

> Enterprise-grade RESTful API and event-driven backend for live cattle transportation logistics, operational route diaries, and animal welfare compliance monitoring.

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations%20(V1--V19)-CC0200?style=flat-square&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Redis](https://img.shields.io/badge/Redis-Streams%20%26%20Cache-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20%2F%20Swagger-85EA2D?style=flat-square&logo=openapiinitiative&logoColor=black)](https://swagger.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)

---

## Table of Contents

1. [Overview](#overview)
2. [Key Architecture & Core Subsystems](#key-architecture--core-subsystems)
3. [Technology Stack](#technology-stack)
4. [Database & Schema Architecture](#database--schema-architecture)
5. [Local Development Setup](#local-development-setup)
   - [Prerequisites](#prerequisites)
   - [Environment Configuration](#environment-configuration)
   - [Database Initialization](#database-initialization)
   - [Building and Running Locally](#building-and-running-locally)
   - [Docker Deployment](#docker-deployment)
   - [Running Automated Tests](#running-automated-tests)
6. [Interactive Documentation & Observability](#interactive-documentation--observability)
7. [Authentication & Authorization Model](#authentication--authorization-model)
8. [API Reference & Endpoint Catalog](#api-reference--endpoint-catalog)
   - [System Status & Observability](#system-status--observability)
   - [Authentication & Identity Management](#authentication--identity-management)
   - [Corporate Onboarding & Partner Companies](#corporate-onboarding--partner-companies)
   - [Base Catalog Registrations](#base-catalog-registrations)
   - [Driver Profile & Fixed Digital Signatures](#driver-profile--fixed-digital-signatures)
   - [Fleet & Vehicle Management](#fleet--vehicle-management)
   - [Trip Reports & Route Diary (6-Stage Lifecycle)](#trip-reports--route-diary-6-stage-lifecycle)
   - [Route Locations Catalog](#route-locations-catalog)
   - [Document Management & Storage](#document-management--storage)
   - [Asynchronous Batch Exports](#asynchronous-batch-exports)
   - [Route Driver Feedback](#route-driver-feedback)
   - [Transactional Outbox Relay](#transactional-outbox-relay)
   - [Temporary Analytics & Visualization Support](#temporary-analytics--visualization-support)
9. [Idempotency & Error Handling Standards](#idempotency--error-handling-standards)
10. [Configuration Reference](#configuration-reference)

---

## Overview

**Efficientia API** provides a backend platform designed for livestock transport logistics, tracking every phase of cattle shipment between producer ranches and slaughterhouse processing facilities. 

The system enforces strict operational integrity, regulatory compliance (GTA - *Guia de Trânsito Animal* and Fiscal Invoices), biometric/digital participant signatures, animal welfare metrics (mortality rates, distress, standing/lying counts, physical anomalies), and fleet sanity inspections.

The API serves two primary client platforms:
- **Mobile Applications (iOS / Android / Flutter / React Native):** For drivers and yard operators operating in intermittent connectivity conditions (offline-first draft sync, progressive route logging, and photo/signature uploads).
- **Web Management Portals:** For corporate fleet analysts, auditors, dispatch managers, and enterprise administrators monitoring live KPIs, approving trip reports, and conducting compliance audits.

---

## Key Architecture & Core Subsystems

```
                                  +---------------------------------------+
                                  |       Mobile App & Web Clients        |
                                  +---------------------------------------+
                                                      |
                                          (HTTPS / REST / JSON / Multipart)
                                                      |
                                                      v
+---------------------------------------------------------------------------------------------------+
| Efficientia Spring Boot 4 Application                                                             |
|                                                                                                   |
|  +------------------------+  +--------------------------+  +-----------------------------------+  |
|  | Security & Auth Layer  |  | Trip Diary Controller    |  | Fleet & Truck Allocator           |  |
|  | - JWT (HS256 / OAuth2) |  | - 6-Stage Form Lifecycle |  | - Tractor/Trailer Binding         |  |
|  | - Role Converter       |  | - Draft Auto-Resume      |  | - Inspection Validation Gate      |  |
|  +------------------------+  +--------------------------+  +-----------------------------------+  |
|                                                                                                   |
|  +------------------------+  +--------------------------+  +-----------------------------------+  |
|  | Digital Signature Svc  |  | Document Storage Engine  |  | Transactional Outbox Relay        |  |
|  | - Fixed Driver PNGs    |  | - Local File Quarantine  |  | - Relational Outbox Events        |  |
|  | - Magic-Bytes & SHA256 |  | - Async ZIP Packager     |  | - Redis Stream Dispatcher         |  |
|  +------------------------+  +--------------------------+  +-----------------------------------+  |
+---------------------------------------------------------------------------------------------------+
             |                                     |                                   |
             v                                     v                                   v
+---------------------------+        +---------------------------+       +--------------------------+
| PostgreSQL 15+            |        | Managed Storage           |       | Redis / Upstash Stream   |
| Multi-Schema Architecture |        | Local Encrypted Disk      |       | - Events Stream          |
| (Flyway V1 - V19)         |        | (/app/data/documentos)    |       | - Driver Leaderboard     |
+---------------------------+        +---------------------------+       +--------------------------+
```

### 1. Six-Stage Route Diary Lifecycle
The operational route diary (`RelatorioViagem`) captures cattle transport from property departure to dock unloading:
- **Stages:** Pre-trip configuration, loading & origin departure, unexpected transit stops, destination arrival & unloading, animal welfare evaluation, and digital sign-off.
- **Draft Resilience (`rascunho`):** Enables incremental saves so mobile drivers never lose field data when connectivity drops.
- **Audited Transition (`pendente` $\rightarrow$ `aprovado` / `reprovado`):** Strict consistency gates prevent final submission if required signatures, timestamps, odometer values, or cattle head counts fail sanity checks.

### 2. Digital Signatures & PNG Verification Engine
- Stores transparent PNG signatures directly into PostgreSQL `BYTEA` with versioning, timestamps, and SHA-256 verification.
- Enforces PNG magic-byte headers (`\x89PNG\r\n\x1a\n`) to prevent malicious payload uploads.
- Supports four distinct participant signature roles: **Rancher** (`pecuarista`), **Driver** (`motorista`), **Shunter** (`manobrista`), and **Corral Operator** (`curraleiro`).

### 3. Fleet Allocation & Sanitary Inspection Gatekeeper
- Dual-entity modeling for Prime Movers / Tractors (`veiculo_cavalo`) and Implement Trailers (`veiculo_carreta`).
- Validates that both vehicles possess active, non-expired sanitary and mechanical inspection certifications (`fn_validar_alocacao_viagem`) prior to trip binding.
- Prevents concurrent vehicle usage across simultaneous active trips.

### 4. Transactional Outbox Pattern & Redis Stream Relay
- Decouples trip persistence from asynchronous event dispatching.
- Persists domain events atomically in `sc_operacao.tb_outbox_evento`.
- The Outbox Relay service dispatches batches into Redis Streams (`efficientia:events:viagens`) for downstream analytics and driver gamification leaderboards.

---

## Technology Stack

| Layer | Component | Specification |
| :--- | :--- | :--- |
| **Runtime** | Java Development Kit (JDK) | OpenJDK 17 / Eclipse Temurin 17 |
| **Framework** | Spring Boot | 4.1.0 (`spring-boot-starter-web`, `data-jpa`, `validation`, `actuator`) |
| **Security** | Spring Security & OAuth2 | JWT Bearer Authentication (`spring-boot-starter-oauth2-resource-server`, HS256 / JWKS) |
| **Database** | PostgreSQL | Version 15+ (HikariCP connection pool) |
| **Schema Migration** | Flyway | Version 10+ (`flyway-database-postgresql`, Migrations V1 through V19) |
| **Caching & Messaging** | Redis | Spring Data Redis (Lettuce client) with Redis Streams and Sorted Sets |
| **Documentation** | SpringDoc OpenAPI | OpenAPI 3.0 with Swagger UI 3.1.0 |
| **Containerization** | Docker | Multi-stage build (`eclipse-temurin:17-jre-jammy`) |
| **Testing** | Testing Framework | JUnit 5, Mockito, Spring Security Test, Testcontainers PostgreSQL, H2 |

---

## Database & Schema Architecture

The relational database is segregated into dedicated domain schemas aligned with enterprise DDD principles:

```
├── public
│   ├── flyway_schema_history
│   ├── usuario / empresa / configuracao_operacao
│   ├── veiculo_cavalo / veiculo_carreta
│   ├── relatorio_viagem / parada_imprevista / anomalia_embarque / anomalia_desembarque
│   └── documento / exportacao / assinatura_motorista
├── sc_corporativo
│   ├── tb_empresa
│   ├── tb_usuario (with CNH, general signature, role, and profile photo)
│   └── tb_empresa_admin
├── sc_frota
│   ├── tb_veiculo_base / tb_veiculo_cavalo / tb_veiculo_carreta
│   └── tb_manutencao_inspecao
├── sc_operacao
│   ├── tb_relatorio_viagem (operational diary)
│   ├── tb_parada_imprevista
│   ├── tb_anomalia_embarque / tb_anomalia_desembarque
│   └── tb_outbox_evento (transactional outbox queue)
└── sc_auditoria
    ├── tb_auditoria_analise
    └── tb_monitoramento_dau (daily active user analytics)
```

Compatibility views in the `public` schema guarantee backward compatibility between legacy integer-indexed JPA entities and canonical multi-schema tables.

---

## Local Development Setup

### Prerequisites

Ensure you have the following software installed on your machine:
- **Java Development Kit (JDK) 17** or higher (`java -version`)
- **Apache Maven 3.9+** (or use the included `./mvnw` wrapper)
- **PostgreSQL 15+** running locally, on Docker, or via a managed cloud provider (e.g., Supabase / Neon / Aiven)
- **Git**
- *(Optional)* **Redis 7+** (Required if testing event streams; the application includes a non-blocking fallback if Redis is disabled).

---

### Environment Configuration

The application includes an environment variable template at the root directory: `.env.example`.

Create your local `.env` file by copying the template:

```bash
cp .env.example .env
```

Edit `.env` to configure your local credentials:

```dotenv
# ==============================================================================
# DATABASE CONFIGURATION (PostgreSQL / Local or Remote)
# ==============================================================================
SUPABASE_DB_URL=postgresql://localhost:5432/efficientia?sslmode=disable
SUPABASE_DB_USERNAME=postgres
SUPABASE_DB_PASSWORD=your_secure_password

DB_SCHEMA=public
DB_SCHEMAS=public,sc_corporativo,sc_frota,sc_operacao,sc_auditoria

# ==============================================================================
# APPLICATION STORAGE
# ==============================================================================
DOCUMENT_STORAGE_PATH=./data/documentos
MAX_PDF_SIZE=25MB
MAX_PNG_SIZE=10MB

# ==============================================================================
# SECURITY & JWT (Set to false for rapid offline local development)
# ==============================================================================
SECURITY_ENABLED=false
JWT_SECRET=efficientia-secret-key-must-be-at-least-32-bytes-long!
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# ==============================================================================
# REDIS (Optional for local development)
# ==============================================================================
REDIS_URL=redis://localhost:6379
REDIS_HEALTH_ENABLED=false

# ==============================================================================
# SERVER SETTINGS
# ==============================================================================
PORT=8080
SERVER_ADDRESS=0.0.0.0
```

> **Note on URL Normalization:**  
> The API includes an automated `DatabaseUrlEnvironmentPostProcessor` that accepts standard cloud connection formats (`postgres://user:pass@host:port/dbname` or `postgresql://...`) and converts them to standard JDBC strings (`jdbc:postgresql://host:port/dbname`) while extracting credentials dynamically.

---

### Database Initialization

Create the database in your local PostgreSQL instance:

```sql
CREATE DATABASE efficientia WITH OWNER postgres ENCODING 'UTF8';
```

When the application boots with `spring.profiles.default=local`, **Flyway automatically executes all migrations (`V1` through `V19`)**, provisions all necessary schemas (`sc_corporativo`, `sc_frota`, `sc_operacao`, `sc_auditoria`), and applies baseline sanity rules.

---

### Building and Running Locally

#### 1. Using Maven Wrapper

**Linux / macOS:**
```bash
./mvnw clean spring-boot:run
```

**Windows (PowerShell or CMD):**
```powershell
.\mvnw.cmd clean spring-boot:run
```

#### 2. Building the Production JAR

```bash
./mvnw clean package -DskipTests
java -jar target/efficientia-1.0.0.jar
```

The application will bind to port `8080` (or the port specified in `PORT`).

---

### Docker Deployment

A multi-stage `Dockerfile` is provided for containerized environments.

#### 1. Build the Docker Image

```bash
docker build -t efficientia-api:latest .
```

#### 2. Run the Container

```bash
docker run -d \
  --name efficientia-api \
  -p 8080:8080 \
  -e DATABASE_URL="postgresql://postgres:password@host.docker.internal:5432/efficientia" \
  -e SECURITY_ENABLED=true \
  -e JWT_SECRET="your-32-character-secret-key-goes-here" \
  efficientia-api:latest
```

---

### Running Automated Tests

The test suite contains unit tests, security converters tests, and Testcontainers integration tests:

```bash
# Execute unit and repository tests
./mvnw test

# Execute verification with coverage
./mvnw clean verify
```

---

## Interactive Documentation & Observability

Once the application is running locally:

| Resource | URL | Description |
| :--- | :--- | :--- |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | Interactive API explorer and client |
| **OpenAPI Specification (JSON)** | `http://localhost:8080/v3/api-docs` | OpenAPI 3.0 schema descriptor |
| **Health Check Endpoint** | `http://localhost:8080/actuator/health` | Service and component status |
| **Application Info** | `http://localhost:8080/actuator/info` | Application release and runtime metadata |
| **Prometheus Metrics** | `http://localhost:8080/actuator/prometheus` | Prometheus-formatted metric scrapers |
| **Public API Status** | `http://localhost:8080/api/v1/status` | Lightweight uptime and commit identifier |

---

## Authentication & Authorization Model

The API employs **Stateless JWT (JSON Web Token)** authentication based on the OAuth2 Resource Server pattern:

### Standard Headers

- **Protected Endpoints:** Require an `Authorization` header:
  ```http
  Authorization: Bearer <your_jwt_token>
  ```
- **State-Mutating Submissions:** Require an `Idempotency-Key` header with a valid UUID:
  ```http
  Idempotency-Key: e4a70e7a-9b16-4318-8f51-2292f7c2a714
  ```

### User Roles & Scopes

| Role | Domain Authority | Responsibilities |
| :--- | :--- | :--- |
| `ROLE_ADMIN` | `ADMIN`, `ADMINISTRADOR` | Corporate onboarding, employee creation, fleet additions, master entity configuration |
| `ROLE_MOTORISTA` | `MOTORISTA` | Route diary creation, progress tracking, personal fixed signature management, photo uploads |
| `ROLE_ANALISTA` | `ANALISTA` | Route audit, report approval/rejection, technical evaluation, document updates, outbox relay |
| `ROLE_MANOBRISTA` | `MANOBRISTA` | Operational yard assistance, vehicle maneuvering sign-off |
| `ROLE_PECUARISTA` | `PECUARISTA` | Cattle origin sign-off, ranch dispatch tracking |
| `ROLE_CURRALEIRO` | `CURRALEIRO` | Reception yard operator, unloading sign-off |
| `ROLE_FUNCIONARIO_FRIBOI` | `FUNCIONARIO_FRIBOI` | Corporate industrial operations, audit review |

---

## API Reference & Endpoint Catalog

### System Status & Observability

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/status` | Returns application health status, version, and deployment commit | Public |
| `GET` | `/actuator/health` | Deep health check (Database, Storage, HikariCP) | Public |
| `GET` | `/actuator/info` | Runtime application info | Public |
| `GET` | `/actuator/metrics` | Internal application telemetry | Public |
| `GET` | `/actuator/prometheus` | Prometheus scraping endpoint | Public |

---

### Authentication & Identity Management

Base path: `/api/v1/auth`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/signup` | Registers a new user account (Driver, Shunter, Analyst, etc.) | Public |
| `POST` | `/api/v1/auth/login` | Authenticates a user with CPF/Email + Password + Company Code; returns JWT | Public |
| `POST` | `/api/v1/auth/adm/login` | Administrator login (returns JWT with `ROLE_ADMIN`) | Public |
| `POST` | `/api/v1/auth/adm/primeiro-acesso` | First admin account setup (mandatory onboarding step for new companies) | Public |
| `POST` | `/api/v1/auth/empresa/login` | Validates company credentials / checks first-admin onboarding status | Public |

---

### Corporate Onboarding & Partner Companies

Base path: `/api/v1/empresas`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/empresas` | Registers a new transport company; generates 8-character unique code | Public |
| `GET` | `/api/v1/empresas` | Lists all partner companies | Protected |
| `GET` | `/api/v1/empresas/{id}` | Retrieves company details by database ID | Protected |
| `GET` | `/api/v1/empresas/codigo/{codigo}` | Retrieves public company profile by 8-digit corporate code | Public |
| `GET` | `/api/v1/empresas/cnpj/{cnpj}` | Retrieves company details by CNPJ | Public |
| `PUT` | `/api/v1/empresas/{id}` | Updates supplementary company details (Address, Phone, Step 1) | Public / Admin |
| `PATCH` | `/api/v1/empresas/{id}` | Partial update of company corporate data | Public / Admin |
| `PUT` | `/api/v1/empresas/codigo/{codigo}` | Updates company data using the 8-digit code | Public |
| `POST` | `/api/v1/empresas/{id}/logo` | Uploads company branding logo (PNG / SVG up to 5 MB) | Public / Admin |
| `GET` | `/api/v1/empresas/{id}/logo/conteudo` | Streams company logo binary image | Public |
| `POST` | `/api/v1/empresas/{empresaId}/adms` | Registers additional company administrators | `ROLE_ADMIN` |
| `GET` | `/api/v1/empresas/{empresaId}/adms` | Lists all administrators belonging to the company | `ROLE_ADMIN` |
| `POST` | `/api/v1/empresas/{empresaId}/funcionarios` | Enrolls employees (drivers, analysts) under the company | `ROLE_ADMIN` |
| `GET` | `/api/v1/empresas/{empresaId}/funcionarios` | Lists company employees | `ROLE_ADMIN` |

---

### Base Catalog Registrations

Base path: `/api/v1`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/usuarios` | Creates an operational user | Protected |
| `POST` | `/api/v1/enderecos` | Registers a geographical postal address | Protected |
| `POST` | `/api/v1/fazendas` | Registers a cattle producer ranch / farm | Protected |
| `POST` | `/api/v1/veiculos/cavalos` | Registers a prime mover / tractor unit | Protected |
| `GET` | `/api/v1/veiculos/cavalos` | Lists all prime movers with detailed specifications | Protected |
| `GET` | `/api/v1/veiculos/cavalos/{id}` | Retrieves a prime mover by ID | Protected |
| `PUT` | `/api/v1/veiculos/cavalos/{id}` | Updates prime mover metadata | Protected |
| `DELETE` | `/api/v1/veiculos/cavalos/{id}` | Soft-deletes a prime mover | Protected |
| `POST` | `/api/v1/veiculos/carretas` | Registers a cattle trailer implement | Protected |
| `GET` | `/api/v1/veiculos/carretas` | Lists all cattle trailers | Protected |
| `GET` | `/api/v1/veiculos/carretas/{id}` | Retrieves a cattle trailer by ID | Protected |
| `PUT` | `/api/v1/veiculos/carretas/{id}` | Updates cattle trailer metadata | Protected |
| `DELETE` | `/api/v1/veiculos/carretas/{id}` | Soft-deletes a cattle trailer | Protected |

---

### Driver Profile & Fixed Digital Signatures

Base path: `/api/v1/usuarios`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `PUT` | `/api/v1/usuarios/me/assinatura` | Uploads/updates the driver's fixed profile signature (PNG, max 1 MB) | `ROLE_MOTORISTA` |
| `GET` | `/api/v1/usuarios/me/assinatura` | Retrieves metadata of the authenticated driver's fixed signature | `ROLE_MOTORISTA` |
| `GET` | `/api/v1/usuarios/me/assinatura/conteudo` | Streams the authenticated driver's fixed signature PNG | `ROLE_MOTORISTA` |
| `PUT` | `/api/v1/usuarios/{motoristaId}/assinatura`| Administrative upload of driver fixed signature | `ROLE_ADMIN` |
| `GET` | `/api/v1/usuarios/{usuarioId}/assinatura` | Retrieves metadata of any driver's signature for audit | Admin / Analyst |
| `GET` | `/api/v1/usuarios/{usuarioId}/assinatura/conteudo` | Streams any driver's signature PNG | Admin / Analyst |
| `PUT` | `/api/v1/usuarios/me/foto` | Uploads driver profile photo (`multipart/form-data`, JPEG/PNG, max 2 MB) | `ROLE_MOTORISTA` |
| `GET` | `/api/v1/usuarios/me/foto/conteudo` | Streams driver profile photo binary | `ROLE_MOTORISTA` |
| `DELETE` | `/api/v1/usuarios/me/foto` | Deletes driver profile photo | `ROLE_MOTORISTA` |

---

### Fleet & Vehicle Management

Base path: `/api/v1/caminhoes`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/caminhoes` | Registers a vehicle (Tractor `CAVALO`, Trailer `CARRETA`, or `CONJUNTO`) | Protected |
| `GET` | `/api/v1/caminhoes` | Lists fleet vehicles with dynamic allocation status (`DISPONIVEL`/`EM_USO`) | Protected |
| `GET` | `/api/v1/caminhoes/{id}` | Retrieves vehicle by ID | Protected |
| `GET` | `/api/v1/caminhoes/placa/{placa}` | Retrieves vehicle by license plate | Protected |
| `PUT` | `/api/v1/caminhoes/{id}` | Updates vehicle specifications | Protected |
| `DELETE` | `/api/v1/caminhoes/{id}` | Deactivates vehicle | Protected |
| `GET` | `/api/v1/caminhoes/app` | Mobile-optimized vehicle listing with inspection validity indicators | Protected |
| `GET` | `/api/v1/caminhoes/disponiveis` | Lists available vehicles with active inspections not in use | Protected |
| `GET` | `/api/v1/caminhoes/relatorio/{relatorioId}`| Retrieves vehicles associated with a specific trip report | Protected |
| `GET` | `/api/v1/caminhoes/motorista/{motoristaId}`| Retrieves the active truck assigned to a driver | Protected |
| `POST` | `/api/v1/caminhoes/relatorio/{relatorioId}/vincular` | Allocates tractor & trailer to a trip (enforces inspection checks) | Protected |

---

### Trip Reports & Route Diary (6-Stage Lifecycle)

Base path: `/api/v1/relatorios-viagem`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/relatorios-viagem` | Creates a new trip report (draft `rascunho` or submitted `pendente`) | `ROLE_MOTORISTA` |
| `PUT` | `/api/v1/relatorios-viagem/{id}` | Incrementally saves or submits a trip report | `ROLE_MOTORISTA` |
| `GET` | `/api/v1/relatorios-viagem` | Paginated listing of trip reports with filters (`status`, `gta`, `pagina`, `tamanho`) | Protected |
| `GET` | `/api/v1/relatorios-viagem/me` | Paginated listing of trips created by the authenticated driver | `ROLE_MOTORISTA` |
| `GET` | `/api/v1/relatorios-viagem/{id}` | Retrieves full trip report details including stops and anomalies | Protected |
| `DELETE`| `/api/v1/relatorios-viagem/{id}` | Logically deletes an unsubmitted draft report belonging to the driver | `ROLE_MOTORISTA` |
| `GET` | `/api/v1/relatorios-viagem/{id}/caminhao` | Retrieves the truck composition assigned to this report | Protected |
| `POST` | `/api/v1/relatorios-viagem/{id}/vincular-caminhao` | Binds tractor and trailer plates to the report | Protected |
| `PATCH`| `/api/v1/relatorios-viagem/{id}/finalizar` | Finalizes approved report and releases vehicles back to pool | Protected |
| `PATCH`| `/api/v1/relatorios-viagem/{id}/status` | Updates trip status (`rascunho`, `pendente`, `aprovado`, `concluido`, `reprovado`) | Protected |
| `PATCH`| `/api/v1/relatorios-viagem/{id}/enviar` | Submits draft report for final auditor analysis (`pendente`) | `ROLE_MOTORISTA` |
| `PATCH`| `/api/v1/relatorios-viagem/{id}/assinaturas` | Batch assigns digital signature references | Protected |
| `POST` | `/api/v1/relatorios-viagem/{id}/assinar-papel` | Records a single role signature assignment | Protected |
| `PUT` | `/api/v1/relatorios-viagem/{id}/assinaturas/{papel}` | Uploads participant PNG signature (`pecuarista`, `motorista`, `manobrista`, `curraleiro`) | Protected |
| `GET` | `/api/v1/relatorios-viagem/{id}/assinaturas/{papel}/conteudo` | Streams participant signature PNG | Protected |

---

### Route Locations Catalog

Base path: `/api/v1/locais-diario`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/locais-diario/fazendas` | Search ranches/farms by name (`?q=...&pagina=0&tamanho=20`) | Protected |
| `GET` | `/api/v1/locais-diario/unidades-frigorificas` | Search processing plants by name (`?q=...&pagina=0&tamanho=20`) | Protected |

---

### Document Management & Storage

Base path: `/api/v1/documentos`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/documentos` (`multipart/form-data`) | Uploads PDF/PNG file associated with a trip (enforces `Idempotency-Key`) | Protected |
| `POST` | `/api/v1/documentos` (`application/json`) | Creates accessible textual signature record | Protected |
| `GET` | `/api/v1/documentos` | Queries documents (supports pagination and opaque cursor) | Protected |
| `GET` | `/api/v1/documentos/{id}` | Retrieves document metadata by UUID | Protected |
| `GET` | `/api/v1/documentos/{id}/conteudo` | Downloads or previews document (`?inline=true\|false`) | Protected |
| `PATCH`| `/api/v1/documentos/{id}` | Optimistic concurrency update of document description | Analyst / Admin |
| `DELETE`| `/api/v1/documentos/{id}` | Soft-deletes document record and removes physical binary | Analyst / Admin |

---

### Asynchronous Batch Exports

Base path: `/api/v1/exportacoes`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/exportacoes` | Requests asynchronous ZIP export of multiple document UUIDs | Protected |
| `GET` | `/api/v1/exportacoes/{id}` | Checks export packaging status (`PENDENTE`, `EM_PROCESSAMENTO`, `CONCLUIDO`, `ERRO`) | Protected |
| `GET` | `/api/v1/exportacoes/{id}/conteudo` | Downloads packaged `.zip` archive upon completion | Protected |

---

### Route Driver Feedback

Base path: `/api/v1/feedbacks-rota`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/feedbacks-rota` | Submits driver evaluation, road condition ratings, and comments | Protected |

---

### Transactional Outbox Relay

Base path: `/api/v1/outbox`

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/outbox/relay` | Triggers immediate batch publication of pending outbox events to Redis Stream | Analyst / Admin |
| `GET` | `/api/v1/outbox/pendentes` | Returns total count of pending outbox events awaiting publication | Analyst / Admin |

---

### Temporary Analytics & Visualization Support

Base path: `/api/v1/temp`  
*(Provided to support frontend chart generation and KPI dashboards prior to direct BI integration).*

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/temp/tabelas` | Catalog of all database tables with record counts and schema metadata | Public |
| `GET` | `/api/v1/temp/tabelas/{tabela}` | Paginated table records with SQL injection sanitization | Public |
| `GET` | `/api/v1/temp/todas` | Consolidated JSON dataset across all core tables | Public |
| `GET` | `/api/v1/temp/graficos/metricas-resumo` | Aggregated analytics (trips by status, mortality rates, anomalies) | Public |
| `GET` | `/api/v1/temp/relatorios-viagem` | Table view of trip reports | Public |
| `GET` | `/api/v1/temp/paradas-imprevistas` | Table view of unforeseen stops | Public |
| `GET` | `/api/v1/temp/anomalias-embarque` | Table view of loading anomalies | Public |
| `GET` | `/api/v1/temp/anomalias-desembarque`| Table view of unloading anomalies | Public |
| `GET` | `/api/v1/temp/usuarios` | Table view of users (passwords sanitized) | Public |
| `GET` | `/api/v1/temp/enderecos` | Table view of addresses | Public |
| `GET` | `/api/v1/temp/fazendas` | Table view of ranches | Public |
| `GET` | `/api/v1/temp/unidades-frigorificas` | Table view of processing plants | Public |
| `GET` | `/api/v1/temp/veiculos-cavalo` | Table view of prime movers | Public |
| `GET` | `/api/v1/temp/veiculos-carreta` | Table view of trailers | Public |
| `GET` | `/api/v1/temp/documentos` | Table view of documents | Public |
| `GET` | `/api/v1/temp/exportacoes` | Table view of export jobs | Public |
| `GET` | `/api/v1/temp/assinaturas-motorista` | Table view of driver signatures | Public |
| `GET` | `/api/v1/temp/empresas` | Table view of companies | Public |
| `GET` | `/api/v1/temp/outbox-eventos` | Table view of outbox queue events | Public |

---

## Idempotency & Error Handling Standards

### RFC 7807 Problem Detail Specifications
All validation errors and domain exceptions return RFC 7807 compliant `ProblemDetail` structures:

```json
{
  "type": "about:blank",
  "title": "Erro de validação dos dados do relatório",
  "status": 400,
  "detail": "Um ou mais campos contêm erros de validação.",
  "instance": "/api/v1/relatorios-viagem",
  "fieldErrors": {
    "horarioChegadaUnidade": "O horário de chegada não pode ser anterior ao horário de saída da propriedade.",
    "quantidadeAnimais": "A soma de machos, fêmeas e marrucos deve equivaler à soma de animais em pé, deitados e mortos."
  }
}
```

### Incomplete Signature Validation (HTTP 422)
When submitting a trip report without required participant signatures, the API responds with `422 Unprocessable Entity` listing the missing parties:

```json
{
  "title": "Assinaturas incompletas",
  "status": 422,
  "detail": "O diário de rota exige assinaturas válidas antes de ser submetido.",
  "papeisFaltantes": ["pecuarista", "curraleiro"]
}
```

### Common HTTP Status Codes

| Code | Meaning | Typical Trigger |
| :--- | :--- | :--- |
| `200 OK` | Request succeeded | Successful query, resource retrieval, or update |
| `201 Created` | Resource created | Successful registration, report creation, or file upload |
| `202 Accepted` | Accepted for processing | Asynchronous export job queued |
| `204 No Content` | Succeeded without body | Successful resource deletion |
| `400 Bad Request` | Validation failure | Malformed payload, chronological inconsistency, invalid values |
| `401 Unauthorized` | Missing / invalid token | Expired or missing Bearer JWT |
| `403 Forbidden` | Access denied | Insufficient permissions / role violation |
| `404 Not Found` | Entity not found | Target ID or UUID does not exist |
| `409 Conflict` | State conflict | Attempting to delete submitted report, duplicate key |
| `413 Payload Too Large` | File size exceeded | Upload exceeds configured threshold (e.g. PNG > 1 MB) |
| `415 Unsupported Media` | Unrecognized format | File failed magic-byte verification (non-PNG or non-PDF) |
| `422 Unprocessable` | Domain rule violation | Vehicle inspection expired, missing mandatory signatures |

---

## Configuration Reference

The following properties can be customized via `.env` or system environment variables:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | Port on which the Spring Boot application listens |
| `SERVER_ADDRESS` | `0.0.0.0` | Host IP binding address |
| `SUPABASE_DB_URL` / `DATABASE_URL` | *(None)* | PostgreSQL JDBC URL or standard connection URI |
| `SUPABASE_DB_USERNAME` | `postgres` | Database username |
| `SUPABASE_DB_PASSWORD` | *(None)* | Database user password |
| `DB_SCHEMA` | `public` | Default PostgreSQL schema for JPA metadata |
| `DB_SCHEMAS` | `public,sc_corporativo,sc_frota,sc_operacao,sc_auditoria` | Comma-delimited list of schemas managed by Flyway |
| `DB_POOL_MAX_SIZE` | `5` | Maximum connections in the HikariCP pool |
| `DB_POOL_MIN_IDLE` | `1` | Minimum idle connections retained by HikariCP |
| `DOCUMENT_STORAGE_PATH` | `./data/documentos` | Local filesystem path for stored files and signatures |
| `MAX_PDF_SIZE` | `25MB` | Maximum file size for PDF attachments |
| `MAX_PNG_SIZE` | `10MB` | Maximum file size for PNG attachments |
| `SECURITY_ENABLED` | `true` | Enables or disables Spring Security filter chain |
| `JWT_SECRET` | *(32+ char key)* | Secret key for HS256 HMAC JWT signature generation and verification |
| `JWT_ISSUER_URI` | `efficientia-api` | Expected issuer URI of inbound JWTs |
| `JWT_AUDIENCE` | `efficientia-api` | Expected audience identifier |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated list of allowed CORS origins |
| `REDIS_URL` | *(Empty)* | Connection URI for Redis instance (`redis://` or `rediss://`) |
| `REDIS_STREAM_VIAGENS` | `efficientia:events:viagens` | Redis Stream topic name for travel events |
| `REDIS_LEADERBOARD_KEY`| `efficientia:leaderboard:motoristas` | Redis Sorted Set key for driver rankings |
| `REDIS_HEALTH_ENABLED` | `false` | Enables Redis connectivity checks within `/actuator/health` |

---

## License & Governance

This project is developed under the academic and professional software architecture curriculum of **Instituto J&F / Ânima Educação**. All proprietary business rules, database models, and operational assets belong to the respective stakeholders.
