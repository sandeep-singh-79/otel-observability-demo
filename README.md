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
| Module 3 | ✅ Prometheus + Grafana metrics reporting                     |
| Module 4 | 📘 Documentation, portfolio/blog showcase (Ongoing)          |

---

## 📂 Directory Structure

```bash
├── src/
│   ├── main/
│   │   ├── java/com/sandeep/api/...         # Main Java source code
│   │   └── resources/
│   │       ├── frameworkConfig.properties
│   │       └── logback.xml
│   └── test/
│       ├── java/com/sandeep/api/tests/...   # Test classes
│       └── resources/
│           ├── extent-config.xml
│           ├── extent.properties
│           ├── logback-test.xml
│           ├── testng_api_suite.xml
│           ├── pacts/
│           └── test_data/
├── otel-collector-config.yml                # OTEL Collector pipeline
├── docker-compose.yaml                      # Brings up all observability services
├── DockerfileBrowser                        # (if present) for browser-based containers
├── grafana-dashboard-api-test-observability.json
├── grafana-dashboard-provisioning.yaml
├── prometheus.yml
├── pom.xml
├── README.md
├── log/                                     # Framework logs
│   └── frameworkLog.<date>.<n>.log
├── target/
│   ├── surefire-reports/                    # TestNG/Maven Surefire reports
│   ├── classes/
│   ├── test-classes/
│   └── ...
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

## 📊 Metrics + Dashboards (Module 3)

All metrics and dashboards are now fully implemented and integrated:

* Prometheus scrapes metrics from the Pushgateway (not OTEL Collector)
* Grafana dashboard templates are included for:
    * Test outcomes per suite
    * Duration trends per TEST_RUN_ID
* Metrics and dashboards are validated in both local and CI environments

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
test_result_total{test_run_id="local-test", aut="my-app-under-test", suite="MyTestSuite"}
```

---

## 📡 Prometheus Pushgateway Integration

