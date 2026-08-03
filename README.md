# Patient System

A multi-tenant patient management platform built as a production-grade microservices system on AWS. Organisations register patients, track treatments, manage billing, import data in bulk, and monitor real-time analytics — all isolated per tenant using a shared infrastructure.

For a full visual overview of the architecture, open **[patient-system-architecture.excalidraw](./patient-system-architecture.excalidraw)** in [Excalidraw](https://excalidraw.com). It shows every service, AWS resource, network zone, and communication path in a single left-to-right diagram.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Repository Layout](#repository-layout)
3. [Microservices](#microservices)
4. [Infrastructure](#infrastructure)
5. [Observability](#observability)
6. [GitOps with ArgoCD](#gitops-with-argocd)
7. [CI/CD Pipeline](#cicd-pipeline)
8. [Provisioning from Scratch](#provisioning-from-scratch)
9. [Tearing Down](#tearing-down)
10. [Required Secrets and Variables](#required-secrets-and-variables)
11. [Security Model](#security-model)

---

## Architecture Overview

```
Browser / API client
  │
  ├─ patientsystem.me       (Angular SPA, served by Cloudflare Pages)
  └─ api.patientsystem.me   (REST API)
       │
       ▼
   Cloudflare DNS → AWS ALB (HTTPS/443, HTTP→HTTPS redirect)
       │
       ▼
   api-gateway  (Spring Cloud Gateway, port 4004)
       │  JWT validation + X-Organization-Id header injection
       │
       ├─► patient-service        REST + gRPC (9003) + Kafka producer + SQS consumer
       ├─► treatment-service      REST + gRPC (9002) + Kafka producer
       ├─► billing-service        REST + gRPC consumer (billing+treatment) + Kafka producer
       ├─► search-service         REST + SSE + OpenSearch + Kafka consumer
       ├─► analytics-service      REST + Kafka consumer (time-series in PostgreSQL)
       └─► organization-service   REST + Cognito admin + gRPC server

Async backbone: Amazon MSK (Kafka)
  Topics: patient-events, treatment-events, billing-events, import-events

Data stores (all in private subnets):
  RDS PostgreSQL   — one database per service (5 total)
  Amazon MSK       — Kafka 3.x, multi-AZ
  Amazon OpenSearch — search-service index
  Redis (in-cluster) — api-gateway rate limiting / session cache

Supporting AWS services:
  Cognito       — user pool, Google OAuth federation, custom:organizationId claim
  S3            — patient CSV file uploads for bulk import
  SQS           — import job queue
  ECR           — Docker image registry (one repo per service)
  Secrets Manager — single secret patient-system/prod, synced to K8s by ESO
```

All workloads run on **Amazon EKS** (Kubernetes 1.32) in the `eu-west-1` region across three private subnets.

---

## Repository Layout

```
patient-system/
├── proto/                      Protobuf definitions (Kafka events + gRPC)
│   └── src/main/proto/
│       ├── patient_kafka.proto
│       ├── treatment_kafka.proto
│       ├── billing_kafka.proto
│       ├── import_kafka.proto
│       ├── patient_grpc.proto
│       ├── treatment_grpc.proto
│       ├── billing_grpc.proto
│       └── organization_grpc.proto
│
├── api-gateway/                Spring Cloud Gateway
├── patient-service/            Patient CRUD + bulk CSV import
├── treatment-service/          Treatment + category management
├── billing-service/            Billing accounts + charges + PDF invoices
├── search-service/             OpenSearch-backed search + SSE notifications
├── analytics-service/          Real-time dashboard metrics
├── organization-service/       Organisation registration + Cognito provisioning
├── patient-frontend/           Angular 21 SPA
│
├── k8s/
│   ├── services/               Deployment + Service + ServiceAccount YAMLs per microservice
│   │                           (managed by ArgoCD)
│   ├── secrets/                ExternalSecret + SecretStore YAMLs (managed by ArgoCD)
│   └── argocd/                 ArgoCD Application CRDs and namespaces
│
├── terraform/
│   ├── environments/prod/      Root module: wires all modules together
│   └── modules/
│       ├── vpc/                VPC, subnets (public × 2, private × 3), NAT gateway
│       ├── eks/                EKS cluster + managed node group + OIDC provider
│       ├── ecr/                ECR repos + GitHub Actions OIDC IAM role
│       ├── cognito/            User pool, app client, Google IdP, hosted UI
│       ├── rds/                Multi-AZ PostgreSQL 16 instance
│       ├── msk/                MSK Kafka cluster (multi-AZ)
│       ├── opensearch/         OpenSearch domain
│       ├── s3/                 Patient file uploads bucket
│       ├── sqs/                SQS FIFO import queue
│       ├── alb-controller/     ALB Ingress Controller Helm + ACM certificate
│       ├── app-config/         Secrets Manager secret + ESO Helm + IRSA roles
│       ├── monitoring/         kube-prometheus-stack + Loki + Tempo Helm charts
│       └── argocd/             ArgoCD Helm chart + Application CRDs
│
├── .github/workflows/
│   ├── ci.yml                  Pull-request gate (tests, Trivy, npm audit)
│   └── deploy.yml              Push-to-main deployment pipeline
│
└── patient-system-architecture.excalidraw   Full architecture diagram
```

---

## Microservices

All services are Spring Boot 3 / Java 25 applications. They share a Maven parent POM (`pom.xml`) and the `proto` module for Protobuf-generated sources. Each service runs as its own Kubernetes Deployment with a dedicated PostgreSQL database.

### api-gateway

Spring Cloud Gateway routing all external traffic to downstream services. Validates JWT tokens issued by Cognito, extracts the `custom:organizationId` claim, and forwards it downstream as the `X-Organization-Id` header so every service knows which tenant's data to scope queries to. Also enforces CORS and rate limiting via Redis.

**Routing table**

| Path prefix | Upstream service |
|---|---|
| `/api/patients` | patient-service:4001 |
| `/api/treatments` | treatment-service:4002 |
| `/api/billing` | billing-service:4003 |
| `/api/search` | search-service:4005 |
| `/api/analytics` | analytics-service:4006 |
| `/api/organizations` | organization-service:4007 |

### patient-service

Manages the patient lifecycle for each organisation. Provides full CRUD over patients and handles the bulk CSV import pipeline:

1. Frontend uploads a CSV to S3 and calls `/api/patients/import/start` with a column→field mapping.
2. Service creates an `ImportJob` record, publishes an `ImportEvent` to Kafka (`import-events`), and drops an `ImportSqsMessage` on the SQS queue containing the S3 key and mapping.
3. `@SqsListener` picks up the message (potentially in a different pod), downloads the file from S3, and processes it in chunks of 5 000 rows, persisting each chunk and emitting Kafka `patient-events` (`PATIENT_CREATED`) for every new patient.
4. Progress and final status (COMPLETED / FAILED, row counts, per-row errors) are stored in the `ImportJob` table.

Also exposes a gRPC server on port 9003 for internal `GetPatient` calls from billing-service and search-service.

IRSA-backed ServiceAccount grants this pod read/write access to the S3 bucket and SQS queue without long-lived credentials.

### treatment-service

Manages treatments and their categories. Each treatment belongs to a category and has a name and price. On creation/update a `TreatmentEvent` is published to the `treatment-events` Kafka topic. The service exposes a gRPC endpoint (`GetTreatment`) on port 9002 consumed by billing-service to enrich charge records.

### billing-service

Creates and manages billing accounts (one per patient, provisioned automatically when a `PATIENT_CREATED` event arrives via Kafka). Accepts charge requests that reference a treatment; the service resolves both the patient details (via gRPC to patient-service:9003) and the treatment price/category (via gRPC to treatment-service:9002) before persisting the charge. Generates PDF invoices using iText and streams them as downloadable responses. Publishes `BillingEvent` messages to the `billing-events` topic consumed by analytics-service.

### search-service

Indexes patients and treatments in Amazon OpenSearch using a `search_as_you_type` field mapping that generates `_2gram` and `_3gram` subfields for prefix matching. Consumes `patient-events` and `treatment-events` from Kafka to keep the index in sync.

Real-time notifications are delivered via Server-Sent Events (SSE). An `SseEmitterService` maintains per-user emitters; when a Kafka consumer processes an event it pushes a notification through the matching emitter. The Angular frontend subscribes using `EventSourcePolyfill`.

### analytics-service

Consumes `patient-events` and `billing-events` from Kafka and persists them into two time-series-friendly tables (`patient_event`, `charge_event`) with covering indexes on timestamp, event type, gender, and category. Exposes a REST dashboard API with endpoints for:

- Active patient count
- Monthly patient registrations (time series)
- Average patient age
- Revenue totals
- Most-used treatments
- Gender distribution

### organization-service

Handles organisation onboarding. When a new organisation registers, the service creates a Cognito user group for that organisation and sets the `custom:organizationId` attribute on the calling user via the Cognito Admin SDK. Uses IRSA to assume an IAM role with `cognito-idp:AdminUpdateUserAttributes` and related permissions — no static credentials in the pod.

---

## Infrastructure

All infrastructure is Terraform-managed (`terraform/environments/prod`). No remote backend is configured; state is local. The modules are designed to be applied in phases since the Helm and Kubernetes providers require an active EKS cluster before they can run.

### VPC

Single VPC (`10.0.0.0/16`) in `eu-west-1` with:
- 2 public subnets (NAT gateway, ALB)
- 3 private subnets (EKS nodes, RDS, MSK, OpenSearch)
- Internet gateway + NAT gateway for outbound access from private subnets

### EKS

Managed node group (on-demand, `t3.medium`, 1–3 nodes, autoscaling). OIDC provider enabled for IRSA. The GitHub Actions IAM role is added to `aws-auth` so the deploy workflow can run `kubectl` without a separate kubeconfig secret.

### ECR

One repository per microservice plus one for the frontend. A GitHub Actions IAM role trusts the GitHub OIDC provider (`token.actions.githubusercontent.com`) so workflows authenticate via OIDC — no `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` secrets.

### RDS

Single PostgreSQL 16 Multi-AZ instance (`db.t3.medium`, 20 GB gp3). Five databases — one per service (`patient_db`, `treatment_db`, `billing_db`, `search_db`, `analytics_db`) — created by `init_databases.sh` on first provisioning. Security group restricts access to EKS node CIDR only.

### MSK

Kafka 3.x cluster, two broker nodes across two private subnets. `PLAINTEXT` listener on port 9092. Topics are created by the services on startup. Security group restricts access to VPC CIDR.

### OpenSearch

Single-node domain (`t3.small.search`, 10 GB) in the private subnet. Fine-grained access control disabled; network policy restricts access to VPC. The search-service pod connects over HTTPS on port 443.

### ALB and Ingress

The AWS Load Balancer Controller runs in `kube-system` (Helm chart 1.7.1) with its own IRSA role. A single ACM certificate covers both `patientsystem.me` and `api.patientsystem.me` (SAN). The Kubernetes Ingress resource is managed by Terraform (`kubernetes_ingress_v1`) so the certificate ARN is never hardcoded. The ALB redirects HTTP to HTTPS and forwards traffic to the `api-gateway` Service on port 4004.

### Secrets Manager + External Secrets Operator

A single AWS Secrets Manager secret (`patient-system/prod`) holds all runtime configuration: RDS endpoint, MSK bootstrap brokers, OpenSearch endpoint, Cognito pool ID, S3 bucket name, SQS URL, and database password. Terraform's `app-config` module writes this secret from module outputs so it is always in sync after a `terraform apply`.

The External Secrets Operator (ESO) runs in `external-secrets` namespace (Helm) and syncs the Secrets Manager secret into a Kubernetes Secret (`patient-system-secrets`) in the `patient-system` namespace every hour. All services consume credentials via `secretKeyRef` env vars referencing this Secret.

> **Note:** `secretKeyRef` values are read once at pod startup. If the MSK cluster is recreated (changing broker addresses), the Secrets Manager secret is updated automatically by Terraform, ESO refreshes the K8s Secret, but running pods will not pick up the change until restarted. Run `kubectl rollout restart deployment -n patient-system` after any MSK replacement.

### IRSA (IAM Roles for Service Accounts)

Two service-specific IAM roles are created by the `app-config` module and annotated on their respective ServiceAccounts:

| Service | IAM permissions |
|---|---|
| patient-service | `s3:GetObject`, `s3:PutObject`, `sqs:SendMessage`, `sqs:ReceiveMessage`, `sqs:DeleteMessage` |
| organization-service | `cognito-idp:AdminUpdateUserAttributes`, `cognito-idp:AdminAddUserToGroup`, `cognito-idp:CreateGroup` |

### Cognito

User pool `patient-system-prod` with:
- Google as federated identity provider (OAuth 2.0)
- Hosted UI at `patient-system.auth.eu-west-1.amazoncognito.com`
- A custom attribute `custom:organizationId` set by organization-service at registration
- A dedicated E2E test user (email/password) for Playwright automation

---

## Observability

The `monitoring` Terraform module deploys the full observability stack into the `monitoring` namespace using Helm:

| Component | Helm chart | Version | Purpose |
|---|---|---|---|
| Prometheus | kube-prometheus-stack | 57.0.0 | Metrics scraping + alerting rules |
| Grafana | (bundled) | — | Dashboards (connected to Prometheus, Loki, Tempo) |
| Alertmanager | (bundled) | — | Alert routing |
| Loki | loki-stack | 2.10.2 | Log aggregation (Promtail ships container logs) |
| Tempo | tempo | 1.7.1 | Distributed tracing backend |

All microservices run the **OpenTelemetry Java agent** (`-javaagent:/otel/opentelemetry-javaagent.jar`) as a JVM argument injected via the Deployment spec. Traces are exported to `http://$(HOST_IP):4317` (OTLP gRPC to the Tempo collector on each node). Metrics are scraped by Prometheus via `ServiceMonitor` resources.

Grafana is accessible at `grafana.patientsystem.me` (ALB Ingress in the `monitoring` namespace, separate from the app Ingress).

---

## GitOps with ArgoCD

ArgoCD runs in the `argocd` namespace (Helm chart 6.7.3) and manages Kubernetes resources declaratively from this repository. It is accessible at `argocd.patientsystem.me`.

Two ArgoCD Applications are defined in `k8s/argocd/applications/`:

| Application | Source path | Sync scope |
|---|---|---|
| `patient-system` | `k8s/services/` (recursive) | All Deployment, Service, ServiceAccount, ConfigMap YAMLs |
| `patient-system-infra` | `k8s/` (non-recursive) | `namespaces.yaml`, `external-secrets.yaml` |

The deploy pipeline (see below) commits updated image tags directly to the `k8s/services/<service>/deployment.yaml` files and pushes to `main`. ArgoCD detects the diff and syncs the change to the cluster. The ALB Ingress is managed by Terraform, not ArgoCD, so it is not in either Application's sync scope.

---

## CI/CD Pipeline

### ci.yml — Pull Request gate

Runs on every push and pull request to `main`. Three parallel jobs:

1. **backend-tests** — Maven build with Java 25, runs unit tests and integration tests (Testcontainers PostgreSQL), fails on any test failure
2. **frontend-tests** — `npm ci` + Angular Jasmine/Karma test suite + `npm audit` for known CVEs
3. **security-scan** — Trivy SCA scan across the entire monorepo, blocks on HIGH/CRITICAL CVEs

### deploy.yml — Main branch deployment

Triggers on push to `main`. Authenticates to AWS via GitHub OIDC (no static credentials).

**Steps:**

1. **Detect changed services** — diffs the commit against `HEAD~1`, maps changed directories to service names
2. **Build and push Docker images** (parallel matrix per changed service):
   - `docker build` with `--build-arg` for the service name
   - Trivy image scan (blocks on CRITICAL)
   - `docker push` to ECR using the commit SHA as the image tag
3. **Commit updated image tags** — patches `k8s/services/<service>/deployment.yaml` with the new SHA tag and commits back to `main` (ArgoCD picks this up on its next sync)
4. **Apply secrets** — `kubectl apply -f k8s/secrets/` to ensure ESO resources are current
5. **Run Playwright E2E tests** — authenticates as the Cognito E2E test user and exercises the golden paths

> The frontend is built separately and deployed to Cloudflare Pages; it is not part of the Docker/EKS pipeline.

---

## Provisioning from Scratch

Prerequisites: AWS CLI configured, `terraform` ≥ 1.6, `kubectl`, `helm`, `aws-iam-authenticator`.

### Step 1 — Bootstrap EKS (required first)

The Helm and Kubernetes Terraform providers need a live cluster. Apply EKS and its dependencies first:

```bash
cd terraform/environments/prod

terraform init

terraform apply \
  -target=module.vpc \
  -target=module.eks \
  -target=module.ecr
```

### Step 2 — Configure kubectl

```bash
aws eks update-kubeconfig \
  --region eu-west-1 \
  --name $(terraform output -raw eks_cluster_name)
```

### Step 3 — Apply everything else

```bash
terraform apply
```

Supply the required variables (see [Required Secrets and Variables](#required-secrets-and-variables)). This single apply provisions:
- RDS, MSK, OpenSearch, S3, SQS
- Cognito (Google IdP federation requires `google_client_id` and `google_client_secret`)
- ALB Controller + ACM certificate
- `kubernetes_namespace` and `kubernetes_ingress_v1` for the app
- Secrets Manager secret + ESO Helm release + IRSA roles
- kube-prometheus-stack, Loki, Tempo
- ArgoCD + Application CRDs

### Step 4 — DNS validation for ACM

After apply, retrieve the CNAME records:

```bash
terraform output acm_dns_validation_records
```

Add them as CNAME records in Cloudflare for `patientsystem.me`. The certificate will validate within a few minutes.

### Step 5 — Point domain to ALB

Get the ALB DNS name (created by the ALB Controller once the Ingress is applied):

```bash
kubectl get ingress patient-system -n patient-system \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

In Cloudflare, add:
- `patientsystem.me` → CNAME → `<alb-hostname>` (proxied)
- `api.patientsystem.me` → CNAME → `<alb-hostname>` (proxied)
- `grafana.patientsystem.me` → CNAME → `<alb-hostname>` (proxied)
- `argocd.patientsystem.me` → CNAME → `<alb-hostname>` (proxied)

### Step 6 — Initialise databases

The RDS instance is created with a single `postgres` database. Create the per-service databases:

```bash
# From an EC2 bastion or kubectl port-forward:
psql -h <rds_address> -U admin_user -f init_databases.sh
```

### Step 7 — Push initial images

Trigger the `deploy.yml` workflow (or push a commit) to build and push the first Docker images to ECR. ArgoCD will sync the deployments once the image tags are written to `k8s/services/`.

---

## Tearing Down

**1. Empty all ECR repositories** — Terraform cannot delete a repository that still contains images.

```bash
for repo in api-gateway patient-service treatment-service billing-service \
            search-service analytics-service organization-service; do
  IMAGES=$(aws ecr list-images \
    --repository-name "patient-system/$repo" \
    --region eu-west-1 \
    --query 'imageIds[*]' --output json)
  [ "$IMAGES" != "[]" ] && aws ecr batch-delete-image \
    --repository-name "patient-system/$repo" \
    --image-ids "$IMAGES" \
    --region eu-west-1
done
```

**2. Empty the S3 bucket including all versions** — the bucket has versioning enabled, so a plain `aws s3 rm` only removes current objects. Terraform cannot delete a bucket that still has versions or delete markers.

```bash
BUCKET=patient-system-s3-storage-jjfd3rf

VERSIONS=$(aws s3api list-object-versions --bucket $BUCKET \
  --query '{Objects: Versions[].{Key:Key,VersionId:VersionId}}' --output json)
[ "$(echo $VERSIONS | python3 -c 'import json,sys; d=json.load(sys.stdin); print(len(d["Objects"] or []))')" -gt 0 ] && \
  aws s3api delete-objects --bucket $BUCKET --delete "$VERSIONS"

MARKERS=$(aws s3api list-object-versions --bucket $BUCKET \
  --query '{Objects: DeleteMarkers[].{Key:Key,VersionId:VersionId}}' --output json)
[ "$(echo $MARKERS | python3 -c 'import json,sys; d=json.load(sys.stdin); print(len(d["Objects"] or []))')" -gt 0 ] && \
  aws s3api delete-objects --bucket $BUCKET --delete "$MARKERS"
```

**3. Delete the Kubernetes Ingress** — this triggers AWS to remove the ALB. If the ALB still exists when `terraform destroy` runs, it holds a security group reference inside the VPC and every subsequent deletion fails with `DependencyViolation`.

```bash
kubectl delete ingress patient-system -n patient-system
```

Wait ~60 seconds for the ALB to be fully deprovisioned before continuing.

**3. Destroy all Terraform-managed resources**

```bash
cd terraform/environments/prod
terraform destroy
```

This removes everything in dependency order: Helm releases (ArgoCD, monitoring, ESO, ALB Controller), EKS cluster, RDS, MSK, OpenSearch, Cognito, S3, SQS, ECR repositories, IAM roles, and the VPC.

---

## Required Secrets and Variables

### 1. Terraform — `terraform/environments/prod/terraform.tfvars`

Create this file locally (it is gitignored). Never commit it.

```hcl
google_client_id             = ""   # Google OAuth 2.0 client ID
google_client_secret         = ""   # Google OAuth 2.0 client secret
db_password                  = ""   # Master password for RDS PostgreSQL
e2e_test_email               = ""   # Email of the Cognito E2E test user (created by Terraform)
e2e_test_password            = ""   # Password for the E2E test user
grafana_admin_password        = ""   # Grafana admin password
argocd_admin_password_bcrypt = ""   # bcrypt hash — generate with:
                                    # htpasswd -nbBC 10 '' PASSWORD | tr -d ':'
acm_certificate_arn          = ""   # Leave empty to create a new ACM cert; or paste an existing ARN
```

### 2. GitHub Actions secrets

Set these in the repository's **Settings → Secrets and variables → Actions → Secrets**.

| Secret | Description |
|---|---|
| `AWS_ACCOUNT_ID` | 12-digit AWS account ID — used to construct ECR image URLs |
| `E2E_TEST_EMAIL` | Same value as `e2e_test_email` in `terraform.tfvars` |
| `E2E_TEST_PASSWORD` | Same value as `e2e_test_password` in `terraform.tfvars` |

`GITHUB_TOKEN` is provided automatically by GitHub Actions — no action needed.

No `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` are required. The `ecr` Terraform module creates an IAM role that trusts GitHub's OIDC provider, so the deploy workflow authenticates via OIDC token.

### 3. Local development — `.env`

Copy `.env` to the project root and fill in values. This file is gitignored and must never be committed.

```bash
# LocalStack (if running AWS services locally)
LOCALSTACK_AUTH_TOKEN=

# AWS credentials for local dev (not needed in CI/prod — OIDC/IRSA handles that)
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=eu-west-1

# S3 and SQS (output by `terraform output` after provisioning)
AWS_S3_BUCKET=
AWS_SQS_IMPORT_QUEUE_URL=

# RDS master password (same as db_password in terraform.tfvars)
DB_PASSWORD=
```

### 4. Hardcoded Cognito pool ID — update after recreating Cognito

`api-gateway/src/main/resources/application.yml` has the Cognito User Pool ID baked into two values:

```yaml
jwk-set-uri: https://cognito-idp.eu-west-1.amazonaws.com/<POOL_ID>/.well-known/jwks.json
issuer-uri:  https://cognito-idp.eu-west-1.amazonaws.com/<POOL_ID>
```

Cognito generates a new pool ID every time the user pool is destroyed and recreated. After running `terraform apply`, get the new pool ID:

```bash
terraform output user_pool_id
```

Then update both occurrences in `application.yml` and push — the deploy pipeline will build and roll out the new api-gateway image automatically.

---

## Security Model

**Multi-tenancy isolation** — All data queries are scoped by `organizationId`. The `api-gateway` extracts the Cognito JWT claim `custom:organizationId` and injects it as `X-Organization-Id` into every downstream request. Services trust this header (only reachable inside the cluster) and never read the raw JWT.

**Zero-trust credentials** — No long-lived AWS credentials exist anywhere in the system. Services use IRSA (IAM Roles for Service Accounts via OIDC token projection). The GitHub Actions pipeline authenticates via GitHub OIDC. The only static credential is the RDS password, which lives exclusively in AWS Secrets Manager.

**Secrets distribution** — The External Secrets Operator syncs Secrets Manager into a Kubernetes Secret (`patient-system-secrets`). Pods consume it via `secretKeyRef`. No secrets are hardcoded in Kubernetes manifests or Docker images.

**Network isolation** — All data stores (RDS, MSK, OpenSearch) are in private subnets with security groups restricting ingress to the VPC CIDR. No data store has a public endpoint. EKS nodes are also in private subnets; only the ALB is public-facing.

**TLS everywhere** — External traffic terminates TLS at the ALB (ACM certificate, TLS 1.2+). Internal service-to-service traffic over gRPC uses plaintext within the cluster network; Kafka uses `PLAINTEXT` within the VPC.

**Supply chain** — Trivy scans both source dependencies (SCA in CI) and built Docker images (in the deploy pipeline) for HIGH and CRITICAL CVEs, blocking the build on any finding.

**Authentication** — Users authenticate via Cognito hosted UI with Google as the identity provider. JWTs are short-lived (1 hour) and validated on every request at the gateway using Cognito's JWKS endpoint.
