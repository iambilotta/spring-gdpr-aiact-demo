# Security policy

This repository is a demonstration. It is not deployed anywhere, holds no real personal data, and is not a security boundary on its own. Still, it imports two libraries that are deployed in production by adopters, so security findings that surface here are valuable upstream.

## Where to report

- **Vulnerability in `spring-aiact`** (audit log integrity, HMAC handling, NDJSON parsing, REST endpoint authorisation): report privately to the upstream maintainers. See [`spring-aiact/SECURITY.md`](https://github.com/iambilotta/spring-aiact/blob/main/SECURITY.md).
- **Vulnerability in `spring-gdpr`** (audit table SQL, erasure flow ordering, REST endpoint authorisation): report privately to the upstream maintainers. See [`spring-gdpr/SECURITY.md`](https://github.com/iambilotta/spring-gdpr/blob/main/SECURITY.md).
- **Demo-only issue** (this repo's `pom.xml`, `application.yaml`, controllers, integration tests): email `francesco@iambilotta.com` with the subject line `[spring-gdpr-aiact-demo security]`.

Do not open a public issue.

## What we consider in scope

- Misconfiguration that produces a misleading result for a demo reader (for example, a controller that returns successful-looking output despite a library failure that should have been visible).
- Dependency CVEs that affect the demo build or the shipped libraries.
- Hard-coded credentials or secrets accidentally committed to this repo.

## What is out of scope

- Deliberate insecure defaults of the demo profile (the default HMAC secret is allowed in `dev`, the `/aiact/**` and `/gdpr/**` endpoints are open without auth). These are documented and the README explicitly calls them out as not production-ready.
- Findings against unrelated dependencies (Spring Boot itself, JDK, H2). Report those upstream.

## Disclosure window

We aim to acknowledge a private report within 5 working days, and to ship a fix or a documented mitigation within 90 days.
