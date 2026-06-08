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

## Live Heatmap WebSocket

The central server supports live dashboard updates through STOMP WebSockets.

WebSocket endpoint:

```text
https://twin-central-server.onrender.com/ws
```

Available topics:

```text
/topic/alerts
/topic/events
/topic/heatmap
```

### Live Alerts Topic

The web app should subscribe to `/topic/alerts` to receive alerts in real time.

The web app can also create alerts through WebSocket by sending messages to `/app/alerts`.

Alert WebSocket directions:

```text
Web App -> Central Server: /app/alerts
Central Server -> Web App: /topic/alerts
```

Example message sent by the web app to `/app/alerts`:

```json
{
  "stageId": 1,
  "type": "SAFETY",
  "message": "Manual alert sent by admin",
  "severity": "HIGH"
}
```

The central server saves this alert, then broadcasts the saved alert to `/topic/alerts`.

This topic receives messages when:

1. A new alert is created with `POST /api/alerts`.
2. An alert is resolved with `POST /api/alerts/{id}/resolve`.
3. A new alert is created through WebSocket with `/app/alerts`.
4. The central server creates an automatic `OVER_CROWD` alert from telemetry.

Example alert message:

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

### Live Heatmap Topic

Heatmap topic:

```text
/topic/heatmap
```

The mobile app or stage server sends participant locations to the central server:

```http
POST https://twin-central-server.onrender.com/api/participant-locations
Content-Type: application/json

{
  "participantId": "user-123",
  "stageId": 1,
  "latitude": 45.7489,
  "longitude": 21.2087,
  "zoneCode": "A1"
}
```

After each saved location, the central server broadcasts the latest heatmap snapshot from the last 10 minutes to `/topic/heatmap`.

Example WebSocket message:

```json
[
  {
    "latitude": 45.7489,
    "longitude": 21.2087,
    "stageId": 1,
    "stageName": "Main Stage",
    "zoneCode": "A1",
    "recordedAt": "2026-06-08T14:30:00+03:00",
    "weight": 1
  }
]
```

### Live Events Topic

The web app should subscribe to `/topic/events` to receive festival event updates in real time.

This topic receives messages when spontaneous events are created, updated, or deleted.

Event topic:

```text
/topic/events
```

Example created or updated event message:

```json
{
  "id": 20,
  "title": "Surprise acoustic session",
  "description": "Short pop-up performance near Main Stage",
  "stageId": 1,
  "stageName": "Main Stage",
  "startsAt": "2026-06-08T18:00:00+03:00",
  "endsAt": "2026-06-08T18:30:00+03:00",
  "status": "ACTIVE",
  "createdAt": "2026-06-08T17:45:00+03:00"
}
```

Example deleted event message:

```json
{
  "action": "deleted",
  "type": "spontaneous",
  "id": 20
}
```

The web app can also fetch the current heatmap without WebSockets:

```http
GET https://twin-central-server.onrender.com/api/participant-locations/heatmap?minutes=10
```

For the dashboard, use both:

1. Fetch `GET /api/participant-locations/heatmap?minutes=10` when the page opens.
2. Connect to `/ws`.
3. Subscribe to `/topic/heatmap`.
4. Subscribe to `/topic/alerts`.
5. Subscribe to `/topic/events`.
6. Replace or refresh the displayed heatmap whenever a new heatmap message arrives.
7. Display a notification or dashboard alert whenever a new alert message arrives.
8. Refresh the event list or event markers whenever a new event message arrives.
