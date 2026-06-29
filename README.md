# Patient Management System

A microservices-based patient management system built with Spring Boot, Angular, and Docker.

## Architecture

| Service | Description | Port |
|---------|-------------|------|
| `api-gateway` | Entry point, JWT validation, request routing | 4004 |
| `patient-service` | Patient CRUD, gRPC server | 4000 |
| `treatment-service` | Treatment & category management, gRPC server | 4003 |
| `billing-service` | Billing & charges, gRPC client | 4001 |
| `analytics-service` | Event-driven analytics with TimescaleDB | 4002 |
| `search-service` | Full-text search via OpenSearch | 8080 |

## Tech Stack

**Backend**
- Java 21 + Spring Boot 3
- Spring Cloud Gateway (API Gateway)
- gRPC (inter-service communication)
- Apache Kafka (event streaming)
- PostgreSQL (per-service databases)
- TimescaleDB (analytics time-series data)
- OpenSearch (full-text search)
- Keycloak (authentication & authorisation)
- Protocol Buffers

**Frontend**
- Angular 19
- Tailwind CSS
- Keycloak-js

**Infrastructure**
- Docker Compose
- Nginx (reverse proxy + SSL termination)
- Cloudflare Pages (frontend hosting)
- Cloudflare DNS
- Oracle Cloud VM

## Services Communication

```
Browser → Cloudflare Pages (frontend)
Browser → Nginx → API Gateway → Services
                              → Keycloak (auth)
Services → Kafka → Analytics, Search
Patient  → gRPC  → Billing
Billing  → gRPC  → Treatment
```

## Running Locally

### Prerequisites
- Docker & Docker Compose
- Java 21
- Node.js 18+

### Setup

1. Clone the repository
2. Copy `.env.example` to `.env` and fill in the values
3. Start all services:

```bash
docker compose up -d
```

4. Run the frontend:

```bash
cd patient-frontend
npm install
ng serve
```

The app will be available at `http://localhost:4200`.

## Environment Variables

| Variable | Description |
|----------|-------------|
| `PATIENT_DB_PASSWORD` | Patient service database password |
| `BILLING_DB_PASSWORD` | Billing service database password |
| `TREATMENT_DB_PASSWORD` | Treatment service database password |
| `ANALYTICS_DB_PASSWORD` | Analytics service database password |
| `KEYCLOAK_DB_PASSWORD` | Keycloak database password |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin console password |
| `LOCALSTACK_AUTH_TOKEN` | LocalStack Pro auth token |

## Production

- Frontend: [patientsystem.me](https://patientsystem.me)
- API & Auth: [api.patientsystem.me](https://api.patientsystem.me)
