# Patient System — Cloud DevOps Transformation Plan

## Overview

Migrate the existing Docker Compose setup to a production-grade AWS cloud stack with Kubernetes, full observability, and GitOps. The infrastructure is designed to be spun up on demand for demos and torn down immediately after to minimize cost.

**Estimated spin-up time:** ~20-25 minutes (`terraform apply`)  
**Estimated cost per demo day:** ~$5-8 (EKS control plane + spot nodes + RDS + OpenSearch)  
**Tear-down:** `terraform destroy` (~15 minutes, everything gone)

---

## Current State

```
EC2 (Docker Compose)
├── nginx (reverse proxy, SSL)
├── api-gateway (Spring Boot, :4004)
├── patient-service
├── search-service
├── organization-service
├── billing-service
├── analytics-service
├── treatment-service
├── kafka
├── redis
└── postgres (local, dev only)

AWS Managed
├── RDS PostgreSQL
├── OpenSearch
├── S3
├── SQS
└── Cognito

Frontend
└── Angular → Cloudflare Pages
```

---

## Target State

```
GitHub (source of truth)
    ↓ push
GitHub Actions
    ↓ build + push
ECR (Docker images)
    ↓ ArgoCD syncs
EKS Cluster
├── Namespace: patient-system
│   ├── api-gateway (Deployment)
│   ├── patient-service (Deployment)
│   ├── search-service (Deployment)
│   ├── organization-service (Deployment)
│   ├── billing-service (Deployment)
│   ├── analytics-service (Deployment)
│   ├── treatment-service (Deployment)
│   ├── redis (StatefulSet)
│   └── kafka (Strimzi KafkaCluster CR)
├── Namespace: monitoring
│   ├── Prometheus
│   ├── Grafana
│   ├── Alertmanager
│   ├── Grafana Loki
│   ├── Grafana Tempo
│   └── OTel Collector (DaemonSet)
└── Namespace: argocd
    └── ArgoCD

AWS
├── ALB (entry point, replaces nginx)
│   └── AWS Load Balancer Controller (K8s)
├── ACM (SSL certificate)
├── ECR (image registry)
├── RDS PostgreSQL (unchanged)
├── OpenSearch (unchanged)
├── S3 (unchanged)
├── SQS (unchanged)
└── Cognito (unchanged)

Frontend
└── Angular → Cloudflare Pages (unchanged)
```

---

## Repository Structure (After Migration)

```
patient-system/
├── PLAN.md                          ← this file
├── docker-compose.yml               ← keep for local dev
├── .github/
│   └── workflows/
│       ├── deploy.yml               ← CI/CD: build → ECR → K8s
│       └── terraform-checks.yml     ← PR: terraform plan
├── terraform/
│   ├── environments/
│   │   └── prod/
│   │       ├── main.tf
│   │       ├── variables.tf
│   │       ├── outputs.tf
│   │       └── terraform.tfvars     ← gitignored, secrets
│   └── modules/
│       ├── vpc/                     ← existing
│       ├── rds/                     ← existing
│       ├── s3/                      ← existing
│       ├── opensearch/              ← existing
│       ├── cognito/                 ← existing
│       ├── eks/                     ← NEW Phase 1
│       ├── ecr/                     ← NEW Phase 2
│       ├── alb-controller/          ← NEW Phase 3
│       └── monitoring/              ← NEW Phase 6
├── k8s/
│   ├── namespaces.yaml
│   ├── services/
│   │   ├── api-gateway/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   └── configmap.yaml
│   │   ├── patient-service/
│   │   ├── search-service/
│   │   ├── organization-service/
│   │   ├── billing-service/
│   │   ├── analytics-service/
│   │   ├── treatment-service/
│   │   └── redis/
│   ├── kafka/
│   │   ├── strimzi-operator.yaml
│   │   └── kafka-cluster.yaml
│   ├── ingress/
│   │   └── ingress.yaml             ← ALB Ingress resource
│   ├── otel/
│   │   └── collector.yaml           ← OTel Collector DaemonSet
│   └── argocd/
│       └── applications/            ← ArgoCD Application CRDs
└── [service folders unchanged]
    ├── patient-service/
    ├── api-gateway/
    └── ...
```

---

