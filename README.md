# API Test Automation Framework with OpenTelemetry Observability

[![CodeQL](https://github.com/sandeep-singh-79/apiTestFramework/actions/workflows/codeQualityChecks.yml/badge.svg)](https://github.com/sandeep-singh-79/apiTestFramework/actions/workflows/codeQualityChecks.yml)
[![maven docker CI](https://github.com/sandeep-singh-79/apiTestFramework/actions/workflows/testCI.yml/badge.svg)](https://github.com/sandeep-singh-79/apiTestFramework/actions/workflows/testCI.yml)

## 🚀 Overview

This project is a **modular, scalable API testing framework** that combines:

* ✅ **API Functional Testing** using REST-assured
* ✅ **Contract Testing** via Pact (CDC: Consumer Driven Contracts)
* ✅ **Service Virtualization** with WireMock
* ✅ **Observability and Distributed Tracing** with OpenTelemetry + Zipkin
* ✅ **Metrics Reporting** for test pass/fail/duration using Prometheus
* ✅ **CI/CD Integration** using GitHub Actions

> 📌 Forked and inspired from the [original open-source repo](https://github.com/sandeep-singh-79/apiTestFramework)

---

## 🧱 Modular Architecture

The framework is organized into clean, isolated modules:

| Module   | Description                                                  |
| -------- | ------------------------------------------------------------ |
| Module 0 | ✅ Local instrumentation + Zipkin verification                |
| Module 1 | ✅ GitHub Actions CI integration with OTEL Collector & Zipkin |
| Module 2 | ✅ OpenTelemetry spans for every TestNG method                |
| Module 3 | 🔄 Prometheus + Grafana metrics reporting (WIP)              |
| Module 4 | 📘 Documentation, portfolio/blog showcase (Ongoing)          |

---

## 📂 Directory Structure

```bash
├── /src/test/java
│   ├── /com/sandeep/api/tests
│   │   ├── contract/pact/...          # CDC tests (PactConsumerTest)
│   │   ├── mocking/wiremock/...      # WireMock based mocking tests
│   │   ├── observability/...         # OpenTelemetry listeners
│   │   └── ...
│
├── /otel-config
│   └── otel-collector-config.yml     # OTEL Collector pipeline
│
├── docker-config.yml                 # Brings up OTEL Collector + Zipkin
├── testng.xml
├── pom.xml
└── README.md
```

---

## 🔍 Observability Setup (Module 0 + 1)

```bash
# Spin up OpenTelemetry Collector + Zipkin locally
$ docker-compose -f docker-compose.yaml up

# Run tests with a test run ID
$ TEST_RUN_ID=local-test mvn test

# View traces at Zipkin UI
Visit: http://localhost:9411
Filter: service.name = api-tests, test.run.id = local-test
```

GitHub Actions auto-runs the same setup with containerized OTEL Collector & Zipkin in workflows:

* `.github/workflows/testCI.yml`
* `.github/workflows/codeQualityChecks.yml`

---

## 🧪 Test Coverage Types

* 🔗 **Contract Tests (CDC)**: Uses Pact to ensure consumers and providers agree
* 🧪 **Mocked API Tests**: Simulates downstream APIs using WireMock
* 🧵 **Lifecycle Traces**: All test methods are traced using OpenTelemetryTestListener
* 📊 **Suite-level Metrics**: Metrics like `test_pass_total`, `test_fail_total`, `test_duration_seconds` (in progress)

---

## 📊 Metrics + Dashboards (Module 3 - WIP)

Planned:

* Prometheus scraping OTEL metrics
* Grafana dashboard templates for:

    * Test outcomes per suite
    * Duration trends per TEST\_RUN\_ID

---

## 📊 Grafana Dashboard

A pre-built Grafana dashboard is included for API test observability.

* **Dashboard JSON:** `grafana-dashboard-api-test-observability.json`
* **Provisioning YAML:** `grafana-dashboard-provisioning.yaml`

When running locally (via `docker-compose`) or in CI, Grafana will automatically load this dashboard.

* **Access Grafana:** [http://localhost:3000](http://localhost:3000)  
  Default credentials: `admin` / `admin`

---

## 📈 Prometheus Metrics

The test framework exposes custom Prometheus metrics:

* **`test_result_total`**  
  Counter for test results, labeled by suite, test name, status, AUT, and test run ID.

* **`test_duration_seconds`**  
  Histogram for test execution duration, labeled by suite, test name, AUT, and test run ID.

* **Access Prometheus:** [http://localhost:9090](http://localhost:9090)  
  Metrics endpoint: [http://localhost:9090/metrics](http://localhost:9090/metrics)

**Example Prometheus Query:**

```promql
test_result_total{status="PASS"}
```

---

## 📡 Prometheus Pushgateway Integration

This project now uses the [Prometheus Pushgateway](https://prometheus.io/docs/practices/pushing/) for test metrics. This is ideal for short-lived test jobs, as metrics are pushed at the end of the test run and scraped by Prometheus from the Pushgateway.

- The Pushgateway is started as a service via `docker-compose` on port `9091`.
- Your test framework pushes metrics to the Pushgateway after the suite finishes.
- Prometheus is configured to scrape the Pushgateway (see `prometheus.yml`).
- No need to expose the Prometheus HTTPServer endpoint from the test JVM.

## 🛠️ Quick Start (Updated for Pushgateway)

1. **Start all services locally:**

   ```powershell
   docker-compose up
   ```

2. **Run tests (PowerShell):**

   ```powershell
   $env:TEST_RUN_ID = "local-test"
   $env:AUT = "my-app-under-test"
   $env:SUITE = "MyTestSuite"
   $env:PUSHGATEWAY_ADDRESS = "localhost:9091"
   mvn clean test
   ```
   Or as a one-liner:
   ```powershell
   $env:TEST_RUN_ID="local-test"; $env:AUT="my-app-under-test"; $env:SUITE="MyTestSuite"; $env:PUSHGATEWAY_ADDRESS="localhost:9091"; mvn clean test
   ```

   For Bash (Linux/macOS):
   ```bash
   TEST_RUN_ID=local-test AUT=my-app-under-test SUITE=MyTestSuite PUSHGATEWAY_ADDRESS=localhost:9091 mvn clean test
   ```

3. **View dashboards and metrics:**

   - Grafana: [http://localhost:3000](http://localhost:3000)
   - Prometheus: [http://localhost:9090](http://localhost:9090)
   - Pushgateway: [http://localhost:9091](http://localhost:9091)

---

## 📈 Prometheus Metrics (via Pushgateway)

- Metrics are pushed to the Pushgateway at the end of the test suite.
- Prometheus scrapes the Pushgateway at `host.docker.internal:9091` (see `prometheus.yml`).
- Example Prometheus query:
  ```promql
  test_result_total{test_run_id="local-test", aut="my-app-under-test", suite="MyTestSuite"}
  ```

---

## 📝 Notes
- The old Prometheus HTTPServer endpoint (`:8081`) is no longer required for metrics scraping.
- The Pushgateway is the single source for test metrics in this setup.
- Make sure the Pushgateway service is running before executing tests.

---

## 📘 Best Practices

* Modular spans: contract, execution, assertion
* Context tags: `test.name`, `cdc.provider`, `contract.version`
* Failures captured as OTEL `event`
* `TEST_RUN_ID` maps GitHub run to Zipkin traces
* Environment variable-based OTLP endpoint switch

---

## 📎 Attribution

This repo is based on and inspired by the original open-source work at:
👉 [https://github.com/sandeep-singh-79/apiTestFramework](https://github.com/sandeep-singh-79/apiTestFramework)

---

## 📌 Status

✅ Modules 0–2 complete
🔄 Module 3 in progress
📘 Docs ongoing

---

For any questions, feel free to raise issues or reach out!
