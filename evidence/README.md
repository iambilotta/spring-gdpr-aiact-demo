# Evidence

Output captured from a real run of this demo against `spring-aiact:v1.1.0` and `spring-gdpr:v1.1.0` (JitPack). Committed so a reader can see what the libraries produce without booting the app or running `mvn verify`.

## `build-time/` — what `mvn -P aiact-build-artifacts verify` produces

| File | Generator | Purpose |
|---|---|---|
| `ropa.csv` | `spring-gdpr-processor` (annotation processor) | GDPR Article 30: records of processing activities |
| `dpia.md` | `spring-gdpr-processor` | GDPR Article 35 scaffold: DPIA |
| `hiring-screener-technical-file.md` | `spring-aiact-maven-plugin` | AI Act Annex IV (nine sections) |
| `hiring-screener-doc.pdf` | `spring-aiact-maven-plugin` | AI Act Article 47 Declaration of Conformity (signature placeholder) |
| `hiring-screener-dataset-cv-corpus-2025.md` | `spring-aiact-maven-plugin` | AI Act Article 10 dataset datasheet |

These are pure functions of the `@AiAct*` and `@Gdpr*` annotations on the demo's source. Refactor an annotation, the next build regenerates the corresponding section.

## `runtime/` — what the live app produces

| File | Endpoint that produces it | Purpose |
|---|---|---|
| `hiring-screener.ndjson` | `POST /hiring/score` (AOP advisor on `@AiActLog`) | AI Act Article 12 audit chain. Two records, the second's `prev_hmac` matches the first's `record_hmac` (the HMAC chain link). |
| `aiact-log-verify.json` | `GET /aiact/log/verify?system=hiring-screener` | Chain verification report (`inspected: 2, invalid: 0`). Tamper a byte of `hiring-screener.ndjson` and this returns the failed event id. |
| `aiact-log-head.json` | `GET /aiact/log/head?system=hiring-screener` | Current head HMAC (cheap tamper canary). Wire format is `{"system_id":..., "head_hmac":...}` even after the v1.1.0 refactor that moved the response to a typed `ChainHead` record (`@JsonProperty` keeps the snake_case JSON keys). |

GDPR runtime evidence (audit access export, erasure flow) is produced inside the H2 in-memory database during `mvn verify`'s integration tests; it is not a file artifact. The `GdprAuditAndErasureIT` integration test asserts the exact wire shape.

## How this directory is regenerated

```bash
# Install the libraries locally (the aiact Maven plugin is not on JitPack).
( cd ../spring-aiact && ./mvnw -B -DskipTests install )
( cd ../spring-gdpr  && ./mvnw -B -DskipTests install )

# Generate.
mvn -B -P aiact-build-artifacts verify

# Capture build-time output.
cp target/generated-sources/annotations/spring/gdpr/ropa.csv evidence/build-time/
cp target/generated-sources/annotations/spring/gdpr/dpia.md  evidence/build-time/
cp target/generated-docs/hiring-screener-technical-file.md   evidence/build-time/
cp target/generated-docs/hiring-screener-doc.pdf             evidence/build-time/
cp target/generated-docs/hiring-screener-dataset-cv-corpus-2025.md evidence/build-time/

# Capture runtime output (boot the app first, then 1-2 POST /hiring/score).
cp aiact-logs/hiring-screener.ndjson evidence/runtime/
curl -s 'http://localhost:8080/aiact/log/verify?system=hiring-screener' | python3 -m json.tool > evidence/runtime/aiact-log-verify.json
curl -s 'http://localhost:8080/aiact/log/head?system=hiring-screener'   | python3 -m json.tool > evidence/runtime/aiact-log-head.json
```

The artifacts in this directory will get out of sync with the demo source over time. They are committed as a reference snapshot, not as a continuous output.