## Phase 1 — EKS Cluster

**Goal:** Working K8s cluster in AWS that kubectl can reach.  
**Prerequisite:** Existing VPC and subnets from current Terraform.

### Terraform Module: `terraform/modules/eks/`

**Files:**
- `main.tf` — cluster, node group, OIDC provider
- `variables.tf`
- `outputs.tf` — cluster name, endpoint, certificate, OIDC ARN

**Key resources:**

```hcl
# Use the official community module (battle-tested)
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = "${var.environment}-patient-system"
  cluster_version = "1.31"

  vpc_id     = var.vpc_id
  subnet_ids = var.private_subnet_ids  # nodes go in private subnets

  # Allow kubectl from within VPC (EC2, CI/CD runners)
  cluster_endpoint_public_access = true

  eks_managed_node_groups = {
    main = {
      instance_types = ["t3.medium"]
      capacity_type  = "SPOT"           # ~70% cost saving
      min_size       = 1
      max_size       = 3
      desired_size   = 2

      labels = {
        Environment = var.environment
      }
    }
  }

  # Enable IRSA (IAM Roles for Service Accounts)
  enable_irsa = true
}
```

**OIDC Provider** (required for IRSA — pods assume IAM roles without long-lived keys):
```hcl
resource "aws_iam_openid_connect_provider" "eks" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks.certificates[0].sha1_fingerprint]
  url             = module.eks.cluster_oidc_issuer_url
}
```

**Outputs needed by other modules:**
```hcl
output "cluster_name"            { value = module.eks.cluster_name }
output "cluster_endpoint"        { value = module.eks.cluster_endpoint }
output "cluster_certificate"     { value = module.eks.cluster_certificate_authority_data }
output "oidc_provider_arn"       { value = aws_iam_openid_connect_provider.eks.arn }
output "oidc_provider_url"       { value = module.eks.cluster_oidc_issuer_url }
```

### Wire into `environments/prod/main.tf`

```hcl
module "eks" {
  source = "../../modules/eks"

  environment        = var.environment
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
}
```

### Post-apply: configure kubectl

```bash
aws eks update-kubeconfig \
  --region eu-west-1 \
  --name prod-patient-system
```

### Verification

```bash
kubectl get nodes          # should show 2 nodes in Ready state
kubectl get namespaces
```

---

## Phase 2 — ECR + CI/CD + Testing

**Goal:** Every push to a feature branch runs the full test suite. Every merge to main additionally builds Docker images, pushes them to ECR, deploys to EKS, and runs Playwright E2E tests against the live deployment.

**Branch strategy:** Feature branches → PR → merge to main. CI runs on all branches. Deploy runs only on main.

---

### Phase 2 Housekeeping (do first)

Before building anything new, three cleanup tasks:

1. **Delete `integration-tests/`** — existing tests call `localhost:4004` with hardcoded credentials and a `/api/auth/login` endpoint that does not exist (app uses Cognito). They cannot be made to work and are replaced by proper Testcontainers integration tests.

2. **Add `search-service` to root `pom.xml`** — it was accidentally omitted. Without it, `mvn test` at the root skips search-service entirely.

3. **Delete all `bin/Dockerfile` files** — these are IDE-generated artifacts (IntelliJ dev mode). CI must use only the root-level `Dockerfile` in each service directory.

---

### 2a — ECR Repositories

**Terraform module:** `terraform/modules/ecr/`

One private ECR repository per service. Lifecycle policy keeps the last 5 images to avoid unbounded storage growth. Image scanning on push is free and catches known CVEs at build time.

```hcl
locals {
  services = [
    "api-gateway", "patient-service", "search-service",
    "organization-service", "billing-service",
    "analytics-service", "treatment-service",
  ]
}

resource "aws_ecr_repository" "services" {
  for_each             = toset(local.services)
  name                 = "patient-system/${each.value}"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "services" {
  for_each   = aws_ecr_repository.services
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 5 images"
      selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 5 }
      action       = { type = "expire" }
    }]
  })
}
```

---

### 2b — GitHub OIDC + IAM Role

**File:** `terraform/modules/ecr/github-oidc.tf`

