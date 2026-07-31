# Completed Phases

Captured here so PLAN.md stays lean. Each section below was fully implemented and CI-verified.

---

## Phase 1 — EKS Cluster ✅ COMPLETE

Terraform module `terraform/modules/eks/` using `terraform-aws-modules/eks/aws ~> 20.0`. Provisions:
- EKS cluster `prod-patient-system`, version 1.31
- Managed node group: t3.medium SPOT, min 1 / max 3 / desired 2, private subnets
- OIDC provider for IRSA
- Wired into `environments/prod/main.tf`

**Post-apply:** `aws eks update-kubeconfig --region eu-west-1 --name prod-patient-system`

---

## Phase 2 — ECR + CI/CD + Testing ✅ COMPLETE

### 2a — Housekeeping
- Deleted `integration-tests/` module (called localhost:4004 with non-existent /api/auth/login endpoint)
- Added `search-service` to root `pom.xml` (accidentally omitted, breaking root `mvn test`)
- Deleted all `bin/Dockerfile` files (IDE-generated, caused Trivy to double-scan services)
- Added `**/bin/` to `.gitignore`, removed all `bin/` directories from git tracking

### 2b — ECR Repositories
Terraform module `terraform/modules/ecr/`. One private ECR repo per service under `patient-system/SERVICE`. Lifecycle policy: keep last 5 images. Scan on push enabled.

### 2c — GitHub OIDC + IAM Role
`terraform/modules/ecr/github-oidc.tf`. Role `github-actions-deploy` trusts GitHub Actions OIDC for repo `stratidisgeorgios/Patient-Management-System`. Policies: ECR power user + EKS describe.

### 2d — Backend Unit Tests
JUnit 5 + Mockito in `src/test/java/` for all 7 services. All external deps mocked. Run via `mvn test`.

### 2e — Backend Integration Tests
Testcontainers `@SpringBootTest` tests. Each DB-owning service spins up real PostgreSQL/Kafka containers. `@DynamicPropertySource` overrides connection strings at test runtime.

### 2f — Frontend Unit Tests
Angular + Jasmine in existing `.spec.ts` files. Runs via `npx ng test --no-watch --browsers=ChromeHeadless`.

### 2g — Playwright E2E (deferred)
Setup is in `deploy.yml` (post-deploy step). Requires a live deployment. Will activate when Phase 4 is deployed.

### 2h — GitHub Actions Workflows
- `ci.yml`: runs on every push + PR → backend tests → frontend tests → SCA (Trivy)
- `deploy.yml`: runs on main → re-runs tests → detects changed services → builds/pushes to ECR → deploys to EKS → E2E

### Vulnerability Fixes Applied
- `grpc-netty-shaded` 1.69.0 → 1.75.0 (CVE-2025-55163, all 6 gRPC services)
- `postgresql` → 42.7.12 via `<postgresql.version>` BOM override (CVE-2026-54291, 5 services)
- Netty → 4.2.16.Final via `<netty.version>` BOM override (CVE-2026-59901+, api-gateway, org-service, search-service, patient-service)
- Spring Boot 4.0.6 → 4.0.7 on patient-service (fixed Tomcat/Spring/Jackson/spring-kafka CVEs)
- `@tailwindcss/vite` moved from `dependencies` → `devDependencies` (removed vite/postcss from prod audit)

### Test Compilation Fixes
- `AnalyticsServiceTest`: `List.of(new Object[]{...})` → `List.<Object[]>of(new Object[]{...})` (Java 25 type inference)
- `SearchServiceTest`: `throws Exception` → `throws IOException` (Mockito 5.x strict checked exception validation)
- `DatabaseIndexConfig.java`: constructor renamed from `TimescaleConfig` → `DatabaseIndexConfig` (class rename bug)

### GitHub Secrets Required
| Secret | Value |
|---|---|
| `AWS_ACCOUNT_ID` | `303357409766` |
| `E2E_TEST_EMAIL` | Dedicated Cognito test user email |
| `E2E_TEST_PASSWORD` | That user's password |

Cognito test user must be created manually in `eu-west-1_Sb4mcDano` and confirmed.
