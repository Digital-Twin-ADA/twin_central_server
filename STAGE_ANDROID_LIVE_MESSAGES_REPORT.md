# Stage Server and Android Live Messages Report

## Goal

Implement the simplest live message flow:

```text
Central Server -> Stage Server -> Android App
```

The Android App should not connect directly to the Central Server. The Stage Server is responsible for receiving central alerts and forwarding only relevant alerts to Android users.

## Final Design

```text
Web App Admin
  -> creates an alert in Central Server

Central Server
  -> saves the alert
  -> broadcasts the alert on /topic/alerts

Stage Server
  -> connects to Central Server WebSocket
  -> subscribes to /topic/alerts
  -> filters alerts by stageId
  -> forwards relevant alerts to Android clients

Android App
  -> connects to Stage Server WebSocket
  -> subscribes to /topic/mobile-alerts
  -> displays received alerts
```

## Central Server Details

Central Server base URL:

```text
https://twin-central-server.onrender.com
```

Central Server WebSocket endpoint for Stage Servers:

```text
https://twin-central-server.onrender.com/ws-native
```

Central Server alert topic:

```text
/topic/alerts
```

The Central Server broadcasts alert messages to `/topic/alerts` when an alert is created or resolved.

Example alert message:

```json
{
  "id": 55,
  "stageId": 1,
  "type": "SAFETY",
  "message": "Please avoid Main Stage entrance",
  "severity": "HIGH",
  "createdAt": "2026-06-09T12:30:00+03:00",
  "resolved": false,
  "resolvedAt": null
}
```

## Stage Server Requirements

The Stage Server must connect to the Central Server as a WebSocket/STOMP client.

It must:

1. Connect to:

```text
https://twin-central-server.onrender.com/ws-native
```

2. Subscribe to:

```text
/topic/alerts
```

3. Receive alert messages.

4. Filter alerts by its own stage ID.

Example rule:

```text
If alert.stageId == thisStageServer.stageId, forward it to Android.
Otherwise, ignore it.
```

5. Expose its own WebSocket endpoint for Android clients.

Example:

```text
ws://stage-server-url/ws
```

or:

```text
wss://stage-server-url/ws
```

6. Broadcast relevant alerts to Android clients on:

```text
/topic/mobile-alerts
```

## Android App Requirements

The Android App connects only to the Stage Server for this live alert flow.

It must:

1. Connect to the Stage Server WebSocket endpoint:

```text
ws://stage-server-url/ws
```

or:

```text
wss://stage-server-url/ws
```

2. Subscribe to:

```text
/topic/mobile-alerts
```

3. Parse the received alert payload.

4. Display the alert to the user.

Example received alert:

```json
{
  "id": 55,
  "stageId": 1,
  "type": "SAFETY",
  "message": "Please avoid Main Stage entrance",
  "severity": "HIGH",
  "createdAt": "2026-06-09T12:30:00+03:00",
  "resolved": false,
  "resolvedAt": null
}
```

## End-To-End Test

1. Start the Stage Server.

2. Stage Server connects to:

```text
https://twin-central-server.onrender.com/ws-native
```

3. Stage Server subscribes to:

```text
/topic/alerts
```

4. Start the Android App.

5. Android App connects to the Stage Server WebSocket.

6. Android App subscribes to:

```text
/topic/mobile-alerts
```

7. Create an alert in Central Server:

```http
POST https://twin-central-server.onrender.com/api/alerts
Content-Type: application/json

{
  "stageId": 1,
  "type": "SAFETY",
  "message": "Please avoid Main Stage entrance",
  "severity": "HIGH"
}
```

8. Central Server broadcasts the saved alert on:

```text
/topic/alerts
```

9. Stage Server receives the alert.

10. Stage Server forwards it to Android only if:

```text
alert.stageId == thisStageServer.stageId
```

11. Android App receives the alert on:

```text
/topic/mobile-alerts
```

12. Android App displays the alert.

## Responsibilities Summary

Central Server:

```text
Provides /ws-native for Stage Servers
Broadcasts alerts on /topic/alerts
Stores official alerts
Does not connect directly to Android
```

Stage Server:

```text
Connects to Central Server /ws
Subscribes to /topic/alerts
Filters alerts by stageId
Exposes its own WebSocket endpoint for Android
Broadcasts relevant alerts on /topic/mobile-alerts
```

Android App:

```text
Connects to Stage Server WebSocket
Subscribes to /topic/mobile-alerts
Displays received alerts
```

Web App:

```text
Creates admin alerts in Central Server
Can use POST /api/alerts or WebSocket /app/alerts
```