GitHub Actions authenticates to AWS using OIDC (no stored AWS keys anywhere). AWS issues a temporary session token scoped to the exact repo (`stratidisgeorgios/Patient-Management-System`). The IAM role gets ECR push access and EKS deploy access (needed from Phase 4 onwards).

```hcl
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

resource "aws_iam_role" "github_actions" {
  name = "github-actions-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringLike = {
          "token.actions.githubusercontent.com:sub" = "repo:stratidisgeorgios/Patient-Management-System:*"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "github_ecr" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser"
}

# EKS deploy permission — used from Phase 4 onwards
resource "aws_iam_role_policy" "github_eks" {
  name = "eks-deploy"
  role = aws_iam_role.github_actions.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["eks:DescribeCluster"]
      Resource = "*"
    }]
  })
}
```

---

### 2c — Backend Unit Tests (JUnit 5 + Mockito)

**Location:** `src/test/java/` inside each service  
**Runs in:** CI on every push (no Docker required, very fast)

One test class per service layer class. All external dependencies (repositories, Kafka, gRPC clients, AWS SDK) are mocked with Mockito. Tests verify business logic only.

| Service | What to test |
|---|---|
| `patient-service` | PatientService: create validates fields, publishes Kafka event; getById throws on missing; import triggers SQS message |
| `analytics-service` | AnalyticsService: all query methods return correct shape; null-safe when no data |
| `billing-service` | BillingService: invoice creation sets correct amounts; retrieval by patient/org |
| `treatment-service` | TreatmentService: CRUD, category validation |
| `organization-service` | OrganizationService: create, get, duplicate name rejected |
| `search-service` | SearchService: PatientCreated routes to index; PatientDeleted routes to delete; bulk index calls correct method |
| `api-gateway` | Auth filter: valid JWT passes; expired JWT rejected; missing header rejected |

---

### 2d — Backend Integration Tests (Testcontainers)

**Location:** `src/test/java/` inside each service (separate test class, e.g. `PatientServiceIntegrationTest`)  
**Runs in:** CI only (GitHub Actions runners have Docker)  
**Replaces:** the deleted `integration-tests/` module

Each service that owns a database gets a `@SpringBootTest` test that spins up real infrastructure via Testcontainers and exercises the full stack (controller → service → repository → database).

| Service | Containers needed |
|---|---|
| `patient-service` | PostgreSQL + Kafka |
| `analytics-service` | PostgreSQL |
| `billing-service` | PostgreSQL |
| `treatment-service` | PostgreSQL |
| `organization-service` | PostgreSQL |
| `search-service` | OpenSearch |

**Example pattern (patient-service):**
```java
@SpringBootTest
@Testcontainers
class PatientServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16").withDatabaseName("patient_db");

    @Container
    static KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void shouldCreatePatientAndPublishEvent() { ... }
}
```

**Dependencies to add to each service `pom.xml`:**
```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<!-- add kafka / opensearch variants as needed per service -->
```

---

### 2e — Frontend Unit Tests (Angular + Jasmine)

**Location:** existing `.spec.ts` files (all scaffolded, all need real content)  
**Runs in:** CI with headless Chrome (`--browsers=ChromeHeadless`)

The existing spec files are all `toBeTruthy()` stubs. Each needs real test content:

- **Services** (`PatientService`, `AnalyticsService`, `BillingService`, etc.) — use `HttpClientTestingModule` to intercept HTTP calls, verify correct URL and method, return mock responses, assert signal/observable output
- **Components** (`PatientList`, `PatientProfile`, etc.) — verify correct rendering, form validation errors appear, button clicks trigger correct service calls
- **Pages** (`Analytics`, `Patients`, etc.) — verify loading state, data display after observable resolves

---

### 2f — E2E Tests (Playwright)

**Setup:** `ng add @playwright/test` inside `patient-frontend/`  
**Location:** `patient-frontend/e2e/`  
**Runs in:** CI after deployment (post-deploy step in `deploy.yml`)

Playwright runs against the live deployed app at `https://patientsystem.me`. Requires a dedicated Cognito test user (stored as GitHub secrets — see below).

**Test scenarios:**
1. Login via Cognito hosted UI → redirects back to app
2. Search for a patient by name → results appear
3. Create a new patient → success notification
4. Navigate to Analytics page → stat cards render with numbers
5. View a treatment → profile page loads

