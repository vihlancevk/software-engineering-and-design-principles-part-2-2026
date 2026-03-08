# Currency Rate Service

A microservices demo built with Spring Boot and gRPC, demonstrating service discovery (Zookeeper) and consumer-driven contract testing (Pact).

## Architecture

```
┌─────────────────┐        gRPC GetRate()        ┌──────────────────────────┐
│   rate-printer  │ ────────────────────────────► │  currency-rate-provider  │
│   (consumer)    │ ◄──── {pair, rate} ─────────  │      (provider)          │
└─────────────────┘                               └──────────────────────────┘
        │                                                      │
        │  publishes pact                        verifies pact │
        ▼                                                      ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                            Pact Broker :9292                             │
└──────────────────────────────────────────────────────────────────────────┘
        Both services register/discover via Zookeeper :2181
```

## Modules

| Module | Description |
|--------|-------------|
| `currency-rate-proto` | Protobuf definitions for the `CurrencyRateService` gRPC API |
| `currency-rate-provider` | gRPC server — returns a random USD/RUB exchange rate |
| `rate-printer` | gRPC client — fetches and prints the rate every 5 seconds |

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

- Java 25
- Maven 3.9+
- Docker & Docker Compose

## Running

**Start infrastructure:**
```bash
docker compose up -d
```

This starts:
- **Zookeeper** on port `2181` — service discovery
- **Pact Broker** on port `9292` — contract storage (`http://localhost:9292`)
- **PostgreSQL** — backing store for the Pact Broker

**Start the provider:**
```bash
cd currency-rate-provider
mvn spring-boot:run
```

**Start the consumer:**
```bash
cd rate-printer
mvn spring-boot:run
```

## Contract Testing (Pact)

Consumer-driven contract testing is implemented with [Pact JVM](https://docs.pact.io/) v4.6.

### How it works

1. **Consumer** (`rate-printer`) defines the expected contract in `CurrencyRateConsumerPactTest` — a synchronous message interaction where an empty request yields a response with `pair` (string) and `rate` (decimal).
2. The pact file is generated in `target/pacts/` during `mvn test` and **published to the Pact Broker** automatically on `mvn package`.
3. **Provider** (`currency-rate-provider`) fetches all consumer contracts from the broker and verifies them during `mvn verify` via `CurrencyRatePactProviderIT`.

### Running contract tests

```bash
# Full build — consumer publishes pact, then provider verifies it
mvn verify

# Override broker URL (e.g. in CI)
mvn verify -Dpact.broker.url=http://pact-broker:9292
```

The root POM builds modules in order (`currency-rate-proto` → `rate-printer` → `currency-rate-provider`) so the pact is always published before verification runs.

### Viewing contracts

Open the Pact Broker UI at **http://localhost:9292** after running the build.
