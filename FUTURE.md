# Future TODO

- Remove `.mvn/`, `mvnw`, and `mvnw.cmd` from each service module and keep only the copies in the root folder. Update each service Dockerfile to copy the wrapper from the root instead of its own module folder.
- Add rate limiting to API gateway routes to prevent abuse. Spring Cloud Gateway has a built-in `RequestRateLimiter` filter that works with Redis.
- Add retry logic to API gateway routes for transient failures. Spring Cloud Gateway has a built-in `Retry` filter.
- Review path rewriting in the gateway — currently `/api-docs/patient` and `/api-docs/auth` rewrite to `/v3/api-docs`, which would conflict if both are hit simultaneously.
- Set up centralized Swagger UI in the API gateway once springdoc releases a version compatible with Spring Boot 4.x / Spring Framework 7 (`UriComponentsBuilder.fromHttpRequest` was removed).
- Add JWT validation to the api-docs routes (`/api-docs/patient`, `/api-docs/auth`) in the gateway. Currently open for convenience — requires configuring Swagger UI to pass the Bearer token when fetching specs.
- Add unit tests for each service — service layer logic, validators, JWT utility.
- Add integration tests hitting real databases and Kafka using Testcontainers.
- Add e2e tests covering full request flows through the API gateway (login → get token → access protected route). using REST-assured