**Cognito auth in CI:** Playwright fills the Cognito hosted UI login form using credentials from `E2E_TEST_EMAIL` and `E2E_TEST_PASSWORD` GitHub secrets. A dedicated test user must be created in the Cognito user pool.

---

### 2g — GitHub Actions Workflows

#### `.github/workflows/ci.yml` — runs on every push and PR

```yaml
name: CI

on:
  push:
    branches: ['**']
  pull_request:
    branches: [main]

jobs:
  test-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: maven
      - name: Run all backend tests
        run: mvn test --no-transfer-progress
      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: backend-test-reports
          path: '**/target/surefire-reports/'

  test-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: patient-frontend/package-lock.json
      - name: Install dependencies
        run: npm ci
        working-directory: patient-frontend
      - name: Run Angular unit tests
        run: npx ng test --no-watch --browsers=ChromeHeadless
        working-directory: patient-frontend
```

#### `.github/workflows/deploy.yml` — runs on push to main only

```yaml
name: Deploy

on:
  push:
    branches: [main]

permissions:
  id-token: write
  contents: write   # needed to commit updated image tags (Phase 7 ArgoCD)

jobs:
  # Re-run tests as a hard gate — nothing deploys if tests fail
  test-backend:
    uses: ./.github/workflows/ci.yml  # reuse ci workflow jobs

  test-frontend:
    uses: ./.github/workflows/ci.yml

  detect-changes:
    needs: [test-backend, test-frontend]
    runs-on: ubuntu-latest
    outputs:
      services: ${{ steps.changes.outputs.services }}
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 2
      - name: Detect changed services
        id: changes
        run: |
          CHANGED=$(git diff --name-only HEAD~1 HEAD \
            | grep -oP '^(api-gateway|patient-service|search-service|organization-service|billing-service|analytics-service|treatment-service)' \
            | sort -u | jq -Rsc 'split("\n")[:-1]')
          echo "services=$CHANGED" >> $GITHUB_OUTPUT

  build-push:
    needs: detect-changes
    if: needs.detect-changes.outputs.services != '[]'
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: ${{ fromJson(needs.detect-changes.outputs.services) }}
    steps:
      - uses: actions/checkout@v4
      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/github-actions-deploy
          aws-region: eu-west-1
      - name: Login to ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
      - name: Build and push
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build \
            -t $ECR_REGISTRY/patient-system/${{ matrix.service }}:$IMAGE_TAG \
            -t $ECR_REGISTRY/patient-system/${{ matrix.service }}:latest \
            -f ${{ matrix.service }}/Dockerfile \
            ${{ matrix.service }}/
          docker push $ECR_REGISTRY/patient-system/${{ matrix.service }}:$IMAGE_TAG
          docker push $ECR_REGISTRY/patient-system/${{ matrix.service }}:latest

  # Placeholder — filled in Phase 4 when K8s manifests exist
  deploy:
    needs: build-push
    runs-on: ubuntu-latest
    steps:
      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::${{ secrets.AWS_ACCOUNT_ID }}:role/github-actions-deploy
          aws-region: eu-west-1
      - name: Configure kubectl
        run: aws eks update-kubeconfig --region eu-west-1 --name prod-patient-system
      # kubectl set image commands added in Phase 4

  e2e:
    needs: deploy
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: patient-frontend/package-lock.json
      - name: Install dependencies
        run: npm ci
        working-directory: patient-frontend
      - name: Install Playwright browsers
        run: npx playwright install --with-deps chromium
        working-directory: patient-frontend
      - name: Run E2E tests
        env:
          E2E_BASE_URL: https://patientsystem.me
          E2E_TEST_EMAIL: ${{ secrets.E2E_TEST_EMAIL }}
          E2E_TEST_PASSWORD: ${{ secrets.E2E_TEST_PASSWORD }}
        run: npx playwright test
        working-directory: patient-frontend
      - name: Upload Playwright report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: patient-frontend/playwright-report/
```

---

### GitHub Secrets Required

| Secret | Value |
|---|---|
| `AWS_ACCOUNT_ID` | `303357409766` |
| `E2E_TEST_EMAIL` | Email of a dedicated Cognito test user |
| `E2E_TEST_PASSWORD` | Password of that test user |

