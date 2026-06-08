# The Central Coordination Server

The Central Coordination Server acts as the global orchestration layer of the Digital Twin Festival Manager and will be implemented using Java 21 with Spring Boot to provide a robust and scalable backend architecture. It maintains the global festival state, synchronizes distributed stage servers, aggregates live crowd telemetry, processes AI-generated alerts, manages volunteer dispatch operations, and supports the real-time organizer dashboard via REST APIs and WebSocket communication. Persistent data storage will be handled by a self-managed PostgreSQL database, integrated through Spring Data JPA (Hibernate), with schema versioning managed using Flyway. The system will be containerized with Docker and deployed on Oracle Cloud Infrastructure to ensure reliability and full control over distributed coordination processes.

## Webhook Integration Plan

The central server already supports outgoing webhooks for alerts. The main use case is overcrowding:

```text
Mobile App -> Stage Server -> Central Server -> Web App
```

When the stage server sends telemetry and the crowd is greater than or equal to the stage capacity, the central server creates an `OVER_CROWD` alert and sends it to every registered webhook subscriber.

### Central Server Responsibilities

The central server owns these endpoints:

```http
POST /api/webhooks
GET /api/webhooks
DELETE /api/webhooks/{id}
POST /api/telemetry
POST /api/alerts
GET /api/alerts
```

Webhook registration:

```http
POST https://twin-central-server.onrender.com/api/webhooks
Content-Type: application/json

{
  "url": "https://web-app-url.com/api/webhooks/central-alerts",
  "secret": "shared-secret"
}
```

Telemetry from the stage server:

```http
POST https://twin-central-server.onrender.com/api/telemetry
Content-Type: application/json

{
  "stageId": 1,
  "currentCrowd": 1200
}
```

If the stage capacity is lower than or equal to `currentCrowd`, the central server creates an alert:

```json
{
  "id": 15,
  "stageId": 1,
  "type": "OVER_CROWD",
  "message": "Stage Main Stage is overcrowded: 1200/1000",
  "severity": "HIGH",
  "createdAt": "2026-06-08T14:30:00+03:00",
  "resolved": false,
  "resolvedAt": null
}
```

The central server sends this payload to the registered webapp URL using `POST`.

If a secret was registered, the central server also sends this header:

```http
X-Webhook-Signature: sha256=<hmac-sha256-of-raw-body>
```

### Web App Responsibilities

The web app team must create a public endpoint that can receive alerts from the central server:

```http
POST /api/webhooks/central-alerts
```

The endpoint should:

1. Accept JSON `POST` requests.
2. Verify `X-Webhook-Signature` using the shared secret.
3. Check the alert type, especially `OVER_CROWD`.
4. Save or display the alert in the dashboard.
5. Return a `2xx` response when processed successfully.

Example received alert:

```json
{
  "id": 15,
  "stageId": 1,
  "type": "OVER_CROWD",
  "message": "Stage Main Stage is overcrowded: 1200/1000",
  "severity": "HIGH",
  "createdAt": "2026-06-08T14:30:00+03:00",
  "resolved": false,
  "resolvedAt": null
}
```

### Stage Server Responsibilities

The stage server team must collect mobile locations, calculate the current crowd for each stage, and send crowd telemetry to the central server:

```http
POST https://twin-central-server.onrender.com/api/telemetry
Content-Type: application/json

{
  "stageId": 1,
  "currentCrowd": 1200
}
```

The central server handles the overcrowding decision. The stage server should not create central alerts directly unless the team decides to support manual stage-server alerts later.

### Mobile App Responsibilities

The mobile app team must:

1. Send user location updates to the stage server.
2. Generate random test locations between stages during development.
3. Display warnings or stage updates received from the stage server.
4. Help test overcrowding by simulating many users near one stage.

### Manual Alert Testing

The team can test the webapp webhook without mobile/stage integration by creating an alert manually:

```http
POST https://twin-central-server.onrender.com/api/alerts
Content-Type: application/json

{
  "stageId": 1,
  "type": "OVER_CROWD",
  "message": "Manual webhook test: Main Stage is overcrowded",
  "severity": "HIGH"
}
```

This should send a webhook to the registered webapp URL.

### End-to-End Test

1. Web app exposes `POST /api/webhooks/central-alerts`.
2. Web app registers that URL with `POST /api/webhooks`.
3. Stage server sends high crowd telemetry to `POST /api/telemetry`.
4. Central server detects overcrowding.
5. Central server creates an `OVER_CROWD` alert.
6. Central server sends the webhook to the web app.
7. Web app shows the alert in real time.
8. Mobile app receives any needed warning from the stage server.

### Retry Behavior

The central server retries failed webhook deliveries up to three attempts. It retries if the request fails or if the web app returns a `5xx` response. Delivery attempts are stored in the `notification_attempts` table.
