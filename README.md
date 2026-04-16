# Currency Rate Service

A microservices demo built with Spring Boot and gRPC, demonstrating service discovery (Zookeeper), consumer-driven contract testing (Pact), observability (Micrometer + Prometheus + Grafana), and the Twelve-Factor App principles **Build / Release / Run** (V) and **Dev/Prod Parity** (X).

## Architecture

```
┌─────────────────┐   x-client-name header   ┌──────────────────────────┐
│   rate-printer  │  ──── gRPC GetRate() ───►│  currency-rate-provider  │
│   (client)      │  ◄─── {pair, rate} ──────│  (service1 / service2)   │
└─────────────────┘                          └──────────────────────────┘
        │                                                  │
        │  publishes pact                    verifies pact │
        ▼                                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Pact Broker :9292                            │
└──────────────────────────────────────────────────────────────────────┘
        All services register / discover via Zookeeper :2181

┌─────────────┐   scrape /actuator/prometheus   ┌──────────────────┐
│  Prometheus │ ◄────────────────────────────── │ service1/2,client│
│   :9091     │ ◄──── JMX exporter :7070 ─────  │ zookeeper        │
└──────┬──────┘                                 └──────────────────┘
       │  datasource
       ▼
┌─────────────┐
│   Grafana   │  http://localhost:3000
│   :3000     │
└─────────────┘
```

## Modules

| Module | Description |
|--------|-------------|
| `currency-rate-proto` | Protobuf definitions for the `CurrencyRateService` gRPC API |
| `currency-rate-provider` | gRPC server — returns a random USD/RUB exchange rate; exposes Micrometer metrics |
| `rate-printer` | gRPC client — fetches and prints the rate every 5 s; injects client identity into gRPC metadata |

### gRPC API (`currency_rate.proto`)

```protobuf
service CurrencyRateService {
  rpc GetRate (RateRequest) returns (RateResponse);
}

message RateRequest {}

message RateResponse {
  string pair = 1;   // e.g. "USDRUB"
  double rate = 2;   // e.g. 92.47
}
```

## Requirements

- Docker & Docker Compose (no local JDK or Maven needed — the build runs inside Docker)

## Dev/Prod Parity (Twelve-Factor X)

Backing services (ZooKeeper, Prometheus, Grafana, Pact Broker) are identical in every environment — no lightweight substitutes in development. The compose file layout enforces this:

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Shared config for all environments: infrastructure + app service ports, env vars, and dependencies. Never run alone. |
| `docker-compose.dev.yml` | Development overlay: adds `build:` directives and activates `SPRING_PROFILES_ACTIVE=dev`. |
| `releases/<id>/docker-compose.release.yml` | Production overlay: pins immutable image tags and activates `SPRING_PROFILES_ACTIVE=prod`. |

Each Spring service has profile-specific property files:
- `application-dev.properties` — `logging.level.root=DEBUG`
- `application-prod.properties` — `logging.level.root=INFO`

### Start the dev stack

```bash
./dev.sh        # builds images from source, starts all services with dev profile
./dev.sh down   # stop
```

---

## Build / Release / Run (Twelve-Factor V)

The project enforces a strict three-stage pipeline. Each stage has a single responsibility and cannot bleed into the next.

```
  ./build.sh          ./release.sh <build-id>       ./run.sh <release-id>
┌───────────┐        ┌──────────────────────┐       ┌──────────────────┐
│   BUILD   │───────►│       RELEASE        │──────►│       RUN        │
│           │        │                      │       │                  │
│ Maven     │        │ build + config =     │       │ docker compose   │
│ inside    │        │ immutable manifest   │       │ from pinned      │
│ Docker    │        │ releases/<id>/       │       │ image tags only  │
│           │        │   manifest.json      │       │                  │
│ tags:     │        │   docker-compose     │       │ no build step,   │
│ <svc>:    │        │   .release.yml       │       │ no Maven,        │
│ build-    │        │                      │       │ no toolchain     │
│ <git-sha> │        │ append-only ledger   │       │                  │
└───────────┘        └──────────────────────┘       └──────────────────┘
```

### Stage 1 — Build

```bash
BUILD_ID=$(./build.sh)
```

`build.sh` triggers a multi-stage Docker build for each service. Maven runs **inside the Docker build stage** — no local JDK or Maven is required on the host. The resulting images are tagged `<service>:build-<git-sha>` and stored in the local Docker daemon.

The Dockerfiles have two stages:
- **`build`** — JDK + Maven; compiles sources and produces a fat JAR
- **`run`** — JRE only; copies the JAR from the build stage; no toolchain survives into the runtime image

### Stage 2 — Release

```bash
RELEASE_ID=$(./release.sh "$BUILD_ID")
```

`release.sh` verifies the build images exist, then creates an immutable release directory:

```
releases/
└── v20260416-143022/
    ├── manifest.json              # build ID, git SHA, timestamp — never edited
    └── docker-compose.release.yml # pins image tags, activates prod Spring profile
```

Releases are an **append-only ledger**: once created, a release directory is never modified. Any config change requires a new `./release.sh` invocation, which produces a new timestamped entry.

### Stage 3 — Run

```bash
./run.sh "$RELEASE_ID"       # start a specific release
./run.sh latest              # start the most recently created release
./run.sh "$RELEASE_ID" down  # stop
```

`run.sh` merges `docker-compose.yml` with the release overlay. The overlay pins exact image tags — Docker Compose cannot trigger a rebuild at runtime. The run stage has no moving parts beyond `docker compose up`.