The Cognito test user must be created manually in the `eu-west-1_Sb4mcDano` user pool and confirmed. It should be a real user (not temporary) that persists across tear-down/spin-up cycles.

---

## Phase 3 — ALB Ingress (replaces nginx)

**Goal:** ALB handles all external traffic, SSL termination, path-based routing to K8s services.

### Terraform Module: `terraform/modules/alb-controller/`

**AWS Load Balancer Controller** (K8s controller that provisions ALBs from Ingress resources):

```hcl
# IRSA role for the controller
module "lb_controller_irsa" {
  source = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"

  role_name                              = "aws-load-balancer-controller"
  attach_load_balancer_controller_policy = true

  oidc_providers = {
    main = {
      provider_arn               = var.oidc_provider_arn
      namespace_service_accounts = ["kube-system:aws-load-balancer-controller"]
    }
  }
}

# Install via Helm
resource "helm_release" "aws_lb_controller" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  version    = "1.7.1"
  namespace  = "kube-system"

  set {
    name  = "clusterName"
    value = var.cluster_name
  }
  set {
    name  = "serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = module.lb_controller_irsa.iam_role_arn
  }
  set {
    name  = "region"
    value = "eu-west-1"
  }
  set {
    name  = "vpcId"
    value = var.vpc_id
  }
}
```

### K8s Ingress Resource: `k8s/ingress/ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: patient-system
  namespace: patient-system
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/certificate-arn: ARN_FROM_ACM
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP":80},{"HTTPS":443}]'
    alb.ingress.kubernetes.io/ssl-redirect: "443"
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health
spec:
  rules:
    - host: patientsystem.me
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: api-gateway
                port:
                  number: 4004
```

**DNS update:** Change Cloudflare CNAME from EC2 IP → ALB DNS name output from Terraform.

---

## Phase 4 — Core Services on Kubernetes

**Goal:** All Spring Boot services, Kafka, and Redis running as K8s workloads.

### Namespaces: `k8s/namespaces.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: patient-system
---
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring
---
apiVersion: v1
kind: Namespace
metadata:
  name: kafka
```

### Service Template (example: `k8s/services/patient-service/deployment.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: patient-service
  namespace: patient-system
spec:
  replicas: 2
  selector:
    matchLabels:
      app: patient-service
  template:
    metadata:
      labels:
        app: patient-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      serviceAccountName: patient-service   # for IRSA (S3, SQS access)
      containers:
        - name: patient-service
          image: ACCOUNT.dkr.ecr.eu-west-1.amazonaws.com/patient-system/patient-service:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: RDS_ADDRESS
              valueFrom:
                secretKeyRef:
                  name: patient-system-secrets
                  key: rds-address
            # ... other env vars from secrets
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
```

### Secrets Management

Use **External Secrets Operator** + AWS Secrets Manager (no `.env` files in K8s):

```hcl
# Terraform: store secrets in AWS Secrets Manager
resource "aws_secretsmanager_secret" "patient_system" {
  name = "patient-system/prod"
}

resource "aws_secretsmanager_secret_version" "patient_system" {
  secret_id = aws_secretsmanager_secret.patient_system.id
  secret_string = jsonencode({
    rds-address             = module.rds.endpoint
    kafka-bootstrap-servers = "kafka.kafka.svc.cluster.local:9092"
    opensearch-endpoint     = module.opensearch.endpoint
    # etc.
  })
}
```

```yaml
# K8s: ExternalSecret syncs from AWS Secrets Manager
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: patient-system-secrets
  namespace: patient-system
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: ClusterSecretStore
  target:
    name: patient-system-secrets
  data:
    - secretKey: rds-address
      remoteRef:
        key: patient-system/prod
        property: rds-address
```

### Kafka via Strimzi: `k8s/kafka/`

```yaml
# strimzi-operator: installed via Helm
# kafka-cluster.yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: patient-system-kafka
  namespace: kafka
