# SmartAsset Sentinel — Interview Project Guide

## What this project is

SmartAsset Sentinel is a small facility and asset-management application, plus an automated test suite. A facilities team can track equipment, create maintenance work orders, and move those work orders through a controlled lifecycle.

The application is the **system under test**. The JUnit 5 and Spring MockMvc tests exercise its HTTP endpoints, business rules, error responses, and a small set of stable dashboard locators.

## A 60-second project introduction

> “I built SmartAsset Sentinel as a realistic asset-management application and used it to demonstrate test engineering. It uses Java 17, Spring Boot, REST APIs, Spring Data JPA, and H2, with a PostgreSQL profile. Assets can be created and filtered, and maintenance work orders follow a defined state machine. I added Spring Boot and MockMvc integration tests for both successful and invalid scenarios. The tests verify not only HTTP responses but also business side effects—for example, creating a work order puts its asset into maintenance, and completing the last active work order releases it. I also check the dashboard’s stable test locators. The current suite focuses on API and application integration testing; browser automation and an AI-driven QA agent are future extensions, not implemented features.”

Adjust the pitch to your own experience and be ready to explain any part you claim to have built.

## Architecture at a glance

```text
Browser dashboard (Thymeleaf + JavaScript)
                |
       REST controllers
                |
       Application services
                |
     Spring Data repositories
                |
       H2 / PostgreSQL
```

- **Dashboard:** `DashboardController` renders the page. The dashboard JavaScript calls the asset and work-order APIs and updates the tables and summary counts.
- **Controllers:** `AssetRestController` and `WorkOrderRestController` define the HTTP API and request validation.
- **Services:** `AssetService` and `WorkOrderService` contain application rules and coordinate related entity updates.
- **Persistence:** Spring Data repositories use JPA. H2 is the default local database; a PostgreSQL profile is also configured.
- **Error handling:** `GlobalExceptionHandler` maps expected errors to API responses, including validation errors (`400`), missing resources (`404`), and conflicts (`409`).
- **Test suite:** `@SpringBootTest` and MockMvc exercise the application through its MVC layer with an isolated H2-backed test context.

## Core workflows to understand

### Asset management

Assets have a type, location, serial number, status, and optional service dates. The API supports listing and filtering, fetching, creating, updating, and deleting assets. Serial numbers must be unique; invalid requests are rejected with field-level validation details.

### Work-order lifecycle

Work orders are associated with an asset and use this state machine:

```text
OPEN        -> IN_PROGRESS | CANCELLED
IN_PROGRESS -> COMPLETED   | CANCELLED
COMPLETED   -> (terminal)
CANCELLED   -> (terminal)
```

Important business rules:

1. Creating a work order puts its asset into `UNDER_MAINTENANCE`.
2. A work order cannot be created for a `DECOMMISSIONED` asset.
3. Completing a work order records its completion time and updates the asset’s last service date.
4. When no active work orders remain, an asset in maintenance returns to `OPERATIONAL`.
5. Deleting the last active work order also releases the asset from maintenance.

These are useful interview examples because they require checking both an API response and the resulting application/database state.

## What the tests demonstrate

Run the suite from the repository root:

```bash
mvn test
```

The current suite contains **11 tests**:

| Test class | What it checks |
|---|---|
| `AssetApiTests` | Combined filters, default status on creation, duplicate serial conflict, field validation, and missing asset response |
| `WorkOrderApiTests` | Maintenance side effect, completion date and asset release, invalid transition rejection, decommissioned-asset rejection, and release after deleting the last active work order |
| `DashboardPageTests` | Dashboard renders stable `data-testid` locators used by automation |

Tests disable demo-data seeding and the H2 TCP listener, so they use a clean, self-contained test setup and do not need a separate application process. Maven Surefire writes results under `mock-asset-service/target/surefire-reports/`.

### A representative test story

For work-order completion, the suite:

1. Creates a test asset and a work order through the API.
2. Moves the work order from `OPEN` to `IN_PROGRESS`, then to `COMPLETED`.
3. Checks the returned state and completion timestamp.
4. Reads the asset back from the repository and verifies its status and last service date.

For a negative path, it tries to move a work order directly from `OPEN` to `COMPLETED`, expects `409 Conflict`, and verifies the work order remains `OPEN`.

## Panel demo plan

### Option A: automated suite (recommended)

1. From `smartasset-sentinel/`, run `mvn test`.
2. Show the passing test summary in the terminal.
3. Open `mock-asset-service/target/surefire-reports/` to show the per-class reports.
4. Walk through one positive case and one negative case in `WorkOrderApiTests`.
5. Point out the repository assertions that verify side effects, not only status codes.

### Option B: application walkthrough

Start the app from the repository root:

```bash
mvn -pl mock-asset-service spring-boot:run
```

Then open `http://localhost:8080/` and demonstrate the dashboard. You can create an asset, create a work order for it, start the work order, and complete it. The API examples and endpoints are in `README.md`.

If running both the application and tests during a panel session, explain that the tests use their own test context; they do not need to use the running dashboard instance.

## Likely interview questions

### Why use MockMvc instead of calling a running server?

MockMvc exercises Spring MVC request handling, validation, controllers, and the application services without requiring a separately started server. It makes the integration suite easier to run locally. A real-server test or browser test could complement it when validating deployment wiring or end-user behavior.

### Why check database state as well as the response?

A `200` response does not prove that related business state changed correctly. Work-order operations affect both a work order and its asset, so I verify those outcomes too.

### How are tests kept independent?

The test configuration disables demo seeding and the H2 TCP listener. The API test classes use transactional test setup and create their own records rather than depending on seeded IDs.

### What would you test next?

I would add cases for concurrent or duplicate work-order operations, more boundary validation, asset deletion with associated work orders, and PostgreSQL-specific behavior. I would also add browser-driven tests for the dashboard workflows and run the suite automatically in CI.

### Is this already an agentic QA system?

No. The current project demonstrates conventional Java/Spring integration testing and testable UI contracts. It does **not** contain an LLM, autonomous test-planning loop, or agent-driven defect triage. If extending it, I would start with a bounded agent that can select from approved test tools and summarize failures, while keeping test execution deterministic and requiring human review for defect conclusions.

### What are the current limitations?

- The suite uses MockMvc; it does not automate a real browser.
- The dashboard test checks rendered locator attributes, not client-side interactions.
- There is no CI pipeline, defect tracker integration, or agentic orchestration in this version.
- The automated suite uses H2; the PostgreSQL profile is configured but is not covered by these tests.

Be candid about these limits. Describing planned work as already implemented can undermine an otherwise strong demonstration.

## Source map

- Application setup: `mock-asset-service/src/main/java/com/smartasset/sentinel/MockAssetServiceApplication.java`
- Asset API and service: `mock-asset-service/src/main/java/com/smartasset/sentinel/controller/AssetRestController.java`, `mock-asset-service/src/main/java/com/smartasset/sentinel/service/AssetService.java`
- Work-order API and service: `mock-asset-service/src/main/java/com/smartasset/sentinel/controller/WorkOrderRestController.java`, `mock-asset-service/src/main/java/com/smartasset/sentinel/service/WorkOrderService.java`
- State machine: `mock-asset-service/src/main/java/com/smartasset/sentinel/model/WorkOrderStatus.java`
- API error mapping: `mock-asset-service/src/main/java/com/smartasset/sentinel/exception/GlobalExceptionHandler.java`
- Integration tests: `mock-asset-service/src/test/java/com/smartasset/sentinel/controller/`
- Dashboard: `mock-asset-service/src/main/resources/templates/dashboard.html`