#### Roll back to any previous release

```bash
./run.sh v20260415-120000
```

All past release directories are preserved, so rolling back is instant — just point `run.sh` at an earlier entry.

---

### Running services locally (without Docker)

**Start infrastructure only:**
```bash
docker compose up -d zookeeper prometheus grafana
```

**Start the provider:**
```bash
cd currency-rate-provider && mvn spring-boot:run
```

**Start the consumer:**
```bash
cd rate-printer && mvn spring-boot:run
```

### Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| service1 (provider) | http://localhost:8081/actuator | gRPC server instance 1 |
| service2 (provider) | http://localhost:8082/actuator | gRPC server instance 2 |
| client (rate-printer) | http://localhost:8083/actuator | gRPC client |
| Zookeeper | localhost:2181 | service discovery |
| Prometheus | http://localhost:9091 | metrics storage |
| Grafana | http://localhost:3000 | dashboards (admin / admin) |
| Pact Broker | http://localhost:9292 | contract storage |

> service1 and service2 both register as `currency-rate-provider` in Zookeeper so the client discovers both automatically and load-balances between them.

## Observability

### Spring Actuator + Micrometer

Both `currency-rate-provider` and `rate-printer` have `spring-boot-starter-actuator` and `micrometer-registry-prometheus`. Prometheus metrics are exposed at `/actuator/prometheus`.

All metrics from each instance are tagged with `application=<service-label>` (controlled by the `METRICS_APPLICATION` environment variable), which is used as the primary filter in Grafana dashboards.

### Custom gRPC Metrics (server side)

`MetricsServerInterceptor` is a `@GrpcGlobalServerInterceptor` that wraps every server call and records:

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `grpc.server.request.duration` | Timer | `method`, `client`, `status` | Request processing time. Histogram buckets are enabled, allowing `histogram_quantile()` in Prometheus for any percentile. |
| `grpc.server.errors` | Counter | `method`, `client`, `status` | Incremented for every non-OK gRPC response (HTTP-500 equivalent). |

The `client` tag is populated from the `x-client-name` gRPC metadata header injected by `ClientNameInterceptor` on the client side.

### Zookeeper JVM Metrics

`monitoring/zookeeper/Dockerfile` extends `zookeeper:3.8` with the [JMX Prometheus Java agent](https://github.com/prometheus/jmx_exporter), which exposes Zookeeper's JVM metrics (heap, GC, threads, CPU) on port `7070`.

### Prometheus

`prometheus/prometheus.yml` scrapes:

| Job | Target | Path |
|-----|--------|------|
| `service1` | `service1:8080` | `/actuator/prometheus` |
| `service2` | `service2:8080` | `/actuator/prometheus` |
| `client` | `client:8081` | `/actuator/prometheus` |
| `zookeeper` | `zookeeper:7070` | `/metrics` (JMX exporter) |

### Grafana Dashboards

Dashboards are auto-provisioned from `grafana/dashboards/` on startup.

#### JVM (Micrometer) — All Services

Displays JVM metrics for **service1**, **service2**, and **client** (Micrometer format) plus a dedicated **ZooKeeper** row (JMX exporter format).

Panels:
- Heap & non-heap memory (used / max / committed)
- GC pause rate and memory allocation rate
- Live and daemon thread counts
- Process and system CPU usage
- Loaded class count
- ZooKeeper: heap memory, threads, CPU, connections, request latency, GC rate

Template variables: **Application** (multi-select), **Instance**.

#### gRPC Server Metrics

Displays the three required server-side metric groups:

**(a) Requests per second broken down by client**
- Time series of `rate(grpc_server_request_duration_seconds_count[…]) by (application, client)`
- Stat panel showing total RPS

**(b) Number of 500 errors**
- Error rate per second by client and gRPC status code
- Cumulative error increase panel
- Stat panel showing all-time error count

**(c) Request processing time**
- Average: `rate(…_sum) / rate(…_count)`
- Median (P50), P75, P95, P99: `histogram_quantile(q, rate(…_bucket[…]))`
- Combined comparison panel showing all percentiles on one chart

Template variable: **Application** (multi-select, defaults to all).

## Contract Testing (Pact)

Consumer-driven contract testing is implemented with [Pact JVM](https://docs.pact.io/) v4.6.

### How it works

1. **Consumer** (`rate-printer`) defines the expected contract in `CurrencyRateConsumerPactTest` — a synchronous message interaction where an empty request yields a response with `pair` (string) and `rate` (decimal).
2. The pact file is generated in `target/pacts/` during `mvn test` and **published to the Pact Broker** automatically on `mvn package` (skipped when `-DskipTests` is set).
3. **Provider** (`currency-rate-provider`) fetches all consumer contracts from the broker and verifies them during `mvn verify` via `CurrencyRatePactProviderIT`.

### Running contract tests

```bash
# Full build — consumer publishes pact, then provider verifies it
mvn verify

# Override broker URL (e.g. in CI)
mvn verify -Dpact.broker.url=http://pact-broker:9292

# Skip Pact publishing only (tests still run)
mvn verify -DskipPactPublish=true
```

The root POM builds modules in order (`currency-rate-proto` → `rate-printer` → `currency-rate-provider`) so the pact is always published before verification runs.

### Viewing contracts

Open the Pact Broker UI at **http://localhost:9292** after starting the infrastructure.
