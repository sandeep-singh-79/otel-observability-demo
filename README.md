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
$ docker-compose -f docker-config.yml up

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

## 🧰 Tech Stack

* ☕ Java 17
* 🧪 TestNG
* 🔍 OpenTelemetry SDK (OTLP Exporter)
* 📦 Pact (CDC Testing)
* 🎭 WireMock (Service Virtualization)
* 🌐 REST-assured
* 🐙 GitHub Actions (CI/CD)
* 📡 Zipkin (Trace Viewer)
* 📈 Prometheus + Grafana (Metrics/Dashboard - upcoming)

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
