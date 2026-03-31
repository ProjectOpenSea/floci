# Persistence

Floci can bake AWS resources into a Docker image at build time so every container starts with those resources already available — no init-script overhead on each startup.

This is useful when your project always needs the same baseline infrastructure (S3 buckets, SQS queues, SSM parameters, etc.) and you want instant availability without paying the setup cost on every `docker compose up`.

## How It Works

1. During `docker build`, Floci starts inside the build container with persistent storage enabled.
2. Your seed scripts run against the live Floci instance, creating resources via the AWS CLI.
3. Floci shuts down and flushes all state to `/app/data`.
4. A final image is produced with that data directory baked in.

When you run the resulting image, Floci loads the persisted state on startup and all seeded resources are immediately available.

## Quick Start

### 1. Create Seed Scripts

Create a `seed/` directory with shell scripts that set up your resources:

```bash title="seed/01-buckets.sh"
#!/bin/bash
aws s3 mb s3://my-app-uploads
aws s3 mb s3://my-app-assets
```

```bash title="seed/02-queues.sh"
#!/bin/bash
aws sqs create-queue --queue-name order-events
aws sqs create-queue --queue-name order-events-dlq
```

```bash title="seed/03-params.sh"
#!/bin/bash
aws ssm put-parameter \
  --name /my-app/db-host \
  --value "localhost" \
  --type String
```

Scripts are executed in lexicographical order, so use numeric prefixes to control ordering.

### 2. Build the Seeded Image

```bash
docker build -f Dockerfile.seed \
  --build-arg BASE_IMAGE=hectorvent/floci:latest-awscli \
  -t my-floci-seeded .
```

!!! note
    The base image must include the AWS CLI. Use `hectorvent/floci:latest-awscli` or build your own with `Dockerfile.awscli`.

### 3. Run It

```bash
docker run -p 4566:4566 my-floci-seeded
```

All seeded resources are available immediately:

```bash
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_DEFAULT_REGION=us-east-1
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test

aws s3 ls                  # my-app-uploads, my-app-assets
aws sqs list-queues        # order-events, order-events-dlq
aws ssm get-parameter --name /my-app/db-host
```

## Docker Compose

Use the seeded image in your compose file:

```yaml title="docker-compose.yml"
services:
  floci:
    image: my-floci-seeded
    ports:
      - "4566:4566"
  my-app:
    environment:
      - AWS_ENDPOINT_URL=http://floci:4566
    depends_on:
      - floci
```

## Health Endpoint

Floci exposes `/_floci/health` which returns the version and status of all enabled services:

```bash
curl http://localhost:4566/_floci/health
```

```json
{
  "version": "1.0.11",
  "edition": "community",
  "services": {
    "ssm": "available",
    "sqs": "available",
    "s3": "available",
    "dynamodb": "available",
    ...
  }
}
```

The seed Dockerfile uses this endpoint to wait for Floci to be ready before running seed scripts. You can also use it in your own readiness checks and CI pipelines.

## How Dockerfile.seed Works

The `Dockerfile.seed` uses a two-stage build:

```
┌─────────────────────────────────────────────┐
│  Stage 1: seed                              │
│  1. Start Floci with persistent storage     │
│  2. Wait for /_floci/health to respond      │
│  3. Run seed/*.sh scripts (create resources)│
│  4. Stop Floci (state flushed to /app/data) │
└─────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│  Stage 2: final                             │
│  Copy /app/data from stage 1 into a clean   │
│  Floci image with persistent storage enabled│
└─────────────────────────────────────────────┘
```

## Tips

- **Idempotent scripts**: Write seed scripts so they can be re-run safely. Use `aws ... 2>/dev/null || true` for resources that might already exist.
- **Script ordering**: Scripts run in lexicographical order. Use numeric prefixes (`01-`, `02-`) to control the sequence.
- **Debugging**: If a seed script fails, the Docker build will fail with the script's error output. Fix the script and rebuild.
- **Runtime persistence**: The seeded image runs with `FLOCI_STORAGE_MODE=persistent` by default. Any resources created at runtime are also persisted. Override with `FLOCI_STORAGE_MODE=memory` if you want runtime changes to be ephemeral.
