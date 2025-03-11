

# User Journey and API Reference for Trusted Participants Whitelist API

This README provides a comprehensive guide on how to perform a data exchange between a **Provider** and a **Consumer** through a **Data Trustee** using the Trusted Participants Whitelist API. The guide includes:

- An overview of the user journey with step-by-step instructions.
- Detailed API endpoint descriptions.
- A data exchange state diagram illustrating the state transitions during the process.

---

## Table of Contents

1. [Overview of the Flow](#overview-of-the-flow)
2. [Participants](#participants)
3. [Prerequisites](#prerequisites)
4. [API Endpoints](#api-endpoints)
5. [Data Exchange State Diagram](#data-exchange-state-diagram)
6. [Step-by-Step Guide](#step-by-step-guide)
    - [1. Setup Trusted Participants](#1-setup-trusted-participants)
    - [2. Initiate Negotiation](#2-initiate-negotiation)
    - [3. Provider and Consumer Send Notifications](#3-provider-and-consumer-send-notifications)
    - [4. Receive Entry IDs](#4-receive-entry-ids)
    - [5. Manually Update Entry State to `IN_PROGRESS`](#5-manually-update-entry-state-to-in_progress)
    - [6. Manually Update Entry State to `COMPLETED`](#6-manually-update-entry-state-to-completed)
    - [7. Notifications Sent Upon Completion](#7-notifications-sent-upon-completion)
7. [Summary](#summary)
8. [Notes](#notes)

---

## Overview of the Flow

1. **Setup Trusted Participants**
2. **Initiate Negotiation**
3. **Provider and Consumer Send Notifications**
4. **Receive Entry IDs**
5. **Manually Update Entry State to `IN_PROGRESS`**
6. **Manually Update Entry State to `COMPLETED`**
7. **Notifications Sent Upon Completion**

---

## Participants

- **Provider**
    - Name: `provider`
    - URL: `http://localhost:29191/api/trusted-participants`
- **Consumer**
    - Name: `consumer`
    - URL: `http://localhost:19191/api/trusted-participants`
- **Data Trustee**
    - Name: `participant2`
    - URL: `http://localhost:29591/api/trusted-participants`
- **Assets**
    - `asset1`, `asset2`

---

## Prerequisites

- Ensure all services (**Provider**, **Consumer**, and **Data Trustee**) are running and accessible.
- Each participant should have the Trusted Participants Whitelist API implemented.
- Participants should have network connectivity to communicate via the provided URLs.
- Ensure that the required assets are available for the data exchange.

---

## API Endpoints

### Trusted Participants Whitelist API Endpoints

| HTTP Request                                       | Description                                                                                                                               |
| -------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `GET /trusted-participants/health`                 | Checks the health of the service and returns its status.                                                                                  |
| `POST /trusted-participants/add`                   | Adds a trusted participant to the whitelist and returns the outcome.                                                                      |
| `GET /trusted-participants/list`                   | Retrieves a list of trusted participants along with a hash for verification.                                                              |
| `DELETE /trusted-participants/remove`              | Removes a trusted participant from the whitelist and returns the outcome.                                                                 |
| `POST /trusted-participants/negotiate/{counterPartyUrl}` | Initiates a negotiation with another participant to determine common trusted participants. Expects a path variable with the counterparty URL. |
| `POST /trusted-participants/receive-negotiation`   | Handles incoming negotiation requests, matches trusted participants, and returns the negotiation outcome.                                 |
| `POST /trusted-participants/notify`                | Receives notifications related to data trustee selection and data exchange initiation.                                                    |
| `POST /trusted-participants/update-entry-state`    | Manually updates the state of a data exchange entry (e.g., to `IN_PROGRESS` or `COMPLETED`).                                             |
| `GET /trusted-participants/data-exchange-entries`  | Retrieves a list of current data exchange entries and their states.                                                                       |
| `POST /trusted-participants/notify-completion`     | Receives completion notifications indicating the data exchange process has completed.                                                    |

---

## Data Exchange State Diagram

The following diagram illustrates the state transitions during the data exchange process:

stateDiagram
[*] --> NOT_READY : One notification received

    NOT_READY --> READY : Second notification received
    NOT_READY --> FAILED : Timeout (1 day)

    READY --> IN_PROGRESS : Start data exchange
    READY --> FAILED : Timeout (1 day)

    IN_PROGRESS --> COMPLETED : Exchange successful
    IN_PROGRESS --> FAILED : Exchange failed

    FAILED --> [*]
    COMPLETED --> [*]



---

## Step-by-Step Guide

### 1. Setup Trusted Participants

Both the **Provider** and **Consumer** add the **Data Trustee** to their list of trusted participants.

#### Provider Adds Data Trustee

**Endpoint:**

```
POST /trusted-participants/add
```

**Request Body:**

```json
{
  "name": "participant2",
  "url": "http://localhost:29591/api/trusted-participants"
}
```

**Provider Terminal Log:**

```
INFO 2025-03-03T13:51:51.134026407 Adding trusted participant: participant2
```

#### Consumer Adds Data Trustee

**Endpoint:**

```
POST /trusted-participants/add
```

**Request Body:**

```json
{
  "name": "participant2",
  "url": "http://localhost:29591/api/trusted-participants"
}
```

**Consumer Terminal Log:**

```
INFO 2025-03-03T13:51:46.973363452 Adding trusted participant: participant2
```

---

### 2. Initiate Negotiation

The **Consumer** initiates a negotiation with the **Provider** to determine a commonly trusted data trustee.

**Endpoint:**

```
POST /trusted-participants/negotiate/{counterPartyUrl}
```

**Path Parameter:**

- `{counterPartyUrl}`: URL-encoded provider's negotiation endpoint:

  ```
  http%3A%2F%2Flocalhost%3A29191%2Fapi%2Ftrusted-participants%2Freceive-negotiation
  ```

**Example Request:**

```
POST /trusted-participants/negotiate/http%3A%2F%2Flocalhost%3A29191%2Fapi%2Ftrusted-participants%2Freceive-negotiation
```

**Consumer Terminal Log:**

```
INFO 2025-03-03T13:51:57.688358318 Negotiation initiated with: http://localhost:29191/api/trusted-participants/receive-negotiation; Response: {"dataSource":{"id":null,"name":"provider","url":"http://localhost:29191/api/trusted-participants"},"dataSink":{"id":null,"name":"consumer","url":"http://localhost:19191/api/trusted-participants"},"trustedDataTrustee":{"id":null,"name":"participant2","url":"http://localhost:29591/api/trusted-participants"},"assets":["asset1","asset2"]}
```

**Provider Receives Negotiation Request**

**Provider Terminal Log:**

```
INFO 2025-03-03T13:51:57.023948562 Received negotiation request
```

**Negotiation Process:**

- Both the **Consumer** and **Provider** share their list of trusted participants.
- They identify `participant2` as a common data trustee.
- Notifications are sent to the **Data Trustee** by both parties.

---

### 3. Provider and Consumer Send Notifications

Both the **Provider** and **Consumer** send notifications to the **Data Trustee** to initiate the data exchange.

#### Provider Sends Notification to Data Trustee

**Endpoint:**

```
POST /trusted-participants/notify
```

**Request Body:**

```json
{
  "dataSource": {
    "name": "provider",
    "role": "provider",
    "url": "http://localhost:29191/api/trusted-participants"
  },
  "dataSink": {
    "name": "consumer",
    "role": "consumer",
    "url": "http://localhost:19191/api/trusted-participants"
  },
  "assets": ["asset1", "asset2"],
  "senderType": "provider"
}
```

**Provider Terminal Log:**

```
INFO 2025-03-03T13:51:57.627115685 Notification sent to participant2; Response: {"message":"Notification received","entryId":"2454c106-66b4-4353-9697-7a978c7df3fd"}
```

**Data Trustee Receives Notification**

**Trustee Terminal Log:**

```
INFO 2025-03-03T13:51:57.550284112 Received notification: DataTrusteeRequest[dataSource=Participant{id='null', name='provider', url='http://localhost:29191/api/trusted-participants'}, dataSink=Participant{id='null', name='consumer', url='http://localhost:19191/api/trusted-participants'}, assets=[asset1, asset2], senderType=provider]
First notification received from provider, waiting for second notification...
```

#### Consumer Sends Notification to Data Trustee

**Endpoint:**

```
POST /trusted-participants/notify
```

**Request Body:**

```json
{
  "dataSource": {
    "name": "provider",
    "role": "provider",
    "url": "http://localhost:29191/api/trusted-participants"
  },
  "dataSink": {
    "name": "consumer",
    "role": "consumer",
    "url": "http://localhost:19191/api/trusted-participants"
  },
  "assets": ["asset1", "asset2"],
  "senderType": "consumer"
}
```

**Consumer Terminal Log:**

```
INFO 2025-03-03T13:51:57.747735089 Notification sent to participant2; Response: {"message":"Notification received","entryId":"2454c106-66b4-4353-9697-7a978c7df3fd"}
```

**Data Trustee Receives Notification**

**Trustee Terminal Log:**

```
INFO 2025-03-03T13:51:57.737034239 Received notification: DataTrusteeRequest[dataSource=Participant{id='null', name='provider', url='http://localhost:29191/api/trusted-participants'}, dataSink=Participant{id='null', name='consumer', url='http://localhost:19191/api/trusted-participants'}, assets=[asset1, asset2], senderType=consumer]
Second notification received from consumer, conditions ready for data exchange.
```

**Notes:**

- Upon receiving both notifications, the **Data Trustee** updates the state of the data exchange entry to `READY`.

---

### 4. Receive Entry IDs

Both the **Provider** and **Consumer** receive an `entryId` from the **Data Trustee**, which uniquely identifies the data exchange entry.

**Sample Response:**

```json
{
  "message": "Notification received",
  "entryId": "2454c106-66b4-4353-9697-7a978c7df3fd"
}
```

**Notes:**

- The same `entryId` is provided to both the **Provider** and **Consumer**.
- The entry is now in the `READY` state, waiting for manual state updates.

---

### 5. Manually Update Entry State to `IN_PROGRESS`

The **Data Trustee** manually updates the state of the data exchange entry to `IN_PROGRESS` to indicate that the data exchange process has started.

**Endpoint:**

```
POST /trusted-participants/update-entry-state
```

**Query Parameters:**

- `entryId`: `2454c106-66b4-4353-9697-7a978c7df3fd`
- `newState`: `IN_PROGRESS`

**Example Request:**

```
POST /trusted-participants/update-entry-state?entryId=2454c106-66b4-4353-9697-7a978c7df3fd&newState=IN_PROGRESS
```

**Response:**

```json
{
  "message": "State updated successfully."
}
```

**Trustee Terminal Log:**

```
INFO 2025-03-03T13:52:35.37296158 Received request to update state of entry 2454c106-66b4-4353-9697-7a978c7df3fd to IN_PROGRESS
State manually updated to IN_PROGRESS for entry: org.eclipse.edc.mvd.model.DataExchangeEntry@399459d7
```

---

### 6. Manually Update Entry State to `COMPLETED`

After the data exchange process is complete, the **Data Trustee** updates the state to `COMPLETED`.

**Endpoint:**

```
POST /trusted-participants/update-entry-state
```

**Query Parameters:**

- `entryId`: `2454c106-66b4-4353-9697-7a978c7df3fd`
- `newState`: `COMPLETED`

**Example Request:**

```
POST /trusted-participants/update-entry-state?entryId=2454c106-66b4-4353-9697-7a978c7df3fd&newState=COMPLETED
```

**Response:**

```json
{
  "message": "State updated successfully."
}
```

**Trustee Terminal Log:**

```
INFO 2025-03-03T13:53:02.37296158 Received request to update state of entry 2454c106-66b4-4353-9697-7a978c7df3fd to COMPLETED
State manually updated to COMPLETED for entry: org.eclipse.edc.mvd.model.DataExchangeEntry@399459d7
```

**Notes:**

- Updating the state to `COMPLETED` triggers the **Data Trustee** to send completion notifications to both the **Provider** and **Consumer**.

---

### 7. Notifications Sent Upon Completion

Upon updating the state to `COMPLETED`, the **Data Trustee** automatically sends completion notifications to both the **Provider** and **Consumer**.

#### Data Trustee Sends Completion Notification to Provider

**Endpoint (Provider’s API):**

```
POST /trusted-participants/notify-completion
```

**Request Body:**

```json
{
  "message": "Data exchange has been completed for assets: [\"asset1\", \"asset2\"]",
  "role": "provider"
}
```

**Trustee Terminal Log:**

```
INFO 2025-03-03T13:53:02.539751291 Completion Notification sent to provider: provider; Response: {"message":"Completion notification received."}
```

**Provider Receives Completion Notification**

**Provider Terminal Log:**

```
INFO 2025-03-03T13:53:02.508754106 Received completion notification for role: provider. Message: Data exchange has been completed for assets: [asset1, asset2]
```

#### Data Trustee Sends Completion Notification to Consumer

**Endpoint (Consumer’s API):**

```
POST /trusted-participants/notify-completion
```

**Request Body:**

```json
{
  "message": "Data exchange has been completed for assets: [\"asset1\", \"asset2\"]",
  "role": "consumer"
}
```

**Trustee Terminal Log:**

```
INFO 2025-03-03T13:53:02.566297676 Completion Notification sent to consumer: consumer; Response: {"message":"Completion notification received."}
```

**Consumer Receives Completion Notification**

**Consumer Terminal Log:**

```
INFO 2025-03-03T13:53:02.561573437 Received completion notification for role: consumer. Message: Data exchange has been completed for assets: [asset1, asset2]
```

---

## Summary

This user journey demonstrates how the **Provider** and **Consumer** coordinate with the **Data Trustee** to perform a data exchange:

1. **Setup Trusted Participants:** Both parties add the **Data Trustee** to their trusted participants list.
2. **Initiate Negotiation:** The **Consumer** initiates a negotiation, and they select a common data trustee.
3. **Send Notifications:** Both parties notify the **Data Trustee** of their intent, providing the necessary details.
4. **Receive Entry IDs:** The **Data Trustee** responds with an `entryId` for tracking the data exchange entry.
5. **Update Entry State to `IN_PROGRESS`:** The **Data Trustee** manually updates the state to indicate the start of the data exchange.
6. **Update Entry State to `COMPLETED`:** After the exchange, the state is updated to `COMPLETED`.
7. **Completion Notifications:** Upon completion, the **Data Trustee** informs both the **Provider** and **Consumer**.

---


## Notes

- **URL Encoding:** When passing URLs as path parameters (e.g., in the `negotiate` endpoint), ensure they are properly URL-encoded.
- **Data Integrity:** The `hash` in negotiation requests and responses is used to verify data integrity. Always compute and verify hashes using the same algorithm.
- **Participant Accuracy:** Ensure that participant details (`name`, `url`) are accurately specified in all requests.
- **Time Out Handling:** Entries in the `NOT_READY` state will transition to `FAILED` if the counterpart notification is not received within a specified timeout (e.g., 1 day).
- **Notification Sequence:** Both provider and consumer must notify the data trustee for the data exchange to proceed.

---
