# Contributing to spring-gdpr-aiact-demo

This repo is a demonstration of the [spring-aiact](https://github.com/iambilotta/spring-aiact) and [spring-gdpr](https://github.com/iambilotta/spring-gdpr) libraries running together. Contributions that make the demo clearer or fix breakage on a current Spring Boot release are welcome. Library-level contributions (new annotations, new endpoints, behaviour changes) belong on the upstream repos, not here.

## What lives here, what does not

| Belongs here | Belongs upstream |
|---|---|
| Showing additional integration angles between the two libraries | New annotation, new endpoint, new SPI |
| Fixing a Spring Boot version bump that broke the demo | Bug in audit-log shape, HMAC chain, ROPA generator |
| README clarifications, doc typos | API docs, library-level FAQ |
| New integration tests that pin observable library output | Unit tests of library internals |

If unsure, open an issue here and we will route the discussion to the right place.

## Local setup

You need:

- JDK 21+
- Maven 3.9+
- Internet access on first build (to fetch the libraries from JitPack)

```bash
git clone git@github.com:iambilotta/spring-gdpr-aiact-demo.git
cd spring-gdpr-aiact-demo
mvn -B verify
```

The first build pulls `com.github.iambilotta.spring-aiact:*:v1.0.0` and `com.github.iambilotta.spring-gdpr:*:v1.0.0` from JitPack. JitPack lazily builds the artifacts on the first request, which can take a couple of minutes the first time only.

## Coding conventions

- Java 21 features welcome. Keep code at minimum Spring Boot 3.5 compatibility.
- Domain code lives under `com.iambilotta.demo`. One package on purpose: the whole point is that the demo fits in a glance.
- Tests under `src/test/java`, same package layout. Prefer `@SpringBootTest` over slice tests for the demo: we want to assert that **the libraries work** in a real-ish boot, not that the controller routes are wired.
- New tests must assert visible behaviour produced by the libraries: a row in the GDPR audit table, a record in the NDJSON file, the response body of `/aiact/log/verify`, the affected-by-type map of the erasure flow. If a test passes without exercising a library output, it is the wrong test.

## Submitting a change

1. Open an issue first for any non-trivial change so we can agree on the shape.
2. Branch off `main`. Use a descriptive branch name (`fix/`, `docs/`, `test/`).
3. Run `mvn verify` locally before pushing. CI will reject anything that fails.
4. PRs require at least one passing CI run on Java 21 and Java 25.
5. Sign-off your commits (`git commit -s`). DCO is required.

## Reporting bugs

- If the bug is in this demo (Maven build, application boot, integration test), open an issue here.
- If the bug is in `spring-aiact` (audit log, technical file generator, AOP advisor) or `spring-gdpr` (DPIA, ROPA, erasure flow, retention sweep), open the issue on the upstream repo with a minimal reproducer. This demo is a good starting point for that reproducer.

## Security

Do not open public issues for security findings. See [`SECURITY.md`](SECURITY.md).

## License

By contributing you agree that your contribution is licensed under Apache 2.0, the same licence as the project.