spec:
  kafka:
    version: 3.7.0
    replicas: 1          # 1 for demo, 3 for HA
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
    storage:
      type: ephemeral    # ephemeral for demo (no PVC cost)
    config:
      offsets.topic.replication.factor: 1
      transaction.state.log.replication.factor: 1
      transaction.state.log.min.isr: 1
  zookeeper:
    replicas: 1
    storage:
      type: ephemeral
  entityOperator:
    topicOperator: {}
    userOperator: {}
```

Internal DNS for services to reach Kafka:
```
kafka-bootstrap.kafka.svc.cluster.local:9092
```

### Redis: `k8s/services/redis/`

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
  namespace: patient-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:latest
          ports:
            - containerPort: 6379
          resources:
            requests:
              memory: "128Mi"
              cpu: "100m"
            limits:
              memory: "256Mi"
```

### IRSA per service (pods access AWS without keys)

Each service that needs AWS access (S3, SQS, etc.) gets its own K8s ServiceAccount annotated with an IAM role:

```hcl
module "patient_service_irsa" {
  source = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"

  role_name = "patient-service"
  oidc_providers = {
    main = {
      provider_arn               = var.oidc_provider_arn
      namespace_service_accounts = ["patient-system:patient-service"]
    }
  }

  # Attach only the permissions this service needs
  role_policy_arns = {
    s3  = aws_iam_policy.patient_service_s3.arn
    sqs = aws_iam_policy.patient_service_sqs.arn
  }
}
```

---

## Phase 5 — OpenTelemetry Instrumentation

**Goal:** Distributed traces and metrics from all Spring Boot services with zero code changes.

### OTel Collector: `k8s/otel/collector.yaml`

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: otel-collector
  namespace: monitoring
spec:
  selector:
    matchLabels:
      app: otel-collector
  template:
    metadata:
      labels:
        app: otel-collector
    spec:
      containers:
        - name: otel-collector
          image: otel/opentelemetry-collector-contrib:0.96.0
          args: ["--config=/conf/config.yaml"]
          volumeMounts:
            - name: config
              mountPath: /conf
          ports:
            - containerPort: 4317   # OTLP gRPC
            - containerPort: 4318   # OTLP HTTP
            - containerPort: 8889   # Prometheus metrics
      volumes:
        - name: config
          configMap:
            name: otel-collector-config
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: otel-collector-config
  namespace: monitoring
data:
  config.yaml: |
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:4317
          http:
            endpoint: 0.0.0.0:4318

    processors:
      batch:
        timeout: 1s
      memory_limiter:
        check_interval: 1s
        limit_mib: 400

    exporters:
      prometheus:
        endpoint: "0.0.0.0:8889"
      otlp/tempo:
        endpoint: "tempo.monitoring.svc.cluster.local:4317"
        tls:
          insecure: true
      loki:
        endpoint: "http://loki.monitoring.svc.cluster.local:3100/loki/api/v1/push"

    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: [memory_limiter, batch]
          exporters: [otlp/tempo]
        metrics:
          receivers: [otlp]
          processors: [memory_limiter, batch]
          exporters: [prometheus]
```

### Java Auto-instrumentation

Inject the OTel Java agent via `JAVA_TOOL_OPTIONS` env var on each pod (no code changes):

```yaml
# Add to every Spring Boot deployment
env:
  - name: JAVA_TOOL_OPTIONS
    value: "-javaagent:/otel/opentelemetry-javaagent.jar"
  - name: OTEL_SERVICE_NAME
    value: "patient-service"
  - name: OTEL_EXPORTER_OTLP_ENDPOINT
    value: "http://$(HOST_IP):4317"
  - name: OTEL_METRICS_EXPORTER
    value: "otlp"
  - name: OTEL_TRACES_EXPORTER
    value: "otlp"
  - name: HOST_IP
    valueFrom:
      fieldRef:
        fieldPath: status.hostIP   # routes to DaemonSet on same node
```

Mount the agent via init container:
```yaml
initContainers:
  - name: otel-agent
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-java:1.32.0
    command: ["cp", "/javaagent.jar", "/otel/opentelemetry-javaagent.jar"]
    volumeMounts:
      - name: otel-agent
        mountPath: /otel
volumes:
  - name: otel-agent
    emptyDir: {}
