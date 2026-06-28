# Future TODO

- Recreate the Java CDK infrastructure from the tutorial in Terraform, following the module/environment structure from the team-2 project. User will provide the tutorial's Java CDK resources and they will be translated to Terraform.

- Deployment plan: Spin up an Oracle Cloud free tier A1 VM (4 OCPUs, 24GB RAM, always free) and run LocalStack Pro on it via Docker. Write Terraform targeting LocalStack to deploy all services (ECS, RDS, MSK, ElastiCache) — no AWS costs since LocalStack Pro subscription is already paid. Services are accessible via the VM's public IP. SSH into the VM to manage/deploy. Angular frontend (hosted on AWS S3 + CloudFront free tier) points to the Oracle VM's public IP as the API base URL.

- Auth plan: Use real AWS Cognito (free under 50k MAUs) instead of Keycloak — supports Google OAuth natively. Everything else stays on LocalStack. The API gateway just swaps `jwk-set-uri` to point at the real Cognito JWKS endpoint. No other real AWS services needed = no AWS bill.

- Remove `.mvn/`, `mvnw`, and `mvnw.cmd` from each service module and keep only the copies in the root folder. Update each service Dockerfile to copy the wrapper from the root instead of its own module folder.
- Add rate limiting to API gateway routes to prevent abuse. Spring Cloud Gateway has a built-in `RequestRateLimiter` filter that works with Redis.
- Add retry logic to API gateway routes for transient failures. Spring Cloud Gateway has a built-in `Retry` filter.
- Review path rewriting in the gateway — currently `/api-docs/patient` and `/api-docs/auth` rewrite to `/v3/api-docs`, which would conflict if both are hit simultaneously.
- Set up centralized Swagger UI in the API gateway once springdoc releases a version compatible with Spring Boot 4.x / Spring Framework 7 (`UriComponentsBuilder.fromHttpRequest` was removed).
- Add JWT validation to the api-docs routes (`/api-docs/patient`, `/api-docs/auth`) in the gateway. Currently open for convenience — requires configuring Swagger UI to pass the Bearer token when fetching specs.
- Add unit tests for each service — service layer logic, validators, JWT utility.
- Add integration tests hitting real databases and Kafka using Testcontainers.
- Add e2e tests covering full request flows through the API gateway (login → get token → access protected route). using REST-assured
