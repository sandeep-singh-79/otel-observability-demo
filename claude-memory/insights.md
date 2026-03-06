# Analysis Insights
> Last updated: 2026-03-06
> Root cause analyses, debugging findings, and diagnostic conclusions only.

---

## [2026-03-01 & 2026-03-06] PR #27 — GrpcSenderConfig NoClassDefFoundError

### Symptom
```
Error: There was an error in the forked process
Error: io/opentelemetry/sdk/common/export/GrpcSenderConfig
Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
BUILD FAILURE — Total time: ~17-21s
```
The surefire forked JVM crashed before any tests ran.

### Root Cause
**Maven dependency mediation conflict** caused by version skew within the OTel release train.

The original `pom.xml` had:
```xml
<opentelemetry-sdk.version>1.50.0</opentelemetry-sdk.version>
<opentelemetry-exporter-otlp.version>1.59.0</opentelemetry-exporter-otlp.version>
```

Resolution chain:
1. `opentelemetry-exporter-otlp:1.59.0` → depends on `opentelemetry-sdk-common:1.59.0` (contains `GrpcSenderConfig`)
2. `opentelemetry-sdk:1.50.0` → depends on `opentelemetry-sdk-common:1.50.0` (does NOT contain `GrpcSenderConfig`)
3. `au.com.dius.pact:consumer:4.6.20` → also depends on `opentelemetry-sdk-common` at a pre-1.43 version, declared before OTel in pom.xml
4. Maven's "nearest + first wins" rule resolved `opentelemetry-sdk-common` to `1.50.0`
5. At runtime `opentelemetry-exporter-otlp:1.59.0` tried to load `GrpcSenderConfig` → class not found → JVM crash

`GrpcSenderConfig` was introduced in `opentelemetry-sdk-common:1.43.0`. Any exporter ≥ 1.43.0 combined with sdk-common < 1.43.0 will produce this error.

### Fix Applied
Two changes to `pom.xml`:

1. **Unified version property** — replaced two separate properties with one:
   ```xml
   <!-- Before -->
   <opentelemetry-sdk.version>1.50.0</opentelemetry-sdk.version>
   <opentelemetry-exporter-otlp.version>1.59.0</opentelemetry-exporter-otlp.version>
   
   <!-- After -->
   <opentelemetry.version>1.59.0</opentelemetry.version>
   ```

2. **BOM in dependencyManagement** — pins all `io.opentelemetry:*` transitives to 1.59.0:
   ```xml
   <dependencyManagement>
     <dependencies>
       <dependency>
         <groupId>io.opentelemetry</groupId>
         <artifactId>opentelemetry-bom</artifactId>
         <version>${opentelemetry.version}</version>
         <type>pom</type>
         <scope>import</scope>
       </dependency>
     </dependencies>
   </dependencyManagement>
   ```

### Validation
- Local test run: **5/5 passed**, 0 failures, 0 errors
- CI (PR #27): `API Test CI` ✅, `CodeQL` ✅ — all checks green

### Preventive Pattern
Always use the OTel BOM when mixing any OTel artifacts. Never split `opentelemetry-sdk` and `opentelemetry-exporter-otlp` versions — they must be co-versioned. Any future Dependabot bump that only bumps one OTel artifact should be handled by updating the single `opentelemetry.version` property.

---

## [2026-03-06] Dependabot Branch vs otel_observability Branch Divergence

### Observation
Local `otel_observability` branch (12 commits ahead) vs remote `origin/otel_observability` (24 commits ahead) — the branches had diverged. The remote contained all the Dependabot bumps that happened after the branch was last pushed locally.

### Insight
The correct branch for the fix was **not** `otel_observability` but the specific Dependabot PR branch (`dependabot/maven/io.opentelemetry-opentelemetry-exporter-otlp-1.59.0`). Applying a fix to `otel_observability` would not have fixed the failing PR. Always verify the active PR's head branch before making fix commits.

---
<!-- Add new insights below this line as separate H2 sections with date prefix -->
