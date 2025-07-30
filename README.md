# The challenge

**Obs.**: here's the [original challenge URL](https://github.com/zanfranceschi/rinha-de-backend-2025)

> Backend fighting 2025 edition challenge solution. The challenge is to integrate with two payment processors services with the smaller tax, ensuring max profit. The plot twist is: they're quite unstable!

# The solution

```mermaid
flowchart TD
    Client[Client]
    NGINX[NGINX]
    Q1[Quarkus Instance 1]
    Q2[Quarkus Instance 2]
    Redis[Redis]
    ProcA[Payment Processor A]
    ProcB[Payment Processor B]

    %% Client to NGINX
    Client -->|POST /payments| NGINX
    Client -->|GET /payments-summary| NGINX

    %% NGINX to Quarkus
    NGINX -->|Forward request| Q1
    NGINX -->|Forward request| Q2

    %% Quarkus to Redis
    Q1 -->|Read/Write| Redis
    Q2 -->|Read/Write| Redis

    %% Quarkus to Payment Processors
    Q1 -->|Process Payment| ProcA
    Q1 -->|Process Payment| ProcB
    Q2 -->|Process Payment| ProcA
    Q2 -->|Process Payment| ProcB

    %% Payment Processors respond to Quarkus
    ProcA -->|Result| Q1
    ProcB -->|Result| Q1
    ProcA -->|Result| Q2
    ProcB -->|Result| Q2

    %% Quarkus instances may notify each other (e.g., POST /h)
    Q1 -->|POST /h| Q2
    Q2 -->|POST /h| Q1
```

## The flows

- POST /payments

```mermaid
sequenceDiagram
    participant Client
    participant NGINX
    participant Quarkus as Quarkus Instance
    participant Deque as In-memory Deque

    Client->>NGINX: POST /payments {correlationId, amount}
    NGINX->>Quarkus: Forward request
    alt First time running
        Quarkus->>Quarkus: Parse JSON, set amount, flag, instant, correlationId
    else Not first time
        Quarkus->>Quarkus: Extract correlationId via substring
    end
    Quarkus->>Deque: Store correlationId
    Quarkus-->>Client: 204 No Content
```

- GET /payments-summary

```mermaid
sequenceDiagram
    participant Client
    participant NGINX
    participant Quarkus as Quarkus Instance
    participant Redis

    Client->>NGINX: GET /payments-summary?from&to
    NGINX->>Quarkus: Forward request
    Quarkus->>Redis: FT.AGGREGATE p @t[from to] GROUP BY @d REDUCE COUNT as c
    Redis-->>Quarkus: {default: {count}, fallback: {count}}
    Quarkus->>Quarkus: Calculate totalAmount = count * static amount
    Quarkus-->>Client: {default, fallback summary}
```

- Pending payments background loop thread

```mermaid
flowchart TD
    Start[Background loop thread]
    FilterCandidates[Filter processor candidates: not failing, retry time ok]
    OrderCandidates[If multiple candidates, order by estimated profit/second]
    NoCandidates{Candidates empty?}
    PollDeque[Poll last correlationId]
    NoCorrelationId{correlationId is null?}
    CallProcessor[Call payment processor]
    UpdateMinRT[Update minResponseTime if changed]
    Success{Success?}
    RedisHSet[Redis hset payment info]
    WasAlreadyFailing{Was already failing?}
    YesItWasAlreadyFailing[Increase retry time scale factor]
    NoItWasntAlreadyFailing[Set failing POST /h]
    IsThereOtherCandidate{Is there other candidate?}
    NotifyOther[POST /h to other instance if changed]
    End[End]

    Start --> FilterCandidates
    FilterCandidates --> OrderCandidates
    OrderCandidates --> NoCandidates
    NoCandidates -- Yes --> End
    NoCandidates -- No --> PollDeque
    PollDeque --> NoCorrelationId
    NoCorrelationId -- Yes --> End
    NoCorrelationId -- No --> CallProcessor
    CallProcessor --> UpdateMinRT
    UpdateMinRT --> Success
    Success -- Yes --> RedisHSet --> NotifyOther
    Success -- No --> WasAlreadyFailing
    WasAlreadyFailing -- Yes --> YesItWasAlreadyFailing --> IsThereOtherCandidate
    WasAlreadyFailing -- No --> NoItWasntAlreadyFailing --> IsThereOtherCandidate
    IsThereOtherCandidate -- Yes --> CallProcessor
    IsThereOtherCandidate -- No --> NotifyOther
    NotifyOther --> End
```

- Service health scheduler

```mermaid
flowchart TD
    Start[Every 5s Scheduler]
    ShouldCheck{SHOULD_EXECUTE_DEFAULT/FALLBACK_CHECK?}
    CallHealth[Call GET /payments/service-health on both processors]
    UpdateFlags[Update minResponseTime and failing flag in memory]
    NotifyOther[POST /h to other instance if changed]
    FlipEnv[Flip SHOULD_EXECUTE env value]
    End[End]

    Start --> ShouldCheck
    ShouldCheck -- No --> FlipEnv
    ShouldCheck -- Yes --> CallHealth
    CallHealth --> UpdateFlags
    UpdateFlags --> NotifyOther
    NotifyOther --> FlipEnv
    FlipEnv --> End
```

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/backend-fighting-2025-0.0.1-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
