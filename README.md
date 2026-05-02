# spring-gdpr-aiact-demo

> **End-to-end demo** of [`spring-aiact`](https://github.com/iambilotta/spring-aiact) and [`spring-gdpr`](https://github.com/iambilotta/spring-gdpr) running together inside a single Spring Boot 3.5 service. One JPA entity, one AI Act high-risk service, one runnable jar, three integration tests that pin the libraries' observable behaviour.

[![ci](https://github.com/iambilotta/spring-gdpr-aiact-demo/actions/workflows/ci.yml/badge.svg)](https://github.com/iambilotta/spring-gdpr-aiact-demo/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-21%2B-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5%2B-6db33f.svg)](https://spring.io/projects/spring-boot)
[![spring-aiact](https://jitpack.io/v/iambilotta/spring-aiact.svg?label=spring-aiact)](https://jitpack.io/#iambilotta/spring-aiact)
[![spring-gdpr](https://jitpack.io/v/iambilotta/spring-gdpr.svg?label=spring-gdpr)](https://jitpack.io/#iambilotta/spring-gdpr)

---

## Why this repo exists

If you read [`spring-aiact`](https://github.com/iambilotta/spring-aiact) and [`spring-gdpr`](https://github.com/iambilotta/spring-gdpr) and wondered "does this actually do what the README claims", this repo is the answer. It boots both libraries on `:8080`, persists to in-memory H2, and ships three integration tests that assert the libraries' observable output against the upstream `v1.1.0` artifacts pulled from JitPack.

**The build-time and runtime artifacts produced by a real run are committed under [`evidence/`](evidence/),** so you can see what the libraries do without cloning, building or booting anything. Direct links:

- Build-time: [`evidence/build-time/ropa.csv`](evidence/build-time/ropa.csv), [`evidence/build-time/dpia.md`](evidence/build-time/dpia.md), [`evidence/build-time/hiring-screener-technical-file.md`](evidence/build-time/hiring-screener-technical-file.md), [`evidence/build-time/hiring-screener-doc.pdf`](evidence/build-time/hiring-screener-doc.pdf), [`evidence/build-time/hiring-screener-dataset-cv-corpus-2025.md`](evidence/build-time/hiring-screener-dataset-cv-corpus-2025.md).
- Runtime: [`evidence/runtime/hiring-screener.ndjson`](evidence/runtime/hiring-screener.ndjson), [`evidence/runtime/aiact-log-verify.json`](evidence/runtime/aiact-log-verify.json), [`evidence/runtime/aiact-log-head.json`](evidence/runtime/aiact-log-head.json).

---

## What this app is

- One JPA entity (`Customer`) annotated with `@GdprDataSubjects` / `@GdprLegalBasis` / `@GdprRetention` / `@GdprErasable` / `@GdprPersonalData`.
- One AI Act high-risk service (`HiringScreener`) annotated with `@AiActHighRiskSystem`, `@AiActIntendedPurpose`, `@AiActOversight`, `@AiActDataset`, `@AiActAccuracyMetric`, plus `@AiActLog` on the scoring method.
- One `ErasureHandler` bean for `Customer` (deletes by id, audited).
- H2 in-memory DB. No Postgres, no docker-compose. The point is to show the libraries, not infrastructure.

```
┌───────────── build time (mvn compile) ─────────────┐  ┌──── runtime (Spring Boot) ────┐
│ spring-gdpr-processor → ropa.csv, dpia.md          │  │ AOP advisor on @AiActLog       │
│ aiact-build-artifacts profile (opt-in, see below): │  │ AOP advisor on @GdprPersonalData│
│   technical-file.md, doc.pdf, datasheets           │  │ /aiact/log/{verify,head,export}│
└────────────────────────────────────────────────────┘  │ /gdpr/audit/access             │
                                                        │ /gdpr/erasure/{subjectId}      │
                                                        └────────────────────────────────┘
```

## Run it

```bash
git clone git@github.com:iambilotta/spring-gdpr-aiact-demo.git
cd spring-gdpr-aiact-demo
mvn -B verify              # builds, runs integration tests against upstream v1.1.0 from JitPack
java -jar target/spring-gdpr-aiact-demo-1.1.0.jar
```

App binds on `:8080`. Audit logs written under `./aiact-logs/`. H2 console at `/h2-console`.

The first build downloads the upstream libraries from JitPack:

```
com.github.iambilotta.spring-aiact:spring-aiact-spring-boot-starter:v1.1.0
com.github.iambilotta.spring-gdpr:spring-gdpr-starter:v1.1.0
com.github.iambilotta.spring-gdpr:spring-gdpr-annotations:v1.1.0
```

JitPack lazily builds these on the first request, which can take a couple of minutes the first time only. Subsequent builds are instant.

### Optional: build-time artifacts (Annex IV technical file, Article 47 DoC PDF, dataset datasheets)

The `spring-aiact-maven-plugin` cannot run via JitPack (JitPack rewrites the plugin's groupId, which Maven rejects as inconsistent in the plugin descriptor). To exercise the build-time generators:

```bash
git clone https://github.com/iambilotta/spring-aiact ../spring-aiact
( cd ../spring-aiact && ./mvnw -B -DskipTests install )   # publishes the plugin to your local Maven repo
mvn -P aiact-build-artifacts verify                       # generates target/generated-docs/*
```

Output (artifact filenames) is shown further down in the [build-time evidence](#what-mvn-verify-produces-build-time-evidence) section.

## The integration tests are the load-bearing claim

The `src/test/java` tree contains three Spring Boot integration tests. **Each test asserts an observable result of the upstream libraries**, not the demo's own glue. This is what makes the demo a continuous heartbeat against `spring-aiact` and `spring-gdpr`: if either library starts misbehaving in a future Spring Boot upgrade, refactor or annotation rename, these tests go red.

| Test | What it pins |
|---|---|
| [`AiActAuditChainIT`](src/test/java/com/iambilotta/demo/AiActAuditChainIT.java) | One POST to `/hiring/score` appends one record to the NDJSON HMAC chain, and `/aiact/log/verify` reports `inspected: >=1, invalid: 0`. Three POSTs advance the chain head HMAC away from the seed. |
| [`GdprAuditAndErasureIT`](src/test/java/com/iambilotta/demo/GdprAuditAndErasureIT.java) | Reading a `Customer` fires the GDPR advisor, and the access record is queryable through `/gdpr/audit/access` (Article 15). `DELETE /gdpr/erasure/{id}` returns the affected-by-type map, the row is gone, and the next read 404s. |
| [`TamperEvidenceIT`](src/test/java/com/iambilotta/demo/TamperEvidenceIT.java) | After one logged invocation, the NDJSON file exists with `snake_case` keys (regression for the v0.1.x ObjectMapper-shadowing bug, closed in `spring-aiact` v1.0.0). Flipping one byte of `input_hash` makes `/aiact/log/verify` return `invalid: >=1` with the failed event id; restoring the byte returns `invalid: 0`. |

Run them: `mvn verify`. CI runs them on every push and PR against Java 21 and Java 25.

---

## What `mvn verify` produces (build-time evidence)

Both libraries write artifacts you can commit to git or hand to a DPO / notified body.

### `target/generated-sources/annotations/spring/gdpr/ropa.csv`

```csv
entity,data_subjects,legal_basis,retention_period,strategy,special_category
com.iambilotta.demo.Customer,customer,6(1)(b),P5Y,ANONYMIZE,false
```

### `target/generated-sources/annotations/spring/gdpr/dpia.md` (excerpt)

```markdown
# Data Protection Impact Assessment (Art. 35) Scaffold

## 1. Records of processing activities (Art. 30)

| Entity | Data subjects | Legal basis | Retention | Strategy | Special category |
|---|---|---|---|---|---|
| com.iambilotta.demo.Customer | customer | 6(1)(b) | P5Y | ANONYMIZE | no |

## 2. Personal-data access points

| Type | Member |
|---|---|
| com.iambilotta.demo.CustomerService | read |
| com.iambilotta.demo.CustomerService | subjectId |

## 3. Necessity and proportionality assessment
(Fill in.)
...
```

Sections 1-2 are populated mechanically. Sections 3-6 are deliberately empty: those are human judgement.

### `target/generated-docs/hiring-screener-technical-file.md` (excerpt, requires the `aiact-build-artifacts` profile)

```markdown
# Technical File, AI Act Annex IV

System:          Hiring screener (demo)
System id:       hiring-screener
Provider:        iambilotta demo
Version:         0.1.0
Annex III:       III.4 (EMPLOYMENT_AND_WORKERS_MANAGEMENT), sub-point 4(a)

## 1. General description
Intended purpose: Score CV applicants for an engineering role.
Deployment context: HR triage before any human review.
Intended users: HR specialists
Foreseeable misuse: Auto-rejection without human review; Use outside the engineering role context

## 2. Design and development
### Logged operations (Article 12 attribution)
| Class           | Method | Operation             | Model id              | Hash    | Input | Output |
|-----------------|--------|-----------------------|-----------------------|---------|-------|--------|
| HiringScreener  | score  | HiringScreener.score  | hiring-screener@0.1.0 | SHA-256 | yes   | yes    |
### Article 14 human oversight
- Level: HUMAN_IN_THE_LOOP
- Override role: hr

## 3. Datasets and data governance
| Id              | Name                       | Phase    | Source                | Size           | Personal data | Documented biases                       |
|-----------------|----------------------------|----------|-----------------------|----------------|---------------|-----------------------------------------|
| cv-corpus-2025  | Anonymized CV corpus 2025  | training | internal-s3://cv-2025 | 12,500 records | yes           | under-representation of women in STEM   |

[...nine sections total, generated from @AiAct* annotations...]
```

Plus `hiring-screener-doc.pdf` (Article 47 Declaration of Conformity, signature placeholder) and `hiring-screener-dataset-cv-corpus-2025.md` (Article 10 dataset datasheet).

The `mvn verify` goal under the `aiact-build-artifacts` profile also fails the build with a precise message if any of the four AI Act companion annotations is missing on a class marked `@AiActHighRiskSystem`. Try removing `@AiActOversight` from `HiringScreener.java` and re-running: build red.

---

## What runtime produces (live evidence)

A real session, captured against the running app.

### POST a customer (the GDPR advisor will audit any subsequent read)

```bash
$ curl -X POST http://localhost:8080/customers \
       -H 'Content-Type: application/json' \
       -d '{"id":"sub-42","email":"alice@example.com","fullName":"Alice Rossi"}'
{"id":"sub-42","email":"alice@example.com","fullName":"Alice Rossi","createdAt":"2026-05-02T09:34:49.861721849Z"}
```

### Read it (this is the access that gets audited)

```bash
$ curl http://localhost:8080/customers/sub-42
{"id":"sub-42","email":"alice@example.com","fullName":"Alice Rossi","createdAt":"2026-05-02T09:34:49.861722Z"}
```

### Score a candidate (this is the call the AI Act log records)

```bash
$ curl -X POST http://localhost:8080/hiring/score \
       -H 'Content-Type: application/json' \
       -d '{"candidateId":"c-1","cvText":"Senior Java engineer with proven leadership..."}'
{"candidateId":"c-1","score":0.203,"decisionHint":"MANUAL_REVIEW_REQUIRED"}
```

### See the AI Act audit chain on disk

```bash
$ cat aiact-logs/hiring-screener.ndjson
```

```json
{"event_id":"08e1a981-a733-4934-bc14-44ff0cae3e4e","event_kind":"INVOCATION",
 "timestamp":"2026-05-02T09:34:50.016657857Z","system_id":"hiring-screener",
 "system_version":"0.1.0","operation":"HiringScreener.score",
 "model_id":"hiring-screener@0.1.0",
 "input_hash":"sha256:b4fe4d8b2defda79049a3a2737caab95138683be901cfc54bee14d53398eb3e6",
 "output_hash":"sha256:84845884ed491d59564b107bbcdb8435e7f8a42a096dff973ca265cf31f5f91b",
 "hash_algorithm":"SHA-256","latency_ms":0,
 "prev_hmac":"0000000000000000000000000000000000000000000000000000000000000000",
 "record_hmac":"0ed15f0b15084444be2880e87ba615b6c98612d27921fd61a4f25d5ee51684d3"}
```

### Verify the chain

```bash
$ curl 'http://localhost:8080/aiact/log/verify?system=hiring-screener'
{"systemId":"hiring-screener","from":null,"to":null,"inspected":1,"invalid":0,"failedEventIds":[]}
```

### Tamper test, the property that actually matters

The reason Article 12 wants HMAC chaining: an attacker cannot edit a record without leaving forensic evidence.

```bash
# Corrupt one byte of an old record on disk
$ sed -i '0,/sha256:b4fe/{s/sha256:b4fe/sha256:00fe/}' aiact-logs/hiring-screener.ndjson

# Verify reports the exact event id that broke the chain
$ curl 'http://localhost:8080/aiact/log/verify?system=hiring-screener'
{"systemId":"hiring-screener","from":null,"to":null,"inspected":1,"invalid":1,
 "failedEventIds":["08e1a981-a733-4934-bc14-44ff0cae3e4e"]}

# Restore the file, the chain is whole again
$ git checkout -- aiact-logs/hiring-screener.ndjson  # or restore from backup
$ curl 'http://localhost:8080/aiact/log/verify?system=hiring-screener'
{"systemId":"hiring-screener","from":null,"to":null,"inspected":1,"invalid":0,"failedEventIds":[]}
```

### Read the GDPR audit log (Article 15 export)

```bash
$ curl 'http://localhost:8080/gdpr/audit/access?subjectId=sub-42'
[
  {"eventId":"080383ae-8778-4e6a-9781-9a9970211590","at":"2026-05-02T09:34:49.991133Z",
   "actor":"system","subjectId":"sub-42",
   "targetType":"com.iambilotta.demo.CustomerService","targetMember":"read",
   "legalBasis":null,"specialCategory":false}
]
```

### Erase the subject (Article 17)

```bash
$ curl -X DELETE http://localhost:8080/gdpr/erasure/sub-42
{"subjectId":"sub-42","affectedByType":{"com.iambilotta.demo.Customer":1}}

# Re-read fails: the row is gone
$ curl -o /dev/null -w '%{http_code}\n' http://localhost:8080/customers/sub-42
404
```

The `affectedByType` map is built by the library: it walks every registered `ErasureHandler`, calls `erase(subjectId)`, sums the counts and audits each step. In this demo there is one handler (`CustomerErasureHandler`); a real app would register one per table holding personal data, and the library would handle ordering and audit on each call.

---

## What this demo deliberately does NOT show

- Production Spring Security wiring around `/aiact/**` and `/gdpr/**`. Both libraries default-deny in production but the demo runs with `aiact.endpoints.allow-without-guard=true` and no `DPO` role on `/gdpr/**`.
- HMAC secret rotation. The HMAC secret in `application.yaml` is a dev placeholder. Production-grade rotation is described in `spring-aiact/docs/PRODUCTION.md`.
- A persistent DB. H2 is in-memory and resets on restart.
- Multi-pod single-writer-lock semantics. One JVM, one writer.

These are real concerns for a production deployment, just out of scope for a 200-line demo.

## Contributing, security, code of conduct

- [`CONTRIBUTING.md`](CONTRIBUTING.md): what belongs here vs upstream, coding conventions, PR flow, DCO.
- [`SECURITY.md`](SECURITY.md): how to report a vulnerability (do not open a public issue).
- [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md): Contributor Covenant 2.1.

## License

Apache License 2.0. See [LICENSE](LICENSE). The two libraries integrated by this demo are also Apache 2.0.
