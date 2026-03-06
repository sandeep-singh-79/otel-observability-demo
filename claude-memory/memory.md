# Context Memory
> Last updated: 2026-03-06
> Read this file at every session start, after context compaction, or whenever context may have been lost.

---

## Repository

| Field | Value |
|---|---|
| Repo | sandeep-singh-79/otel-observability-demo |
| Remote | git@github.com:sandeep-singh-79/otel-observability-demo.git |
| Default branch | `main` |
| Working branch (as of last update) | `dependabot/maven/io.opentelemetry-opentelemetry-exporter-otlp-1.59.0` |
| Local path | `c:\Data\IdeaProjects\otel-observability-demo` |
| Language / Build | Java 17, Maven (pom.xml) |

---

## Project Overview

An API test automation framework demonstrating OpenTelemetry (OTel) observability. Tests are run via TestNG + Maven Surefire. Observability stack: OTel Collector → Zipkin (traces), Prometheus + Pushgateway (metrics), Grafana (dashboards). CI is GitHub Actions.

---

## Key Files

| File | Purpose |
|---|---|
| `pom.xml` | Maven build — all dependency versions live here |
| `.github/workflows/testCI.yml` | Main CI pipeline — spins up all services via Docker, runs `mvn clean test` |
| `.github/workflows/codeQualityChecks.yml` | CodeQL static analysis (Java) |
| `.github/dependabot.yml` | Dependabot config — maven + github-actions, monthly schedule |
| `src/main/java/.../config/OpenTelemetryConfig.java` | Singleton OTel SDK setup, uses OtlpGrpcSpanExporter |
| `src/main/java/.../config/PrometheusTestMetrics.java` | Prometheus simpleclient metrics pushed to Pushgateway |
| `src/main/java/.../listeners/OpenTelemetryTestListener.java` | TestNG listener integrating OTel spans per test |

---

## Current Dependency Versions (pom.xml — post fix)

| Artifact | Version |
|---|---|
| `opentelemetry.version` (all OTel) | `1.59.0` |
| `logback.version` | `1.5.32` |
| `slf4j.version` | `2.0.17` |
| `restassured.version` | `6.0.0` |
| `surefire.version` | `3.5.4` |
| `testng` | `7.12.0` |
| `lombok` | `1.18.42` |
| `pact consumer` | `4.6.20` |
| `json` (org.json) | `20251224` |
| `json-schema-validator` | `6.0.0` |
| `prometheus simpleclient` | `0.16.0` |
| `wiremock-jre8` | `2.35.2` |
| `awaitility` | `4.3.0` |

---

## Active / Recent PRs

| PR | Branch | Status | Notes |
|---|---|---|---|
| #27 | `dependabot/maven/io.opentelemetry-opentelemetry-exporter-otlp-1.59.0` | ✅ All checks passing | Fixed by us — see insights.md |
| #1 | Dependabot logback 1.5.17→1.5.18 | Superseded by later bumps | |

---

## CI Pipeline Summary (testCI.yml)

Services started as Docker containers in the job: zipkin, otel-collector, prometheus, grafana, pushgateway.

Key steps:
1. Checkout + JDK 17 (adopt) setup
2. Maven cache
3. Copy `prometheus.yml` / Grafana dashboard configs into running containers, restart them
4. Health check waits for Prometheus, Grafana, Zipkin, Pushgateway
5. `mvn clean test` (no profile — default surefire config)
6. Post-test: verify Prometheus metrics, Grafana dashboard, Zipkin traces, Pushgateway metrics

Known issue: `otel-collector` service container uses `volumes` mount (supported in `services:` on self-hosted but may behave differently on GitHub-hosted runners — monitor).

---

## Ongoing Initiative

**Automate Dependabot PR lifecycle** — see `plan.md` for full detail.
Short version: auto-merge passing patch/minor PRs from Dependabot; auto-analyse and comment on failures using GitHub Copilot/Models.