```

---

## Phase 6 — Prometheus + Grafana + Loki + Tempo

**Goal:** Full observability stack — metrics, logs, traces — all in Grafana.

### Terraform Module: `terraform/modules/monitoring/`

```hcl
# kube-prometheus-stack: Prometheus + Grafana + Alertmanager + node exporters
resource "helm_release" "kube_prometheus_stack" {
  name       = "kube-prometheus-stack"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  version    = "57.0.0"
  namespace  = "monitoring"

  values = [file("${path.module}/values/prometheus-stack.yaml")]
}

# Grafana Loki (log aggregation)
resource "helm_release" "loki" {
  name       = "loki"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "loki-stack"
  version    = "2.10.0"
  namespace  = "monitoring"

  set {
    name  = "promtail.enabled"
    value = "true"    # DaemonSet that collects pod logs
  }
  set {
    name  = "grafana.enabled"
    value = "false"   # already have Grafana from kube-prometheus-stack
  }
}

# Grafana Tempo (distributed traces)
resource "helm_release" "tempo" {
  name       = "tempo"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "tempo"
  version    = "1.7.0"
  namespace  = "monitoring"
}
```

### Grafana Configuration (`values/prometheus-stack.yaml`)

```yaml
grafana:
  adminPassword: "change-me-in-tfvars"
  
  additionalDataSources:
    - name: Loki
      type: loki
      url: http://loki:3100
      access: proxy
    - name: Tempo
      type: tempo
      url: http://tempo:3100
      access: proxy
      jsonData:
        tracesToLogs:
          datasourceUid: loki
          filterByTraceID: true

  dashboardProviders:
    dashboardproviders.yaml:
      apiVersion: 1
      providers:
        - name: patient-system
          folder: Patient System
          type: file
          options:
            path: /var/lib/grafana/dashboards/patient-system

prometheus:
  prometheusSpec:
    # Scrape pods with prometheus annotations
    podMonitorSelectorNilUsesHelmValues: false
    serviceMonitorSelectorNilUsesHelmValues: false
    additionalScrapeConfigs:
      - job_name: spring-boot-services
        kubernetes_sd_configs:
          - role: pod
            namespaces:
              names: [patient-system]
        relabel_configs:
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
            action: keep
            regex: "true"
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
            action: replace
            target_label: __metrics_path__
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_port]
            action: replace
            target_label: __address__
            regex: (.+)
            replacement: $1
```

### Spring Boot Services — Required Changes

Add to every service's `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Add to every service's `application.properties`:

```properties
management.endpoints.web.exposure.include=health,prometheus,info
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
management.metrics.export.prometheus.enabled=true
```

### Grafana Dashboards to Build

| Dashboard | Metrics |
|---|---|
| **JVM Overview** | Heap used/max, GC pause time, thread count, class loading |
| **HTTP Traffic** | Request rate, P50/P95/P99 latency, error rate (per service) |
| **Import Jobs** | Jobs PENDING/PROCESSING/COMPLETED/FAILED, rows/sec |
| **K8s Cluster** | Node CPU/mem, pod restarts, pending pods |
| **Kafka** | Consumer lag per topic, messages/sec |
| **Business** | Patients created/hour, search queries/sec |

---

## Phase 7 — ArgoCD (GitOps)

**Goal:** Git is the single source of truth. Push to main → ArgoCD detects drift → deploys.

### Install ArgoCD

```hcl
resource "helm_release" "argocd" {
  name       = "argocd"
  repository = "https://argoproj.github.io/argo-helm"
  chart      = "argo-cd"
  version    = "6.7.0"
  namespace  = "argocd"

  set {
    name  = "server.ingress.enabled"
    value = "true"
  }
  set {
    name  = "server.ingress.annotations.kubernetes\\.io/ingress\\.class"
    value = "alb"
  }
}
```

### Application CRDs: `k8s/argocd/applications/`

```yaml
# patient-system-app.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: patient-system
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/stratidisgeorgios/Patient-Management-System
    targetRevision: main
    path: k8s/services
  destination:
    server: https://kubernetes.default.svc
    namespace: patient-system
  syncPolicy:
    automated:
      prune: true      # delete K8s resources removed from git
      selfHeal: true   # revert manual kubectl changes
    syncOptions:
      - CreateNamespace=true
```

### Updated CI/CD Flow with ArgoCD

