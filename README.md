# Agentic Haiku - Haiku and Image Generation Service

## Running Locally with Postgres and Jaeger

First start a local Jaeger and Postgres in docker using the prepared docker compose file:

```shell
docker compose up
```

Then start your service locally, with tracing enabled and reporting to the local Jaeger instance:

```shell
TRACING_ENABLED=true COLLECTOR_ENDPOINT="http://localhost:4317" mvn compile exec:java
```