This project now uses the [Prometheus Pushgateway](https://prometheus.io/docs/practices/pushing/) for test metrics. This is ideal for short-lived test jobs, as metrics are pushed at the end of the test run and scraped by Prometheus from the Pushgateway.

* The Pushgateway is started as a service via `docker-compose` on port `9091`.
* Your test framework pushes metrics to the Pushgateway after the suite finishes.
* Prometheus is configured to scrape the Pushgateway (see `prometheus.yml`).
* **The Prometheus HTTPServer endpoint from the test JVM and the otel-collector Prometheus endpoint are both disabled and not scraped.**

## 🛠️ Quick Start (Updated for Pushgateway)

1. **Start all services locally:**

   ```powershell
   docker-compose up -d
   ```

2. **Run tests (PowerShell):**

   ```powershell
   # Using environment variables (legacy style)
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

   **Or using Maven command-line parameters (recommended):**
   ```powershell
   mvn clean test -Dtest_run_id=local-test -Daut=my-app-under-test -Dsuite=MyTestSuite -Dpushgateway_address=localhost:9091
   ```

   For Bash (Linux/macOS):
   ```bash
   # Using environment variables
   TEST_RUN_ID=local-test AUT=my-app-under-test SUITE=MyTestSuite PUSHGATEWAY_ADDRESS=localhost:9091 mvn clean test
   
   # Or using Maven command-line parameters (recommended)
   mvn clean test -Dtest_run_id=local-test -Daut=my-app-under-test -Dsuite=MyTestSuite -Dpushgateway_address=localhost:9091
   ```

3. **View dashboards and metrics:**

   * Grafana: [http://localhost:3000](http://localhost:3000)
   * Prometheus: [http://localhost:9090](http://localhost:9090)
   * Pushgateway: [http://localhost:9091](http://localhost:9091)

---

## 📈 Prometheus Metrics (via Pushgateway)

- Metrics are pushed to the Pushgateway at the end of the test suite.
- Prometheus scrapes the Pushgateway at `host.docker.internal:9091` (see `prometheus.yml`).
- Example Prometheus query:
  ```promql
  test_result_total{test_run_id="local-test", aut="my-app-under-test", suite="MyTestSuite"}
  ```

---

## ⚙️ Configuration: Ports & Environment Variables

All observability service ports are now configurable via environment variables (with sensible defaults):

| Service        | Docker Compose Variable      | Default |
| -------------- | --------------------------- | ------- |
| Zipkin         | `ZIPKIN_PORT`               | 9411    |
| OTEL Collector | `OTEL_PORT`                 | 4317    |
| OTEL Prometheus| `OTEL_PROM_PORT`            | 8889 (DISABLED: not exposed, no longer scraped)   |
| Prometheus     | `PROMETHEUS_PORT`           | 9090    |
| Grafana        | `GRAFANA_PORT`              | 3000    |
| Pushgateway    | `PUSHGATEWAY_PORT`          | 9091    |

Override any port by setting the variable before running `docker-compose up`:

```powershell
$env:PROMETHEUS_PORT = "9095"; docker-compose up
```

---

## 📋 OpenTelemetry & Metrics Attribute Naming

All span attributes and Prometheus metric labels follow OpenTelemetry semantic conventions (snake_case):

| OpenTelemetry Attribute | Prometheus Label | Description                |
|------------------------|------------------|----------------------------|
| `test_suite`           | `test_suite`     | Test suite name            |
| `test_class`           | `test_class`     | Test class name            |
| `test_name`            | `test_name`      | Test method name           |
| `test_status`          | `test_status`    | Test result (pass/fail)    |
| `test_run_id`          | `test_run_id`    | Unique test run/session ID |
| `aut`                  | `aut`            | App under test             |

These attributes are used for both tracing (OpenTelemetry/Zipkin) and metrics (Prometheus/Grafana), enabling easy correlation.

---

## 🛠️ OpenTelemetry Configuration & Zipkin Integration

* The OTLP endpoint is set via the `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable (no hardcoded endpoints).
* Traces are exported to the OTEL Collector, which forwards to Zipkin.
* To analyze traces:
  * Go to [http://localhost:9411](http://localhost:9411)
  * Filter by `service.name = api-tests` and `test_run_id = <your_run_id>`
* All test spans and metrics are linked by `test_run_id` and other shared attributes.

---

## 🔗 Span/Metric Linking

* Every test span and metric shares the same `test_run_id`, `test_suite`, `test_class`, `test_name`, and `test_status`.
* This enables direct correlation between traces (Zipkin) and metrics (Prometheus/Grafana).
* Example: Find a failed test in Grafana, then search for its trace in Zipkin using the same `test_run_id` and `test_name`.

---

## 📘 Best Practices

* Modular spans: contract, execution, assertion
* Context tags: `test.name`, `cdc.provider`, `contract.version`
* Failures captured as OTEL `event`
* `TEST_RUN_ID` maps GitHub run to Zipkin traces
* Environment variable-based OTLP endpoint switch

---

## CI/CD Pipeline (GitHub Actions)

This project uses GitHub Actions for CI/CD. The workflow is defined in `.github/workflows/testCI.yml` and performs the following key steps:

- Spins up the observability stack (Prometheus, Grafana, Zipkin, Pushgateway, OTEL Collector) as Docker service containers.
- **Copies Prometheus and Grafana configuration files into their respective containers after startup, then restarts the containers.** This is necessary because GitHub Actions runners sometimes have issues with direct volume mounting of config files. The workflow ensures the latest configs are always loaded.
- Waits for all services to become healthy.
- Runs the full test suite with OpenTelemetry and Prometheus instrumentation.
- Verifies that metrics, traces, and dashboards are available and correct.

For full details, see the [`testCI.yml`](.github/workflows/testCI.yml) file.

**Note:** You do not need to manually copy config files when running locally with `docker-compose up -d`—the volume mounts work as expected outside of CI.

---

## 📎 Attribution

This repo is based on and inspired by the original open-source work at:
👉 [https://github.com/sandeep-singh-79/apiTestFramework](https://github.com/sandeep-singh-79/apiTestFramework)

---

## 📌 Status

✅ Modules 0–3 complete
✅ CI/CD pipeline and documentation complete

---

For any questions, feel free to raise issues or reach out!