```
push to main
    ↓
GitHub Actions builds image → pushes to ECR
    ↓
GitHub Actions updates image tag in k8s/services/SERVICE/deployment.yaml
    ↓
git commit + push (the image tag change)
    ↓
ArgoCD detects git change → applies new deployment.yaml
    ↓
K8s rolling update
```

---

## Spin-Up Runbook (Demo Day)

```bash
# 1. Apply all infrastructure (~20 min)
cd terraform/environments/prod
terraform init
terraform apply -auto-approve

# 2. Configure kubectl
aws eks update-kubeconfig --region eu-west-1 --name prod-patient-system

# 3. Apply K8s base resources
kubectl apply -f k8s/namespaces.yaml
kubectl apply -f k8s/kafka/

# 4. Wait for Kafka to be ready
kubectl wait kafka/patient-system-kafka \
  --for=condition=Ready --timeout=300s -n kafka

# 5. GitHub Actions deploys services automatically on push
# OR manually:
kubectl apply -f k8s/services/
kubectl apply -f k8s/ingress/
kubectl apply -f k8s/otel/

# 6. Update Cloudflare DNS to new ALB endpoint
terraform output alb_dns_name   # copy this
# Update Cloudflare CNAME manually or via Cloudflare Terraform provider

# 7. Verify
kubectl get pods -n patient-system
kubectl get pods -n monitoring
kubectl get ingress -n patient-system
```

## Tear-Down Runbook

```bash
# Delete K8s resources first (avoids ALB/ELB orphan resources)
kubectl delete -f k8s/ --recursive

# Wait for ALBs to deregister
sleep 60

# Destroy all AWS infrastructure
cd terraform/environments/prod
terraform destroy -auto-approve
```

---

## Cost Breakdown (Per Demo Day)

| Resource | Cost/hour | Cost/8hr demo day |
|---|---|---|
| EKS control plane | $0.10 | $0.80 |
| 2x t3.medium SPOT nodes | ~$0.02 | ~$0.16 |
| RDS db.t3.micro | ~$0.02 | ~$0.16 |
| OpenSearch t3.small | ~$0.04 | ~$0.32 |
| ALB | ~$0.008 | ~$0.06 |
| NAT Gateway | ~$0.045 | ~$0.36 |
| **Total** | | **~$1.86/day** |

---

## Implementation Order

1. **Phase 1** — EKS cluster + kubectl access ✅ COMPLETE
2. **Phase 2** — ECR repos + CI/CD + Testing (current phase)
   - 2a. Housekeeping (delete integration-tests, fix pom.xml)
   - 2b. ECR Terraform module
   - 2c. GitHub OIDC + IAM role
   - 2d. Backend unit tests (JUnit 5 + Mockito)
   - 2e. Backend integration tests (Testcontainers)
   - 2f. Frontend unit tests (Angular + Jasmine)
   - 2g. Playwright E2E setup
   - 2h. GitHub Actions workflows (ci.yml + deploy.yml)
3. **Phase 4** — All services on K8s (core functionality working)
4. **Phase 3** — ALB Ingress (replace nginx, public traffic working)
5. **Phase 5** — OTel instrumentation
6. **Phase 6** — Prometheus + Grafana + Loki + Tempo
7. **Phase 7** — ArgoCD (GitOps polish)

Each phase is independently demoable. Stop at any phase if time-constrained.

---

## Open Questions / Decisions

- [x] GitHub repo for OIDC trust: `stratidisgeorgios/Patient-Management-System`
- [x] Testing strategy: JUnit 5 + Mockito (unit), Testcontainers (integration), Angular Testing Module (frontend), Playwright (E2E)
- [x] All tests run in GitHub Actions CI — not locally
- [x] Branch strategy: feature branches → PR → merge to main
- [x] `integration-tests/` module: delete entirely (broken, calls localhost, unsalvageable)
- [ ] Grafana admin password (store in tfvars, gitignored)
- [ ] Whether to keep EC2 instance running for SSH access or decommission it after Phase 4
- [ ] Whether to add Cloudflare Terraform provider for automated DNS updates
- [ ] Create dedicated Cognito test user for E2E tests (`E2E_TEST_EMAIL` / `E2E_TEST_PASSWORD`)
